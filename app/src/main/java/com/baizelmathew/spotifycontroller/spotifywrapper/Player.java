/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.spotifywrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baizelmathew.spotifycontroller.web.WebServer;
import com.baizelmathew.spotifycontroller.web.utils.OnEventCallback;
import com.google.gson.Gson;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.net.NoRouteToHostException;

/**
 * calss to interface wit the Spotify SDK
 * Uses authorisation sdk to get access token that can be used on the web page
 */
public class Player {
    public static final int REQUEST_CODE = 1337;
    public static final String REDIRECT_URI = "https://www.baizelmathew.com/callback";
    private static final String TAG = "Spotify";
    public static final String CLIENT_ID = "05e5055c73a74eb8b8f536e3a2e5a3ac";
    private static ConnectionParams connectionParams = null;
    private static Player instance = null;
    private static SpotifyAppRemote spotifyRemoteRef = null;
    private static String accessToken = null;
    private static UserQueue userQueue = null;
    private String cachedPlayerState = null;
    private PlayerState cachedRawPlayerState = null;
    private Bitmap currentTrackImage = null;
    private Subscription.EventCallback<PlayerState> outsideEventNotifier = null;

    private Player() {
        userQueue = new UserQueue();
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
        cachedPlayerState = gsonState;
    }

    public String getCachedPlayerState() {
        return cachedPlayerState;
    }

    public static Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    /**
     * Connects to Spotify if there has not already been a connection made.
     * After connecting the SpotifyAppRemote will be passed can now register callback for updates on he app
     *
     * @param context
     * @param callback
     * @param playerStateEventCallback
     */
    public void connect(Context context, final Connector.ConnectionListener callback, final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        if (spotifyRemoteRef == null || !spotifyRemoteRef.isConnected()) {
            SpotifyAppRemote.connect(context, connectionParams, getConnectionListener(callback, playerStateEventCallback));
        }
    }

    public void disconnect() {
        SpotifyAppRemote.disconnect(spotifyRemoteRef);
    }

    public void getImageOfTrack(Track t, final CallResult.ResultCallback<Bitmap> callback) {
        spotifyRemoteRef.getImagesApi()
                .getImage(t.imageUri, Image.Dimension.LARGE)
                .setResultCallback(new CallResult.ResultCallback<Bitmap>() {
                    @Override
                    public void onResult(Bitmap bitmap) {
                        setCurrentTrackImage(bitmap);
                        try {
                            WebServer.getInstance().broadcast();
                        } catch (NoRouteToHostException e) {
                            e.printStackTrace();
                        }
                        callback.onResult(bitmap);
                    }
                });
    }

    public void getCurrentImageOfTrack(final CallResult.ResultCallback<Bitmap> callback) {
        spotifyRemoteRef.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                getImageOfTrack(playerState.track, callback);
            }
        });
    }

    public CallResult<Empty> nextTrack() {
        return spotifyRemoteRef.getPlayerApi().skipNext();
    }

    public CallResult<Empty> previousTrack() {
        return spotifyRemoteRef.getPlayerApi().skipPrevious();
    }

    public CallResult<Empty> pause() {
        return spotifyRemoteRef.getPlayerApi().pause();
    }

    public CallResult<Empty> resume() {
        return spotifyRemoteRef.getPlayerApi().resume();
    }

    public CallResult<Empty> addToQueue(String uri) {
        userQueue.addToQueue(uri);
        return spotifyRemoteRef.getPlayerApi().queue(uri);
    }

    public void getPlayerState(final OnEventCallback callback) {
        spotifyRemoteRef.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                callback.onEvent(playerState);
            }
        });
    }

    public void subscribeToPlayerState(Subscription.EventCallback<PlayerState> callback) {
        spotifyRemoteRef.getPlayerApi().subscribeToPlayerState().setEventCallback(callback);
    }

    public UserQueue getCustomQueue() {
        return userQueue;
    }

    private void subscribeToStateChange(final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        Subscription<PlayerState> subscription = spotifyRemoteRef.getPlayerApi().subscribeToPlayerState();
        outsideEventNotifier = playerStateEventCallback;
        subscription.setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                cachedRawPlayerState = playerState;
                onPlayerStateEvent(playerState, playerStateEventCallback);
            }
        });
    }

    private void onPlayerStateEvent(final PlayerState playerState, final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        userQueue.onPlayerState(playerState);
        Gson g = new Gson();
        updatePlayerState(g.toJson(playerState));
        playerStateEventCallback.onEvent(playerState);
    }

    private Connector.ConnectionListener getConnectionListener(final Connector.ConnectionListener callback, final Subscription.EventCallback<PlayerState> playerStateEventCallback) {
        return new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                handleOnSpotifyRemoteConnect(spotifyAppRemote, playerStateEventCallback, callback);
            }

            @Override
            public void onFailure(Throwable throwable) {
                handleOnError(throwable, callback);
            }
        };
    }

    private void handleOnError(Throwable throwable, Connector.ConnectionListener callback) {
        Log.e(TAG, throwable.getMessage(), throwable);
        callback.onFailure(throwable);
    }

    private void handleOnSpotifyRemoteConnect(SpotifyAppRemote spotifyAppRemote, Subscription.EventCallback<PlayerState> playerStateEventCallback, Connector.ConnectionListener callback) {
        spotifyRemoteRef = spotifyAppRemote;
        //Subscribe to any player events such as music change
        subscribeToStateChange(playerStateEventCallback);
        callback.onConnected(spotifyRemoteRef);
        Log.d(TAG, "Connected to spotify");
    }

    public Bitmap getCurrentTrackImage() {
        return currentTrackImage;
    }

    public void setCurrentTrackImage(Bitmap currentTrackImage) {
        this.currentTrackImage = currentTrackImage;
    }
}
