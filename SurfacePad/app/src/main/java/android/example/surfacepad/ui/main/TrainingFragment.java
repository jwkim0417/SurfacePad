package android.example.surfacepad.ui.main;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.example.surfacepad.R;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class TrainingFragment extends Fragment {

    private final int mAudioSource = MediaRecorder.AudioSource.MIC;
    //private final int mAudioSource = MediaRecorder.AudioSource.MIC;
    private final int mSampleRate = 48000;
    private final short mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private final short mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);
    private String mFilepath = "";
    private String mFilename = "";

    public AudioRecord mAudioRecord = null;
    public AudioTrack mAudioTrack = null;
    public Thread mRecordThread = null;
    public Thread mPlayThread = null;
    public boolean isRecording = false;
    public boolean isPlaying = false;

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
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);

        mRecordThread = new Thread(() -> {
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
            mFilename = simpleDate.format(new Date(System.currentTimeMillis())) + ".wav";
            Log.d("FILENAME", mFilename);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Training");
            //values.put(MediaStore.Audio.Media.DISPLAY_NAME, "hi.wav");
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, mFilename);
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Audio.Media.IS_PENDING, 1);
            }

            ContentResolver contentResolver = Objects.requireNonNull(getContext()).getContentResolver();
            Uri item = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

            try {
                ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);
                if (pdf == null) {
                    Log.d("PDFF", "null");
                } else {
                    byte[] readData = new byte[mBufferSize];
                    FileOutputStream fos = new FileOutputStream(pdf.getFileDescriptor());
                    //mFilepath = getRealPathFromURI(getContext(), item);
                    Log.d("FILEPATH", mFilepath);
                    while (isRecording) {
                        int ret = mAudioRecord.read(readData, 0, mBufferSize);
                        Log.d("TAG", "read bytes is " + ret);
                        try {
                            writeWavHeader(fos, mChannelCount, mSampleRate, mAudioFormat);
                            fos.write(readData, 0, mBufferSize);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;

                    fos.close();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear();
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
                        contentResolver.update(item, values, null, null);
                    }

                    AssetFileDescriptor adf = contentResolver.openAssetFileDescriptor(item, "r", null);
                    long length = adf.getLength();
                    updateWavHeader(pdf.getFileDescriptor(), length);
//                    File wav = new File(mFilepath);
//                    updateWavHeader(wav);
                    Log.d("UPDATE", "DONE");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        mPlayThread = new Thread(() -> {
            byte[] writeData = new byte[mBufferSize];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(mFilepath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            DataInputStream dis = new DataInputStream(fis);
            mAudioTrack.play();

            while (isPlaying) {
                try {
                    int ret = dis.read(writeData, 0, mBufferSize);
                    if (ret <= 0) {

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;

            try {
                dis.close();
                fis.close();
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
        Button bt_play = view.findViewById(R.id.bt_play);
        bt_record.setOnClickListener(this::onRecord);
        bt_play.setOnClickListener(this::onPlay);
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

    public void onPlay(View view) {
        Button b = (Button)view;
        if (isPlaying) {
            isPlaying = false;
            b.setText("Play");
        }
        else {
            isPlaying = true;
            b.setText("Stop");

            if(mAudioTrack == null) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);
            }
            mPlayThread.start();
        }
    }

    public static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();
        out.write(new byte[]{
                'R', 'I', 'F', 'F', // Chunk ID
                0, 0, 0, 0, // Chunk Size
                'W', 'A', 'V', 'E', // Format
                'f', 'm', 't', ' ', //Chunk ID
                16, 0, 0, 0, // Chunk Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // Num of Channels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // Byte Rate
                littleBytes[10], littleBytes[11], // Block Align
                littleBytes[12], littleBytes[13], // Bits Per Sample
                'd', 'a', 't', 'a', // Chunk ID
                0, 0, 0, 0, //Chunk Size (나중에 업데이트 될 것)
        });
    }

    public static void updateWavHeader(FileDescriptor fd, long length) throws IOException {
        byte[] sizes1 = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (length - 8))
                .array();
        byte[] sizes2 = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (length - 44))
                .array();
        FileOutputStream fos = new FileOutputStream(fd);
        try {
            FileChannel ch = fos.getChannel();
            ch.position(4);
            ch.write(ByteBuffer.wrap(sizes1));
            ch.position(40);
            ch.write(ByteBuffer.wrap(sizes2));
        } catch (IOException e) {
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Audio.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}