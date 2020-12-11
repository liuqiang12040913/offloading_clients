package io.antmedia.android;

import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import io.antmedia.android.liveVideoBroadcaster.*;
import io.antmedia.android.liveVideoPlayer.LiveVideoPlayerActivity;
import io.antmedia.android.liveVideoBroadcaster.R;

public class MainActivity extends AppCompatActivity {

    /**
     * PLEASE WRITE RTMP BASE URL of the your RTMP SERVER.
     */
    public static String RTMP_BASE_URL = "rtmp://192.168.1.206/LiveApp/";
    public static int SERVERPORT = 9002;
    public static String SERVER_IP = "192.168.1.206";
    private TextInputEditText theserveraddr, theserverport, thertmpurl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.antmedia.android.liveVideoBroadcaster.R.layout.activity_main);
        theserveraddr = (TextInputEditText) findViewById(R.id.Input_Addr);
        theserverport = (TextInputEditText) findViewById(R.id.Input_Port);
        thertmpurl = (TextInputEditText) findViewById(R.id.RTMP_URL);

    }

    public void set_configurations(View view){
        SERVER_IP = theserveraddr.getText().toString();
        SERVERPORT = Integer.parseInt(theserverport.getText().toString());
        RTMP_BASE_URL = thertmpurl.getText().toString();
    }

    public void openVideoBroadcaster(View view) {
        set_configurations(view); // no matter which program starts, set configuration first
        Intent i = new Intent(this, LiveVideoBroadcasterActivity.class);
        startActivity(i);
    }

    public void openVideoPlayer(View view) {
        set_configurations(view); // no matter which program starts, set configuration first
        Intent i = new Intent(this, LiveVideoPlayerActivity.class);
        startActivity(i);
    }
}
