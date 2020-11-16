/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baizelmathew.spotifycontroller.eventbus_messeages.ServerAddressEvent;
import com.baizelmathew.spotifycontroller.eventbus_messeages.TrackChangeEvent;
import com.baizelmathew.spotifycontroller.service.ForeGroundServerService;
import com.baizelmathew.spotifycontroller.service.ServiceBroadcastReceiver;
import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.jgabrielfreitas.core.BlurImageView;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.CLIENT_ID;
import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.REDIRECT_URI;
import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.REQUEST_CODE;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            //Register received to so the view can be updated with track info and server address
            //Authenticate the Spotify SDK and get token
            AuthorizationRequest.Builder builder =
                    new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

            builder.setScopes(new String[]{"streaming"});
            AuthorizationRequest request = builder.build();
            AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                case TOKEN:
                    Player.setAccessToken(response.getAccessToken());
                    broadcastForeGroundService(ForeGroundServerService.ACTION_START_FOREGROUND_SERVICE);
                    break;
                case CODE:
                case EMPTY:
                case ERROR:
                case UNKNOWN:
                    //intentional fall through
                default:
                    //TODO:
                    // Handle other state Not connected or no auth
            }
        }
    }

    public void onClickStop(View v) {
        broadcastForeGroundService(ForeGroundServerService.ACTION_STOP_FOREGROUND_SERVICE);
    }

    public void onClickStart(View v) {
        broadcastForeGroundService(ForeGroundServerService.ACTION_START_FOREGROUND_SERVICE);
    }

    private void broadcastForeGroundService(String action) {
        //start Server
        Intent startForeGroundServiceIntent = new Intent(this, ServiceBroadcastReceiver.class);
        startForeGroundServiceIntent.setAction(action);
        sendBroadcast(startForeGroundServiceIntent);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateLink(ServerAddressEvent addressEvent) {
        TextView link = findViewById(R.id.link);
        link.setText(addressEvent.getServerAddr());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onTrackChange(TrackChangeEvent trackEvent) {
        try {
            Track currentTrack = trackEvent.getCurrentTrack();
            if (currentTrack != null) {
                TextView song = findViewById(R.id.songName);
                song.setText(currentTrack.name);
                TextView artist = findViewById(R.id.artist);
                artist.setText(currentTrack.artist.name);
                updateImage(currentTrack.imageUri);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void updateImage(ImageUri uri) {
        try {
            Player.getImageOfTrack(uri, new CallResult.ResultCallback<Bitmap>() {
                @Override
                public void onResult(Bitmap bitmap) {
                    BlurImageView img = findViewById(R.id.image);
                    img.setImageBitmap(bitmap);
                    img.setBlur(3);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
