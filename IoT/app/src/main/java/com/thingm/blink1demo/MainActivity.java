/*
 * Copyright 2020 Tod E. Kurt / todbot.com
 *
 */

package com.thingm.blink1demo;


import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.thingm.blink1.Blink1;
import com.thingm.blink1.Blink1Finder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity

{
    private static final String TAG = "BLINK1DEMO";
    private static final String ACTION_USB_PERMISSION = "com.thingm.blink1demo.action.USB_PERMISSION";
    private PendingIntent permissionIntent;

    Blink1Finder blink1Finder;
    Blink1 blink1;
    SeekBar seekBarR;
    SeekBar seekBarG;
    SeekBar seekBarB;
    TextView statusText;
    TextView serialText;

    int r = 0;
    int g = 0;
    int b = 0;

    SeekBar.OnSeekBarChangeListener seekbarChange;

    int count = 0;
    int length = 10240;
    private Socket socket;
    private static int SERVERPORT = 9003;
    private static long INTERVAL = 1000;
    private static String SERVER_IP = "192.168.1.206";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText=(TextView) findViewById(R.id.statusText);
        serialText=(TextView) findViewById(R.id.serialText);
        statusText.setText("looking for blink(1)");
        serialText.setText(" ");

        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        blink1Finder = new Blink1Finder();
        blink1Finder.setContext(this);
        blink1Finder.setPermissionIntent(permissionIntent);

        Log.d(TAG, "Looking for BLINK1");
        blink1 = blink1Finder.openFirst();

        if( blink1 != null ) {
            statusText.setText("blink(1) connected!");
            serialText.setText("serial:"+blink1.getSerialNumber()+", fw version:"+blink1.getVersion());
            blink1.off();
            Log.d(TAG,"blink1 serial:"+ blink1.getSerialNumber());
        }

        seekbarChange =  new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(      seekBar == seekBarR ) { r = progress; }
                else if( seekBar == seekBarG ) { g = progress; }
                else if( seekBar == seekBarB ) { b = progress; }
                Log.d(TAG, "onProgressChanged:"+progress+"  rgb:"+r+","+g+","+b);
                if( blink1!=null ) {
                    blink1.setRGB(r, g, b);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        };

        seekBarR=(SeekBar)findViewById(R.id.seekBarR);
        seekBarR.setOnSeekBarChangeListener(seekbarChange);
        seekBarG = (SeekBar) findViewById(R.id.seekBarG);
        seekBarG.setOnSeekBarChangeListener(seekbarChange);
        seekBarB = (SeekBar) findViewById(R.id.seekBarB);
        seekBarB.setOnSeekBarChangeListener(seekbarChange);
    }

    public void TCP(View view){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // get all information
                Button thebutton = (Button) findViewById(R.id.TheButton);
                thebutton.setClickable(false);
                thebutton.setTextColor(0xff888888); // gray color

                TextInputEditText theinterval = (TextInputEditText) findViewById(R.id.Interval);
                TextInputEditText theserveraddr = (TextInputEditText) findViewById(R.id.Input_Addr);
                TextInputEditText theserverport = (TextInputEditText) findViewById(R.id.Input_Port);

                INTERVAL = Long.parseLong(theinterval.getText().toString());
                SERVER_IP = theserveraddr.getText().toString();
                SERVERPORT = Integer.parseInt(theserverport.getText().toString());

                while (true) {
                    //  create a socket connection to the server
                    try {
                        InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                        Log.i(TAG, "New Socket");
                        socket = new Socket(serverAddr, SERVERPORT);

                    } catch (UnknownHostException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    //  send images continuously to the server
                    try {
                        final long StartTime = SystemClock.elapsedRealtime();
                        int num_point = 0;
                        while (true) {
                            count += 1;
                            final long StartTime2 = SystemClock.elapsedRealtime();
                            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            DataOutputStream dataOS2 = new DataOutputStream(socket.getOutputStream());
                            dataOS2.writeInt(8);
                            String rp = br.readLine() + System.getProperty("line.separator");
                            Log.i(TAG, "Response:" + rp);
                            final long allLatency = SystemClock.elapsedRealtime() - StartTime2;

                            String[] RGB = rp.split(",");
                            int R = 0;
                            int G = 0;
                            int B = 0;

                            try {
                                R = Integer.parseInt(RGB[0]);
                                G = Integer.parseInt(RGB[1]);
                                B = Integer.parseInt(RGB[2]);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                            // set RGB to LED
                            try {
                                blink1.setRGB(R, G, B);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                            //appendLog(Long.toString(allLatency3));
                            num_point =  num_point + 1; // plus 1
                            num_point = num_point % 3;  // rem
                            String points = ".";
                            for (int idx = 0; idx < num_point; idx++) {
                                points = points + points;
                            }
                            thebutton.setText("Requesting"+points);
                            // set the RGB value
                            seekBarR.setProgress(R);
                            seekBarG.setProgress(G);
                            seekBarB.setProgress(B);
                            // wait 1 sec to send
                            try {
                                TimeUnit.MILLISECONDS.sleep(INTERVAL);
                            } catch (InterruptedException e) {
                                System.out.println("sleep 1 sec is interrupted");
                            }
                        }

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // sleep for 1 second to retry if above exception is detected
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        System.out.println("sleep 1 sec is interrupted");
                    }
                }
            }
        };
        Thread hThread = new Thread(r);
        hThread.start();
    }


}
