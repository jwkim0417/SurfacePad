package android.example.surfacepad.ui.main;

import android.example.surfacepad.util.Complex;
import android.example.surfacepad.util.FFT;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.example.surfacepad.R;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EtcFragment extends Fragment {
    private static final String TAG = "ETC";
    private static double DETECT_THRESHOLD = 70;
    private static double ENERGY_THRESHOLD = 3;
    private static int WINDOW_LENGTH = 128;
    private static int WINDOW_STRIDE = 1;

    private final int mSampleRate = 48000;
    private final short mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private final short mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    private AudioRecord mAudioRecord = null;
    private boolean isRecording = false;

    private double[] mLWindows = new double[WINDOW_LENGTH*2];
    private double[] mRWindows = new double[WINDOW_LENGTH*2];
    private double currentEnergy = 0;

    private int count;
    private int countU;
    private int countD;
    private int countC;

    public EtcFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_etc, container, false);
    }

    @Override
    public void onViewCreated (@NotNull View view,
                               Bundle savedInstanceState) {
        Button bt_record = view.findViewById(R.id.bt_record3);
        bt_record.setOnClickListener(this::onRecord);
    }

    public class mRecordThread extends Thread {
        public mRecordThread() {
            int mAudioSource = MediaRecorder.AudioSource.MIC;
            mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
            mAudioRecord.startRecording();
        }

        public void run() {
            byte[] readData = new byte[mBufferSize];
            short[] convertShort = new short[mBufferSize/2];
            boolean oneKnock = false;

            while(isRecording) {
                if(!oneKnock && (countC != 0 || countD != 0 || countU != 0)) posDecision();
                if(oneKnock) {
                    for(int j = 0; j < WINDOW_STRIDE; j++) {
                        mAudioRecord.read(readData, 0, mBufferSize);
                    }
                }
                else {
                    mAudioRecord.read(readData, 0, mBufferSize);
                    count = 0;
                }
                ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(convertShort);
                for(int i = 0; i < mBufferSize/4; i++) {
                    if (detect(convertShort[2*i]/32768.0, convertShort[2*i+1]/32768.0, count)) {
                        oneKnock = true;
                        count++;
                        break;
                    }
                    else if (oneKnock){
                        oneKnock = false;
                    }
                }
            }

            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private boolean detect(double lData, double rData, int count) {
        currentEnergy += (lData * lData - mLWindows[0] * mLWindows[0]);
        System.arraycopy(mLWindows, 1, mLWindows, 0, WINDOW_LENGTH-1);
        mLWindows[WINDOW_LENGTH-1] = lData;
        System.arraycopy(mRWindows, 1, mRWindows, 0, WINDOW_LENGTH-1);
        mRWindows[WINDOW_LENGTH-1] = rData;
        if (currentEnergy > ENERGY_THRESHOLD && count < 5) {
            classify(mLWindows, mRWindows);
            return true;
        }
        else {
            //Log.d(TAG, "COUNT_NUM: " + count);
        }
        return false;
    }

    private void classify(double[] lData, double[] rData) {

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
        if (idx < 4 || idx > WINDOW_LENGTH * 2 - 4) {
            Log.d(TAG, "CENTER");
            countC++;
        }
        else if (idx < WINDOW_LENGTH) {
            Log.d(TAG, "UP");
            countU++;
        }
        else {
            Log.d(TAG, "DOWN");
            countD++;
        }
    }

    private void posDecision() {
        if (countC > countD) {
            if (countC > countU) {
//                if (countC > DETECT_THRESHOLD)
                    Log.d(TAG, "POS: CENTER" + countC);
            }
            else {
//                if (countU > DETECT_THRESHOLD)
                    Log.d(TAG, "POS: RIGHT/UP" + countU);
            }
        }
        else {
            if (countD > countU) {
//                if (countD > DETECT_THRESHOLD)
                    Log.d(TAG, "POS: LEFT/DOWN" + countD);
            }
            else {
//                if (countU > DETECT_THRESHOLD)
                    Log.d(TAG, "POS: RIGHT/UP" + countU);
            }
        }
        countC = 0;
        countD = 0;
        countU = 0;
    }

    public void onRecord(View view) {
        Button b = (Button)view;
        if (isRecording) {
            isRecording = false;
            b.setText(R.string.bt_record);
        }
        else {
            b.setText(R.string.btn_stop);

            EtcFragment.mRecordThread t = new mRecordThread();
            t.start();
            isRecording = true;
        }
    }
}