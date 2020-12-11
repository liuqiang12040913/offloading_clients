package com.example.hwangsocket;
/* hwangsocket energy & accuracy test*/

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit ;



public class SocketImageClient extends AppCompatActivity {

    int count = 0;
    int length = 10240;
    private Socket socket;
    private static int SERVERPORT = 9001;
    private static String SERVER_IP = "192.168.1.206";
    private static final String TAG = "TheImageMsg"; // filter

    private TextView textView;
    //private Handler handler = null;
    Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_image_client);
    }



    public void TCP(View view){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // get all information
                Button thebutton = (Button) findViewById(R.id.TheButton);
                thebutton.setClickable(false);
                thebutton.setTextColor(0xff888888); // gray color

                TextView thetextview = (TextView) findViewById(R.id.TheLatencyView);
                TextView theresultview = (TextView) findViewById(R.id.TheResultView);

                TextInputEditText theserveraddr = (TextInputEditText) findViewById(R.id.Input_Addr);
                TextInputEditText theserverport = (TextInputEditText) findViewById(R.id.Input_Port);
                SERVER_IP = theserveraddr.getText().toString();
                SERVERPORT = Integer.parseInt(theserverport.getText().toString());

                TextInputEditText theimagelocation = (TextInputEditText) findViewById(R.id.ImageLocation);

                String path = Environment.getExternalStorageDirectory() + theimagelocation.getText().toString();

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
                            int id = 1;
                            Bitmap bitmap2 = getImages(path);
                            Log.i(TAG, "Get bitmap: " + id);
                            final long StartTime2 = SystemClock.elapsedRealtime();
                            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            DataOutputStream dataOS2 = new DataOutputStream(socket.getOutputStream());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            Log.i(TAG, "Setup ...");
                            bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] b = baos.toByteArray();
                            dataOS2.writeInt(b.length);
                            Log.i(TAG, "image size is" + b.length);
                            dataOS2.write(b);
                            Log.i(TAG, "Complete offloading frameID is" + id);
                            String rp = br.readLine() + System.getProperty("line.separator");
                            Log.i(TAG, "Response:" + rp);
                            final long allLatency = SystemClock.elapsedRealtime() - StartTime2;
                            Log.i(TAG, id + " latency2: " + allLatency );
                            appendLog(Long.toString(allLatency), "/TestResults/TCP/Latency.txt");
                            //appendLog(Long.toString(allLatency3));
                            num_point =  num_point + 1; // plus 1
                            num_point = num_point % 3;  // rem
                            String points = ".";
                            for (int idx = 0; idx < num_point; idx++) {
                                points = points + points;
                            }
                            thebutton.setText("Offloading"+points);
                            thetextview.setText("E2E Latency (ms): " + Long.toString(allLatency));
                            theresultview.setText("Matched ID: " + rp );
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


    public void EXIT(View view){
        finish();
        System.exit(0);
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }
    protected Bitmap getImages(String path){
        File mfile = new File(path);
        if (mfile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            Log.i(TAG, "Read image!");
            return bitmap;
        }else{
            Log.i(TAG, "Path not exist!");
            return null;
        }
    }

    /* Define the Thread */
    class TheImageThread implements Runnable{
        @Override
        public void run() {
            try{
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Log.i(TAG,"New Socket");
                socket = new Socket(serverAddr, SERVERPORT);

            }catch (UnknownHostException e1){
                e1.printStackTrace();
            }catch (IOException e1){
                e1.printStackTrace();
            }
        }
    }

    /** Save Log to file **/
    public void appendLog(String text, String path)
    {
        final long timetest0 = SystemClock.elapsedRealtime();//
        File logFile = new File(Environment.getExternalStorageDirectory() + path);
        //File logFile = new File(Environment.getExternalStorageDirectory() + "/TestResults/UDPlatencyframe.txt");

        if (!logFile.exists())
        {
            Log.i(TAG," no this file");
            try
            {
                Log.i(TAG," create file ");
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                //TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            //Log.i(TAG,"write........");
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
            final long delay0 = SystemClock.elapsedRealtime() - timetest0;//
            //Log.i(TAG,"delay0: " + delay0);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
