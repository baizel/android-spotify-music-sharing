package com.baizelmathew.spotifycontroller.spotifywrapper;

import android.content.Context;
import android.util.Log;

import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.google.gson.Gson;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

public class Player {
    public static final int REQUEST_CODE = 1337;
    public static final String REDIRECT_URI = "https://www.baizelmathew.com/callback";
    private static final String TAG = "Spotify"; // TODO: add this to class in utils
    public static final String CLIENT_ID = "05e5055c73a74eb8b8f536e3a2e5a3ac";
    private static ConnectionParams connectionParams = null;
    private static Player instance = null;
    private static SpotifyAppRemote mSpotifyAppRemote = null;
    private String initialPlayerState = null;
    private static String accessToken = null;

    private Player() {
        connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();
    }

    public static void setAccessToken(String accessToken) {
        Player.accessToken = accessToken;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    private void updatePlayerState(String gsonState) {
        initialPlayerState = gsonState;
    }

    public String getInitialPlayerState() {
        return initialPlayerState;
    }

    public static Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    public void connect(Context context, final Connector.ConnectionListener callback) {
        connect(context, callback, null);
    }

    public void connect(Context context, final Connector.ConnectionListener callback, final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        if (mSpotifyAppRemote == null || !mSpotifyAppRemote.isConnected()) {
            SpotifyAppRemote.connect(context, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            //Set spotify remote
                            mSpotifyAppRemote = spotifyAppRemote;
                            Subscription<PlayerState> subscription = mSpotifyAppRemote.getPlayerApi().subscribeToPlayerState();
                            subscription.setEventCallback(new Subscription.EventCallback<PlayerState>() {
                                @Override
                                public void onEvent(PlayerState playerState) {
                                    Gson g = new Gson();
                                    updatePlayerState(g.toJson(playerState));
                                    playerStateEventCallback.onEvent(playerState);
                                }
                            });
                            callback.onConnected(mSpotifyAppRemote);
                            Log.d(TAG, "Connected to spotify");

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

    public void getPlayerState(final OnEventCallback callback) {
        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                callback.onEvent(playerState);
            }
        });
    }

}
