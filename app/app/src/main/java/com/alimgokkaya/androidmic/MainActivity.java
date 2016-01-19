package com.alimgokkaya.androidmic;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int SERVER_PORT = 9900;
    private EditText mEditIp;
    private RecordThread recordThread;
    private int sampleRate;
    private int bufferSize;
    private TextView mTextStatus;
    private View mBtnStart;
    private View mBtnStop;


    public class UdpStreamClient implements RecordingListener {

        private InetAddress hostAddress;
        private DatagramSocket mSocket;
        private int packetIndex = 0;

        public UdpStreamClient(String ip) {
            try {
                hostAddress =  InetAddress.getByName(ip);
                mSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBytes(int freq, byte[] buffer, int numBytes) {
            byte[] message = ByteBuffer.allocate(numBytes + 8)
                    .putInt(freq)
                    .putInt(packetIndex)
                    .put(buffer, 0, numBytes)
                    .array();
            DatagramPacket p = new DatagramPacket(message, message.length, hostAddress, SERVER_PORT);
            try {
                mSocket.send(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
            packetIndex++;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextStatus = (TextView) findViewById(R.id.tv_status);
        mEditIp = (EditText) findViewById(R.id.editText);
        mBtnStart = findViewById(R.id.btn_start);
        mBtnStop = findViewById(R.id.btn_stop);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = mEditIp.getText().toString();
                if (recordThread == null) {
                    recordThread = new RecordThread(sampleRate, bufferSize, new UdpStreamClient(ip));
                    recordThread.start();
                    showRecording(true);
                }
            }
        });
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recordThread != null) {
                    recordThread.finish();
                    //byte buffer[] = recordThread.getData();
                    //thread.setSoundData(buffer,(int)(200*mRecordRatio),buffer.length/2-1);
                    recordThread = null;
                    showRecording(false);
                }
            }
        });

        int[] srinfo = RecordThread.findSampleRate();
        sampleRate = srinfo[0];
        bufferSize = srinfo[1];
        Log.d("Main", "Recording "+sampleRate+" "+bufferSize);
        showRecording(false);
        mTextStatus.setText("Ready for streaming");
    }

    private void showRecording(boolean recording) {
        if(recording) {
            mTextStatus.setText("Streaming");
            mBtnStop.setVisibility(View.VISIBLE);
            mBtnStart.setVisibility(View.GONE);
            mEditIp.setEnabled(false);
        } else {
            mTextStatus.setText("Stopped. Ready for streaming");
            mBtnStop.setVisibility(View.GONE);
            mBtnStart.setVisibility(View.VISIBLE);
            mEditIp.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
