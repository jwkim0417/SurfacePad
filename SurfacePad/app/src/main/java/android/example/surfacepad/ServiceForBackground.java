package android.example.surfacepad;

import android.app.Service;
import android.content.Intent;
import android.example.surfacepad.util.Complex;
import android.example.surfacepad.util.FFT;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Vector;

public class ServiceForBackground extends Service {
    private static final String TAG = "BackgroundService";
    private static final String TAG2 = "DOUBLE";
    private static final double DETECT_THRESHOLD = 50;
    private static final double DETECT_DOUBLEKNOCK = 5;
    private static final double ENERGY_THRESHOLD = 3;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "START!");
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        mAudioRecord.startRecording();
        mAudioRecord2 = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        mAudioRecord2.startRecording();
        isRecording = true;
        new mRecordThread().start();
        return START_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        mAudioRecord2.stop();
        mAudioRecord2.release();
        mAudioRecord2 = null;
        Log.d(TAG, "DESTROYED!!");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                if (count > 22 && count < 28) {
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
                if (count > 22 && count < 28) {
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
        if (idx < 3 || idx > WINDOW_LENGTH * 2 - 7) {
//            Log.d(TAG, "CENTER: " + idx);
            if (isDouble) {
                numCenter2++;
            }
            else {
                numCenter++;
            }
        }
        else if (idx < WINDOW_LENGTH) {
//            Log.d(TAG, "UP: " + idx);
            if (isDouble) {
                numUp2++;
            }
            else {
                numUp++;
            }
        }
        else {
//            Log.d(TAG, "DOWN: " + idx);
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
//                    Log.d(TAG, "POS: CENTER");
                } else {
                    action = 20;
//                    Log.d(TAG, "POS: UP");
                }
            } else {
                if (numDown > numUp) {
                    action = 30;
//                    Log.d(TAG, "POS: DOWN");
                } else {
                    action = 20;
//                    Log.d(TAG, "POS: UP");
                }
            }
//            Log.d(TAG, "COUNT: " + detectNum);
            numCenter = 0;
            numDown = 0;
            numUp = 0;
        }
        else {
            if (numCenter2 > numDown2) {
                if (numCenter2 > numUp2) {
                    action += 1;
//                    Log.d(TAG2, "POS: CENTER");
                } else {
                    action += 2;
//                    Log.d(TAG2, "POS: UP");
                }
            } else {
                if (numDown2 > numUp2) {
                    action += 3;
//                    Log.d(TAG2, "POS: DOWN");
                } else {
                    action += 2;
//                    Log.d(TAG2, "POS: UP");
                }
            }
//            Log.d(TAG2, "COUNT: " + detectNum2);
            numCenter2 = 0;
            numDown2 = 0;
            numUp2 = 0;
        }
    }

    private void performAction(int status) {
        switch(status) {
            case 10:
                Log.d(TAG2, "POS: CENTER");
                break;
            case 20:
                Log.d(TAG2, "POS: UP");
                break;
            case 30:
                Log.d(TAG2, "POS: DOWN");
                break;
            case 11:
                Log.d(TAG2, "POS: CENTER+CENTER");
                onDestroy();
                break;
            case 12:
                Log.d(TAG2, "POS: CENTER+UP");
                break;
            case 13:
                Log.d(TAG2, "POS: CENTER+DOWN");
                break;
            case 21:
                Log.d(TAG2, "POS: UP+CENTER");
                break;
            case 22:
                Log.d(TAG2, "POS: UP+UP");
                break;
            case 23:
                Log.d(TAG2, "POS: UP+DOWN");
                break;
            case 31:
                Log.d(TAG2, "POS: DOWN+CENTER");
                break;
            case 32:
                Log.d(TAG2, "POS: DOWN+UP");
                break;
            case 33:
                Log.d(TAG2, "POS: DOWN+DOWN");
                break;
            default:
                Log.d(TAG2, "NOT A KNOCK!");
                break;
        }
        Log.d(TAG2, "ACTION: " + action);
    }

    public static Double[] initDoubleWithZeros() {
        Double[] dArr = new Double[WINDOW_LENGTH];
        for(int i = 0; i < WINDOW_LENGTH; i++) {
            dArr[i] = 0.0;
        }
        return dArr;
    }
}
