package android.example.surfacepad;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.example.surfacepad.util.Complex;
import android.example.surfacepad.util.FFT;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class ServiceForBackground extends Service {
    private static final String TAG = "BackgroundService";
    private static final String TAG2 = "DOUBLE";

    private static final double DETECT_THRESHOLD = 50;
    private static final double DETECT_DOUBLEKNOCK = 5;
    private static final double ENERGY_THRESHOLD = 4;
    private static final int WINDOW_LENGTH = 128;

    private final int mSampleRate = 48000;
    private final int mAudioSource = MediaRecorder.AudioSource.MIC;
    private final short mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private final short mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    private AudioRecord mAudioRecord;
    private boolean isRecording = false;
    private Vector<Double> lWin;
    private Vector<Double> rWin;
    private double currEnergy;
    private int numUp;
    private int numDown;

    private int numCenter;

    private AudioRecord mAudioRecord2;
    private boolean isRecording2 = false;
    private Vector<Double> lWin2;
    private Vector<Double> rWin2;
    private double dblEnergy;
    private int numUp2;
    private int numDown2;
    private int numCenter2;

    private int action;

    private final Intent activityIntent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private final Intent mainIntent = new Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addCategory(Intent.CATEGORY_HOME);
    private final Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:01020437158")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    private final Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:01020437158")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    private final Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=JNU1ObVB1VU")).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    private AudioManager audioManager;
    private Context mContext;
    private boolean isMute;

    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        mAudioRecord2 = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        isRunning = false;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mContext = getApplicationContext();
        audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND);
        isMute = false;

        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            super.onStartCommand(intent, flags, startId);
            Log.d(TAG, "START!");
            MediaPlayer mp = MediaPlayer.create(this, R.raw.service_started);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
            mAudioRecord.startRecording();
            mAudioRecord2.startRecording();
            isRecording = true;
            new mRecordThread().start();
            startTimer();
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        MediaPlayer mp = MediaPlayer.create(this, R.raw.service_terminated);
        mp.setOnCompletionListener(MediaPlayer::release);
