package android.example.surfacepad.ui.main;

import android.graphics.Color;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecognitionFragment extends Fragment {
    private static final String TAG = "RECOGNITION";

    private final int mSampleRate = 48000;
    private final short mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private final short mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    private AudioRecord mAudioRecord = null;
    private boolean isRecording = false;

    private LineChart mChartL;
    private LineChart mChartR;
    private boolean plotDataL = true;
    private boolean plotDataR = true;
    private Thread thr;

    public RecognitionFragment() {
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
        return inflater.inflate(R.layout.fragment_recognition, container, false);
    }

    @Override
    public void onViewCreated (@NotNull View view,
                               Bundle savedInstanceState) {

        mChartL = view.findViewById(R.id.chartL);
        mChartR = view.findViewById(R.id.chartR);
        setChart(mChartL, true);
        setChart(mChartR, false);

        Button bt_recog = view.findViewById(R.id.bt_recog);
        bt_recog.setOnClickListener(this::onRecord);
    }

    private void setChart(LineChart mChart, boolean tf) {
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Real-Time Data Plot");

        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.WHITE);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis lAxis = mChart.getAxisLeft();
        lAxis.setTextColor(Color.WHITE);
        lAxis.setDrawGridLines(false);
//        lAxis.setAxisMaximum(200f);
//        lAxis.setAxisMinimum(-200f);
        lAxis.setDrawGridLines(true);

        YAxis rAxis = mChart.getAxisRight();
        rAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

        startPlot(tf);
    }

    public class mRecordThread extends Thread {
        public mRecordThread() {
            int mAudioSource = MediaRecorder.AudioSource.MIC;
            mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
            mAudioRecord.startRecording();
        }

        public void run() {
            byte[] readData = new byte[mBufferSize];
            byte[] lData = new byte[mBufferSize/2];
            byte[] rData = new byte[mBufferSize/2];

            while(isRecording) {
                mAudioRecord.read(readData, 0, mBufferSize);
//                for(byte dt : readData) {
//                    if(plotData) {
//                        addEntry(dt);
//                        plotData = false;
//                    }
//                }
                for(int i = 0; i < mBufferSize; i++) {
                    Log.d(TAG, "i: " + i/2);
                    if (i % 2 == 0) lData[i/2] = readData[i];
                    else rData[i/2] = readData[i];
                }
                if(plotDataL && plotDataR) {
                    addEntry(mChartL, byteArrToInt(lData));
                    addEntry(mChartR, byteArrToInt(rData));
                    plotDataL = false;
                    plotDataR = false;
                }
            }

            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private void startPlot(boolean tf) {
        if(thr != null) {
            thr.interrupt();
        }

        thr = new Thread(() -> {
            while (true) {
                if(tf) {
                    plotDataL = true;
                } else {
                    plotDataR = true;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thr.start();
    }

    private void addEntry(LineChart mChart, int dt) {
        LineData data = mChart.getLineData();

        if(data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if(set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), dt), 0);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(30);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    public void onRecord(View view) {
        Button b = (Button)view;
        if (isRecording) {
            isRecording = false;
            b.setText(R.string.bt_recog);
        }
        else {
            isRecording = true;
            b.setText(R.string.btn_stop);

            RecognitionFragment.mRecordThread t = new mRecordThread();
            t.start();
        }
    }

    private static int byteArrToInt(byte[] bytes) {
        final int size = Integer.SIZE / 8;
        ByteBuffer buff = ByteBuffer.allocate(size);
        final byte[] newBytes = new byte[size];
        for (int i = 0; i < size; i++) {
            if (i + bytes.length < size) {
                newBytes[i] = (byte) 0x00;
            } else {
                newBytes[i] = bytes[i + bytes.length - size];
            }
        }
        buff = ByteBuffer.wrap(newBytes);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        return buff.getInt();
    }
}