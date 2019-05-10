/**
 * @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.baizelmathew.spotifycontroller.service.ForeGroundServerService;
import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.ServiceBroadcastReceiver;
import com.google.gson.Gson;
import com.jgabrielfreitas.core.BlurImageView;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import static com.baizelmathew.spotifycontroller.spotifywrapper.Player.CLIENT_ID;
import static com.baizelmathew.spotifycontroller.spotifywrapper.Player.REDIRECT_URI;
import static com.baizelmathew.spotifycontroller.spotifywrapper.Player.REQUEST_CODE;

/**
 * MainActivity shown to user
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Creats the view and starts the foreground service
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String track = intent.getStringExtra(ForeGroundServerService.EXTRA_TRACK);
                        updateInfo(new Gson().fromJson(track, Track.class));
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

        broadcastForeGroundService(ForeGroundServerService.ACTION_START_FOREGROUND_SERVICE);

        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

    }
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                   Player.setAccessToken(response.getAccessToken());
                    break;
                default:
                    // Handle other cases
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

    private void updateInfo(Track t) {
        SpotifyAppRemote remote = Player.getInstance().getSpotifyAppRemote();

        if (t != null) {
            remote.getImagesApi().getImage(t.imageUri, Image.Dimension.LARGE).setResultCallback(new CallResult.ResultCallback<Bitmap>() {
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
    }
}
