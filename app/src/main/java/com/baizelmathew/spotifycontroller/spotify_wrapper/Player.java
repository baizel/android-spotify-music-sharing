/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.spotify_wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.mappers.jackson.JacksonMapper;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.concurrent.TimeUnit;

/**
 * calss to interface wit the Spotify SDK
 * Uses authorisation sdk to get access token that can be used on the web page
 */
public class Player {
    public static final int REQUEST_CODE = 1337;
    public static final String REDIRECT_URI = "https://www.baizelmathew.com/callback";
    private static final String TAG = "Spotify";
    public static final String CLIENT_ID = "05e5055c73a74eb8b8f536e3a2e5a3ac";
    private static Player instance = null;
    private static SpotifyAppRemote spotifyRemoteRef = null;
    private static String accessToken = null;
    private static UserQueue userQueue = null;
    private boolean isInit = false;

    private Player() {
        userQueue = new UserQueue();
    }

    public static void setAccessToken(String accessToken) {
        Player.accessToken = accessToken;
    }

    public static String getAccessToken() throws NoSuchFieldException {
        if (accessToken != null)
            return accessToken;
        throw new NoSuchFieldException("Access token not set yet");
    }

    public static synchronized Player getRawInstance() {
        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    public static synchronized Player getInitializedInstance() throws IllegalStateException {
        if (instance != null) {
            if (!instance.isInit) {
                throw new IllegalStateException("Init method not called yet!, Initialize Player first");
            }
        } else {
            throw new IllegalStateException("Init method not called yet!, Initialize Player first");
        }
        return instance;
    }

    /**
     * Connects to Spotify if there has not already been a connection made.
     * After connecting the SpotifyAppRemote will be passed can now register callback for updates on he app
     *
     * @param context
     * @param playerStateResultCallback
     */
    public void init(Context context, final OnEventCallback<PlayerState> playerStateResultCallback) {
        if (spotifyRemoteRef == null || !spotifyRemoteRef.isConnected()) {
            ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .setJsonMapper(JacksonMapper.create())
                    .build();
            SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    spotifyRemoteRef = spotifyAppRemote;
                    isInit = true;
                    spotifyRemoteRef.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                        @Override
                        public void onEvent(PlayerState playerState) {
                            onEventHandler(playerState);
                        }
                    });
                    spotifyRemoteRef.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                        @Override
                        public void onResult(PlayerState playerState) {
                            playerStateResultCallback.onResult(playerState);
                        }
                    });
                    Log.d(TAG, "Connected to spotify");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    playerStateResultCallback.onFailure(throwable);
                    Log.d(TAG, "Failed to Connect to Spotify");
                }
            });
        }
    }

    public void close() {
        SpotifyAppRemote.disconnect(spotifyRemoteRef);
        Log.d("Spotify", "Disconnected");
        spotifyRemoteRef = null;
        isInit = false;
    }

    public Bitmap getCurrentImageOfTrackBlocking(long timeout, TimeUnit timeUnit) throws Throwable {
        Result<PlayerState> playerState = spotifyRemoteRef.getPlayerApi().getPlayerState().await(timeout, timeUnit);
        if (playerState.isSuccessful()) {
            Result<Bitmap> imageResult = spotifyRemoteRef.getImagesApi().getImage(playerState.getData().track.imageUri).await(timeout, timeUnit);
            if (imageResult.isSuccessful()) {
                return imageResult.getData();
            }
            throw imageResult.getError();
        }
        throw playerState.getError();
    }

    public void getImageOfTrack(Track t, CallResult.ResultCallback<Bitmap> callback) {
        spotifyRemoteRef.getImagesApi().getImage(t.imageUri).setResultCallback(callback);
    }

    public String getPlayerStateBlocking(long timeout, TimeUnit timeUnit) throws Throwable {
        Result<PlayerState> playerState = spotifyRemoteRef.getPlayerApi().getPlayerState().await(timeout, timeUnit);
        if (playerState.isSuccessful()) {
            return JacksonMapper.create().toJson(playerState.getData());
        }
        throw playerState.getError();
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

    public CallResult<Empty> seekTo(long pos) {
        return spotifyRemoteRef.getPlayerApi().seekTo(pos);
    }

    public CallResult<Empty> addToQueue(String uri) {
        userQueue.addToQueue(uri);
        return spotifyRemoteRef.getPlayerApi().queue(uri);
    }

    public void getCurrentPlayerState(CallResult.ResultCallback<PlayerState> callback) {
        spotifyRemoteRef.getPlayerApi().getPlayerState().setResultCallback(callback);
    }

    public Subscription<PlayerState> getSubscriptionPlayerState() {
        return spotifyRemoteRef.getPlayerApi().subscribeToPlayerState();
    }

    public UserQueue getCustomQueue() {
        return userQueue;
    }

    private void updateUserQueue(final PlayerState playerState) {
        userQueue.onPlayerState(playerState);
    }


    private void onEventHandler(PlayerState playerState) {
        updateUserQueue(playerState);
    }
}
