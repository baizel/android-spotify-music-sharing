package com.baizelmathew.spotifycontroller;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web.WebServer;
import com.jgabrielfreitas.core.BlurImageView;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Player p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        p = Player.getInstance();
        Subscription.EventCallback<PlayerState>  playerStateEventCallback = new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                updateInfo(playerState.track);
                WebServer w = WebServer.getInstance(getApplicationContext());

                updateLink(w.getHttpAddress());
                try {
                    w.start();
                    fu("it may have worked");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Connector.ConnectionListener connectionListener = new Connector.ConnectionListener() {

            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                p.getPlayerState(new OnEventCallback() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        updateInfo(playerState.track);
                    }
                });

                //launchServer();
            }

            @Override
            public void onFailure(Throwable throwable) {
                //TODO: Implement this
                fu("Reeeeee " + throwable.getLocalizedMessage());
            }
        };

        p.connect(this, connectionListener, playerStateEventCallback);

    }

    private void updateLink(String address) {
        TextView link = findViewById(R.id.link);
        link.setText(address);
    }

    private void fu(String s) {
        Toast toast = Toast.makeText(this, s, Toast.LENGTH_LONG);
        toast.show();
    }

    private void updateInfo(Track t) {
        SpotifyAppRemote remote = p.getSpotifyAppRemote();

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

    @Override
    protected void onStop() {
        super.onStop();
        p.disconnect();
        Toast toast = Toast.makeText(this, "Killing all servers", Toast.LENGTH_LONG);
        toast.show();
        WebServer.getInstance(getApplicationContext()).stop();
    }
}
