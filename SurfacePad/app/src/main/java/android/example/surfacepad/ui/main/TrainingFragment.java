package android.example.surfacepad.ui.main;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TrainingFragment extends Fragment {
    private static final String TAG = "TRAINING";

    private final int mAudioSource = MediaRecorder.AudioSource.MIC;
    private final int mSampleRate = 44100;
    private final short mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private final short mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);
    private String mFilename = "";

    private AudioRecord mAudioRecord = null;
    private Thread mRecordThread = null;
    private boolean isRecording = false;

    public TrainingFragment() {
        // Required empty public constructor
    }

//    private AudioRecord findAudioRecord() {
//        for (int rate : new int[] {44100, 48000}) {
//            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
//                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
//                    try {
//                        Log.d("AudiopRecording", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
//                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
//                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
//                            AudioRecord recorder = new AudioRecord(mAudioSource, rate, channelConfig, audioFormat, bufferSize);
//                            Log.d("STATE", "val: " + recorder.getState());
//                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
//                                return recorder;
//                        }
//                    } catch (Exception e) {
//                        Log.e("AudiopRecording", rate + "Exception, keep trying.",e);
//                    }
//                }
//            }
//        }
//        return null;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        //mAudioRecord = findAudioRecord();
        mAudioRecord.startRecording();

        mRecordThread = new Thread(() -> {
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
            mFilename = simpleDate.format(new Date(System.currentTimeMillis())) + ".pcm";
            Log.d("FILENAME", mFilename);

            byte[] readData = new byte[mBufferSize];
            FileOutputStream fos = null;
            try {
                fos = getContext().openFileOutput(mFilename, Context.MODE_APPEND);
                Log.d("PATH", getContext().getFileStreamPath(mFilename).toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(isRecording) {
                int ret = mAudioRecord.read(readData, 0, mBufferSize);
                Log.d(TAG, "read bytes is " + ret);
                try {
                    fos.write(readData, 0, mBufferSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;

            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training, container, false);
    }

    @Override
    public void onViewCreated (@NotNull View view,
                               Bundle savedInstanceState) {
        Button bt_record = view.findViewById(R.id.bt_record);
        bt_record.setOnClickListener(this::onRecord);
    }

    public void onRecord(View view) {
        Button b = (Button)view;
        if (isRecording) {
            isRecording = false;
            b.setText("Record");
        }
        else {
            isRecording = true;
            b.setText("Stop");

            if(mAudioRecord == null) {
                mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
                mAudioRecord.startRecording();
            }
            mRecordThread.start();
        }
    }
}