//        mAudioRecord.stop();
//        mAudioRecord2.stop();
        super.onDestroy();
        stoptimertask();
        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Timer timer;
    public int counter=0;
    public void startTimer() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                Log.i("Count", "=========  " + (counter++));
            }
        };
        timer.schedule(timerTask, 1000, 1000); //
    }
    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public class mRecordThread extends Thread {
        public mRecordThread() {
        }

        public void run() {
            lWin = new Vector<>(Arrays.asList(initDoubleWithZeros()));
            rWin = new Vector<>(Arrays.asList(initDoubleWithZeros()));
            byte[] readData = new byte[mBufferSize];
            short[] convertShort = new short[mBufferSize/2];
            boolean oneKnock = false;
            currEnergy = 0;
            action = 0;
            int detectNum = 0;


            while(isRecording) {
                if(!oneKnock && detectNum > 0) {
                    if (detectNum < DETECT_THRESHOLD) {
                        Log.d(TAG, "NOT A KNOCK!");
                        isRecording2 = false;
                    }
                    else posDecision(false);
                    lWin = new Vector<>(Arrays.asList(initDoubleWithZeros()));
                    rWin = new Vector<>(Arrays.asList(initDoubleWithZeros()));
                    currEnergy = 0;
                    detectNum = 0;
                    Log.d(TAG, "DETECT DONE!");
                    continue;
                }
                else {
                    mAudioRecord.read(readData, 0, mBufferSize);
                }
                ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(convertShort);
                for(int i = 0; i < mBufferSize/4; i++) {
                    if (detect(convertShort[2*i]/32768.0, convertShort[2*i+1]/32768.0, detectNum, false)) {
                        if (detectNum == DETECT_DOUBLEKNOCK) {
                            new mDoubleKnockThread().start();
                        }
                        oneKnock = !(detectNum++ > DETECT_THRESHOLD);
                        break;
                    }
                    else {
                        oneKnock = false;
                    }
                }
            }
        }
    }

    public class mDoubleKnockThread extends Thread {
        public mDoubleKnockThread() {
        }

        public void run() {
            Log.d(TAG2, "DOUBLE KNOCK DETECT START");
            lWin2 = new Vector<>(Arrays.asList(initDoubleWithZeros()));
            rWin2 = new Vector<>(Arrays.asList(initDoubleWithZeros()));
            byte[] readData = new byte[mBufferSize];
            short[] convertShort = new short[mBufferSize/2];
            boolean oneKnock = false;
            dblEnergy = 0;
            int detectNum2 = 0;
            isRecording2 = true;
            int loopCount = 0;

            while(loopCount++ < 70 && isRecording2) {
                if(!oneKnock && detectNum2 > 0) {
                    if (detectNum2 < DETECT_THRESHOLD) {
                        Log.d(TAG2, "NOT A KNOCK!");
                    }
                    else posDecision(true);
                    isRecording2 = false;
                }
                else {
                    mAudioRecord2.read(readData, 0, mBufferSize);
                }
                ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(convertShort);
                for(int i = 0; i < mBufferSize/4; i++) {
                    if (detect(convertShort[2*i]/32768.0, convertShort[2*i+1]/32768.0, detectNum2, true)) {
                        oneKnock = !(detectNum2++ > DETECT_THRESHOLD);
                        break;
                    }
                    else {
                        oneKnock = false;
                    }
                }
            }

            Log.d(TAG2, "DOUBLE DONE!");
            performAction(action);
            action = 0;
        }
    }

    private boolean detect(double lData, double rData, int count, boolean isDouble) {
        double tmpL;
        if (!isDouble) {
            tmpL = lWin.remove(0);
            lWin.add(lData);
            rWin.remove(0);
            rWin.add(rData);
            currEnergy += (lData * lData - tmpL * tmpL);
            if (currEnergy > ENERGY_THRESHOLD) {
                if (count > 44 && count < 50) {
                    Double[] mLWindowsArr = lWin.toArray(new Double[WINDOW_LENGTH * 2]);
                    System.arraycopy(initDoubleWithZeros(), 0, mLWindowsArr, WINDOW_LENGTH, WINDOW_LENGTH);
                    Double[] mRWindowsArr = rWin.toArray(new Double[WINDOW_LENGTH * 2]);
                    System.arraycopy(initDoubleWithZeros(), 0, mRWindowsArr, WINDOW_LENGTH, WINDOW_LENGTH);
                    classify(mLWindowsArr, mRWindowsArr, false);
                }
                return true;
            }
        } else {
            tmpL = lWin2.remove(0);
            lWin2.add(lData);
            rWin2.remove(0);
            rWin2.add(rData);
            dblEnergy += (lData * lData - tmpL * tmpL);
            if (dblEnergy > ENERGY_THRESHOLD) {
                if (count > 44 && count < 50) {
                    Double[] mLWindowsArr = lWin2.toArray(new Double[WINDOW_LENGTH*2]);
                    System.arraycopy(initDoubleWithZeros(), 0, mLWindowsArr, WINDOW_LENGTH, WINDOW_LENGTH);
                    Double[] mRWindowsArr = rWin2.toArray(new Double[WINDOW_LENGTH*2]);
                    System.arraycopy(initDoubleWithZeros(), 0, mRWindowsArr, WINDOW_LENGTH, WINDOW_LENGTH);
                    classify(mLWindowsArr, mRWindowsArr, true);
                }
                return true;
            }
        }
        return false;
    }

    private void classify(Double[] lData, Double[] rData, boolean isDouble) {
        int idx = -1;
        double maxV = Double.MIN_VALUE;
        int n = lData.length;

        Complex[] lComplex = new Complex[n];
        Complex[] rComplex = new Complex[n];
        for (int i = 0; i < n; i++) {
            lComplex[i] = new Complex(lData[i], 0);
            rComplex[i] = new Complex(rData[i], 0);
        }
        Complex[] fftS = FFT.fft(lComplex);
        Complex[] fftT = FFT.fft(rComplex);
        for (int i = 0; i < fftS.length; i++) {
            fftS[i] = fftS[i].conjugate();
        }
        Complex[] timeProduct = new Complex[fftS.length];
        for (int i = 0; i < fftS.length; i++) {
            timeProduct[i] = fftS[i].times(fftT[i]);
        }
        Complex[] y = FFT.ifft(timeProduct);

        for(int x = 0; x < y.length; x++)
        {
            if(y[x].abs() > maxV)
            {
                maxV = y[x].abs();
                idx = x;
            }
        }
        Log.d(TAG, "IDX: " + idx);
        if (idx < 3 || idx > WINDOW_LENGTH * 2 - 7) {
            if (isDouble) {
                numCenter2++;
            }
            else {
                numCenter++;
            }
        }
        else if (idx < WINDOW_LENGTH) {
            if (isDouble) {
                numUp2++;
            }
            else {
                numUp++;
            }
        }
        else {
            if (isDouble) {
                numDown2++;
            }
            else {
                numDown++;
            }
        }
    }

    private void posDecision(boolean isDouble) {
        if (!isDouble) {
            if (numCenter > numDown) {
                if (numCenter > numUp) {
                    action = 10;
                } else {
                    action = 20;
                }
            } else {
                if (numDown > numUp) {
                    action = 30;
                } else {
                    action = 20;
                }
            }
            numCenter = 0;
            numDown = 0;
            numUp = 0;
        }
        else {
            if (numCenter2 > numDown2) {
                if (numCenter2 > numUp2) {
                    action += 1;
                } else {
                    action += 2;
                }
            } else {
                if (numDown2 > numUp2) {
                    action += 3;
                } else {
                    action += 2;
                }
            }
            numCenter2 = 0;
            numDown2 = 0;
            numUp2 = 0;
        }
    }

    private void performAction(int status) {
        MediaPlayer mp;
        switch(status) {
            case 10:
                Log.d(TAG2, "POS: CENTER");
                mp = MediaPlayer.create(this, R.raw.center);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                if (isMute) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND);
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "UNMUTED", Toast.LENGTH_SHORT).show());
                    isMute = false;
                }
                else {
                    audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND);
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "MUTED", Toast.LENGTH_SHORT).show());
                    isMute = true;
                }
                break;
            case 20:
                Log.d(TAG2, "POS: UP");
                mp = MediaPlayer.create(this, R.raw.up);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                startActivity(webIntent);
                break;
            case 30:
                Log.d(TAG2, "POS: DOWN");
                mp = MediaPlayer.create(this, R.raw.down);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "DOWN", Toast.LENGTH_SHORT).show());
                break;
            case 11:
                Log.d(TAG2, "POS: CENTER+CENTER");
                mp = MediaPlayer.create(this, R.raw.center_center);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                startActivity(mainIntent);
                break;
            case 12:
                Log.d(TAG2, "POS: CENTER+UP");
                mp = MediaPlayer.create(this, R.raw.center_up);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "CENTER+UP", Toast.LENGTH_SHORT).show());
                break;
            case 13:
                Log.d(TAG2, "POS: CENTER+DOWN");
                mp = MediaPlayer.create(this, R.raw.center_down);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                startActivity(dialIntent);
                startActivity(callIntent);
                break;
            case 21:
                Log.d(TAG2, "POS: UP+CENTER");
                mp = MediaPlayer.create(this, R.raw.up_center);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "UP+CENTER", Toast.LENGTH_SHORT).show());
                break;
            case 22:
                Log.d(TAG2, "POS: UP+UP");
                mp = MediaPlayer.create(this, R.raw.up_up);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "VOLUME UP", Toast.LENGTH_SHORT).show());
                break;
            case 23:
                Log.d(TAG2, "POS: UP+DOWN");
                mp = MediaPlayer.create(this, R.raw.up_down);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "UP+DOWN", Toast.LENGTH_SHORT).show());
                stopSelf();
                break;
            case 31:
                Log.d(TAG2, "POS: DOWN+CENTER");
                mp = MediaPlayer.create(this, R.raw.down_center);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "DOWN+CENTER", Toast.LENGTH_SHORT).show());
                break;
            case 32:
                Log.d(TAG2, "POS: DOWN+UP");
                mp = MediaPlayer.create(this, R.raw.down_up);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "DOWN+UP", Toast.LENGTH_SHORT).show());
                break;
            case 33:
                Log.d(TAG2, "POS: DOWN+DOWN");
                // PHONE CALL ACTION;
                mp = MediaPlayer.create(this, R.raw.down_down);
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, "VOLUME DOWN", Toast.LENGTH_SHORT).show());
                break;
            default:
                Log.d(TAG2, "NOT A KNOCK!");
                break;
        }
    }

    public static Double[] initDoubleWithZeros() {
        Double[] dArr = new Double[WINDOW_LENGTH];
        for(int i = 0; i < WINDOW_LENGTH; i++) {
            dArr[i] = 0.0;
        }
        return dArr;
    }
}
