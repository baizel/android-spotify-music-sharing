/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.spotify_wrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baizelmathew.spotifycontroller.eventbus_messeages.PlayerActionEvent;
import com.baizelmathew.spotifycontroller.eventbus_messeages.PlayerStateEvent;
import com.baizelmathew.spotifycontroller.eventbus_messeages.TrackChangeEvent;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.mappers.jackson.JacksonMapper;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
        EventBus.getDefault().register(this);
    }

    public static void setAccessToken(String accessToken) {
        Player.accessToken = accessToken;
    }

    public static String getAccessToken() throws NoSuchFieldException {
        if (accessToken != null)
            return accessToken;
        throw new NoSuchFieldException("Access token not set yet");
    }

    private static synchronized Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    public static void start(Context context, final OnEventCallback<PlayerState> playerStateResultCallback) {
        final Player instance = getInstance();
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
                    instance.isInit = true;
                    spotifyRemoteRef.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                        @Override
                        public void onEvent(PlayerState playerState) {
                            instance.updateUserQueue(playerState);
                            EventBus.getDefault().postSticky(new PlayerStateEvent(playerState, userQueue));
                            EventBus.getDefault().postSticky(new TrackChangeEvent(playerState.track));
                            Log.d(TAG, "Evntes emitted");

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

    public static void close() {
        EventBus.getDefault().unregister(getInstance());
        if (spotifyRemoteRef != null) {
            SpotifyAppRemote.disconnect(spotifyRemoteRef);
            Log.d("Spotify", "Disconnected");
            spotifyRemoteRef = null;
        }
        getInstance().isInit = false;
    }

    @Subscribe
    public void onPLayerEvent(PlayerActionEvent playerActionEvent) {
        switch (playerActionEvent.getPlayerActions()) {
            case TOGGLE_PLAY:
                handlePlayState();
                break;
            case NEXT:
                nextTrack();
                break;
            case PREVIOUS:
                previousTrack();
                break;
            case ADD_TO_QUEUE:
                addToQueue(playerActionEvent.getPayload());
                break;
            case SEEK_TO:
                seekTo(Long.parseLong(playerActionEvent.getPayload()));
                break;
        }

    }

    public static Bitmap getCurrentImageOfTrackBlocking(long timeout, TimeUnit timeUnit) throws Throwable {
        Result<PlayerState> playerState = spotifyRemoteRef.getPlayerApi().getPlayerState().await(timeout, timeUnit);
        if (playerState.isSuccessful()) {
            return getImageWithUriBlocking(timeout, timeUnit, playerState.getData().track.imageUri);
        }
        throw playerState.getError();
    }

    public static Bitmap getImageWithUriBlocking(long timeout, TimeUnit timeUnit, ImageUri uri) throws Throwable {
        Result<Bitmap> imageResult = spotifyRemoteRef.getImagesApi().getImage(uri).await(timeout, timeUnit);
        if (imageResult.isSuccessful()) {
            return imageResult.getData();
        }
        throw imageResult.getError();
    }

    public static void getImageOfTrack(ImageUri uri, CallResult.ResultCallback<Bitmap> callback) {
        spotifyRemoteRef.getImagesApi().getImage(uri).setResultCallback(callback);
    }

    public static String getPlayerStateBlocking(long timeout, TimeUnit timeUnit) throws Throwable {
        Result<PlayerState> playerState = spotifyRemoteRef.getPlayerApi().getPlayerState().await(timeout, timeUnit);
        if (playerState.isSuccessful()) {
            return JacksonMapper.create().toJson(playerState.getData());
        }
        throw playerState.getError();
    }

    public static void getCurrentPlayerState(CallResult.ResultCallback<PlayerState> callback) {
        spotifyRemoteRef.getPlayerApi().getPlayerState().setResultCallback(callback);
    }

    public UserQueue getCustomQueue() {
        return userQueue;
    }

    private void updateUserQueue(final PlayerState playerState) {
        userQueue.onPlayerState(playerState);
    }

    private void nextTrack() {
        spotifyRemoteRef.getPlayerApi().skipNext();
    }

    private void previousTrack() {
        spotifyRemoteRef.getPlayerApi().skipPrevious();
    }

    private void pause() {
        spotifyRemoteRef.getPlayerApi().pause();
    }

    private void resume() {
        spotifyRemoteRef.getPlayerApi().resume();
    }

    private void seekTo(long pos) {
        spotifyRemoteRef.getPlayerApi().seekTo(pos);
    }

    private void addToQueue(String uri) {
        userQueue.addToQueue(uri);
        spotifyRemoteRef.getPlayerApi().queue(uri);
    }

    private void handlePlayState() {
        getCurrentPlayerState(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                if (playerState.isPaused)
                    resume();
                else
                    pause();
            }
        });
    }

}
