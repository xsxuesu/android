package cn.com.yqhome.instrumentapp;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

import cn.com.yqhome.instrumentapp.Class.Live;
import cn.com.yqhome.instrumentapp.webRtc.PeerConnectionParameters;
import cn.com.yqhome.instrumentapp.webRtc.WebRtcClient;

/**
 * Created by depengli on 2017/10/15.
 */

public class LiveActivity extends Activity implements WebRtcClient.RtcListener{

    private final static String TAG = "LiveActivity";
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId ;

    private Live live;

    private Button backBtn;
    private ListView listView;

    private boolean hadPresent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_live);
//      获得live
        live = (Live) getIntent().getSerializableExtra(BaseUtils.INTENT_Live);


//        资源get
        backBtn = (Button)findViewById(R.id.live_btn_back);
        backBtn.getBackground().setAlpha(50);
        backBtn.setTextColor(R.color.white);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        listView = (ListView)findViewById(R.id.live_listview_chat);
        listView.getBackground().setAlpha(50);
//        listView.setAdapter();

        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext());
        client.roomid = live.id;
        client.isPresent = true;

    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if(client != null) {
            client.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(client != null) {
            client.onDestroy();
        }
    }

    @Override
    public void onCallReady(String callId) {
        Log.i(TAG,"callId:"+callId);
        try {
            client.joinRoom(client.roomid,callId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        if (callerId != null) {
//            try {
//                answer(callerId);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else {
//            没有主播client id
//            call(callId);
//        }
    }

    @Override
    public void onPresendId(String presendId) {
        Log.i(TAG,"presendId:"+presendId);
        callerId = presendId;
        if (callerId != null && !callerId.isEmpty()) {
            if (!hadPresent){
                hadPresent = true;
                try {
                    answer(callerId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
//            没有主播client id
//            call(callId);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "主播还在化妆，请稍后...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void answer(String callerId) throws JSONException {

        client.sendMessage(callerId,"阿毛", "init", null);
        if (client.isPresent){
            client.start(live.avator);
        }else{
            client.recieve(live.avator);
        }

    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.i(TAG,"local");
        if (client.isPresent){
            localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
            VideoRendererGui.update(localRender,
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                    LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                    scalingType);
        }

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        Log.i(TAG,"REMOTE:");
        if (!client.isPresent){
            remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
            VideoRendererGui.update(remoteRender,
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);
//            VideoRendererGui.update(localRender,
//                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
//                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
//                    scalingType);
        }

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }
}
