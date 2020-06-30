/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.baizelmathew.spotifycontroller.service.ForeGroundServerService;
import com.baizelmathew.spotifycontroller.service.ServiceBroadcastReceiver;
import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jgabrielfreitas.core.BlurImageView;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.CLIENT_ID;
import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.REDIRECT_URI;
import static com.baizelmathew.spotifycontroller.spotify_wrapper.Player.REQUEST_CODE;

/**
 * MainActivity shown to user
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Creats the view and starts the foreground service
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            //Register received to so the view can be updated with track info and server address
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String track = intent.getStringExtra(ForeGroundServerService.EXTRA_TRACK);
                            ObjectMapper mapper = new ObjectMapper();
                            try {
                                updateInfo(mapper.readValue(track, Track.class));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new IntentFilter(ForeGroundServerService.ACTION_TRACK_BROADCAST)
            );

            LocalBroadcastManager.getInstance(this).registerReceiver(
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String link = intent.getStringExtra(ForeGroundServerService.EXTRA_SERVER_ADDRESS);
                            updateLink(link);
                        }
                    }, new IntentFilter(ForeGroundServerService.ACTION_SERVER_ADDRESS_BROADCAST)
            );
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
//                            updateLink("Server Has stopped, Please start manually");
                        }
                    }, new IntentFilter(ForeGroundServerService.ACTION_SERVICE_STOPPED)
            );


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

    private void updateLink(String address) {
        TextView link = findViewById(R.id.link);
        link.setText(address);
    }

    /**
     * Updates the background image and track info on the app
     *
     * @param t
     */
    private void updateInfo(Track t) {
        try {
            Player player = Player.getInitializedInstance();
            if (t != null) {
                player.getImageOfTrack(t, new CallResult.ResultCallback<Bitmap>() {
                    @Override
                    public void onResult(Bitmap bitmap) {
                        BlurImageView img = findViewById(R.id.image);
                        img.setImageBitmap(bitmap);
                        img.setBlur(3);
                    }
                });
                TextView song = findViewById(R.id.songName);
                song.setText(t.name);
                TextView artist = findViewById(R.id.artist);
                artist.setText(t.artist.name);

            }
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
    }
}
