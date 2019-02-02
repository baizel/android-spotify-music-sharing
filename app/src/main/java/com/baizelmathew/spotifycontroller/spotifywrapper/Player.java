package com.baizelmathew.spotifycontroller.spotifywrapper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

public class Player {
    private static final String TAG = "Spotify"; // add this to class in utils
    private static final String CLIENT_ID = "05e5055c73a74eb8b8f536e3a2e5a3ac";
    private static final String REDIRECT_URI = "https://www.baizelmathew.com/callback";
    private static SpotifyAppRemote mSpotifyAppRemote = null;
    private static ConnectionParams connectionParams = null;
    private static Player instance = null;
    private PlayerState mPlayerState = null;

    private Player() {
        connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

    }

    public static Player getInstance() {
        if (instance == null) {
            return new Player();
        }
        return instance;
    }

    public void connect(Context context,final Connector.ConnectionListener callback) {
        connect(context, callback,null);
    }

    public void connect(Context context, final Connector.ConnectionListener callback, final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        if (mSpotifyAppRemote == null || !mSpotifyAppRemote.isConnected()) {
            SpotifyAppRemote.connect(context, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            //Set spotify remote
                            mSpotifyAppRemote = spotifyAppRemote;
                            Log.d(TAG, "Connected to spotify");

                            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                                @Override
                                public void onResult(PlayerState playerState) {
                                    mPlayerState = playerState;
                                }
                            });

                            //Subscribe to playerState
                            Subscription<PlayerState> subscription = mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState();
                            subscription.setEventCallback(new Subscription.EventCallback<PlayerState>() {
                                @Override
                                public void onEvent(PlayerState playerState) {
                                    mPlayerState = playerState;
                                    if (playerStateEventCallback != null) {
                                        playerStateEventCallback.onEvent(mPlayerState);
                                    }
                                }
                            });

                            callback.onConnected(mSpotifyAppRemote);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(TAG, throwable.getMessage(), throwable);
                            callback.onFailure(throwable);
                        }
                    });
        }
    }

    public void disconnect() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    public SpotifyAppRemote getSpotifyAppRemote() {
        return mSpotifyAppRemote;
    }

    public PlayerState getPlayerState(){
        return  mPlayerState;
    }

}
