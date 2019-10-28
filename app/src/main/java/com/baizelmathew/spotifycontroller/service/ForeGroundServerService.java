/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.baizelmathew.spotifycontroller.R;
import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.ServiceBroadcastReceiver;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web.WebServer;
import com.google.gson.Gson;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.io.IOException;

/**
 * This class is responsible for managing the web server in the background.
 */
public class ForeGroundServerService extends Service {

    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public static final String ACTION_SERVER_ADDRESS_BROADCAST = ForeGroundServerService.class.getName() + "AddressBroadcast";
    public static final String ACTION_TRACK_BROADCAST = ForeGroundServerService.class.getName() + "TrackBroadcast";

    public static final String EXTRA_SERVER_ADDRESS = "extra_server_address";
    public static final String EXTRA_TRACK = "extra_track";

    private static final int ONGOING_NOTIFICATION_ID = 1234;
    private static final String ACTION_STOP_FOREGROUND_SERVICE_ID = "ACTION_STOP_FOREGROUND_SERVICE_ID";

    private WebServer webServer;
    private Player player;
    private ServiceBroadcastReceiver serviceBroadcastReceiver = new ServiceBroadcastReceiver();

    public ForeGroundServerService() {
        //https://developer.android.com/guide/components/services
    }

    private void debugToast(String s) {
        Toast toast = Toast.makeText(this, s, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Method used to broadcast the web server address to anything listening.
     * Used to retrieve the web server address by the main activity so it can be updated
     * and shown to the user when the server is launched.
     * @param serverLink as a http link
     */
    private void sendBroadcastAddress(String serverLink) {
        if (serverLink != null) {
            Intent intent = new Intent(ACTION_SERVER_ADDRESS_BROADCAST);
            intent.putExtra(EXTRA_SERVER_ADDRESS, serverLink);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Same as sendBroadcastAddress but for current song information.
     * @param track See Spotify track object
     */
    private void sendBroadcastTrack(Track track) {
        if (track != null) {
            Intent intent = new Intent(ACTION_TRACK_BROADCAST);
            intent.putExtra(EXTRA_TRACK, new Gson().toJson(track));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Creates a foreground service with always displaying notification on the channel
     * com.baizelmathew.mobiledev
     * The notification has the option to stop the server by using the button
     */
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.baizelmathew.mobiledev";
        String channelName = "Spotify Web Server";

        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;

        manager.createNotificationChannel(chan);

        Intent snoozeIntent = new Intent(this, ServiceBroadcastReceiver.class);
        snoozeIntent.setAction(ACTION_STOP_FOREGROUND_SERVICE);
        snoozeIntent.putExtra(ACTION_STOP_FOREGROUND_SERVICE_ID, 0);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);


        Notification notification = new NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_tap_and_play_black_24dp)
                .setContentTitle("Server running in background")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(R.drawable.ic_stop_black_24dp, "Stop Server", stopPendingIntent)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    /**
     * Initialises the web server and registers the stop service receiver.
     * Also connects Spotify SDK to client.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter stopServiceFilter = new IntentFilter(ACTION_STOP_FOREGROUND_SERVICE);
        stopServiceFilter.addAction(ACTION_STOP_FOREGROUND_SERVICE);
        this.registerReceiver(serviceBroadcastReceiver, stopServiceFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(ONGOING_NOTIFICATION_ID, new Notification());

        webServer = WebServer.getInstance();
        player = Player.getInstance();
        Subscription.EventCallback<PlayerState> playerStateEventCallback = new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                sendBroadcastAddress(webServer.getHttpAddress());
                sendBroadcastTrack(playerState.track);
                try {
                    webServer.startServer();
                } catch (IOException e) {
                    e.printStackTrace();
                    stopSelf();
                }
            }
        };

        Connector.ConnectionListener connectionListener = new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                webServer.startListening();
                player.getPlayerState(new OnEventCallback() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        sendBroadcastTrack(playerState.track);
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                stopSelf();
                debugToast("Service Not Started " + throwable.getLocalizedMessage());
            }
        };

        player.connect(this, connectionListener, playerStateEventCallback);
    }

    /**
     * Required for Service classes
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Required for Service classes
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Kills the server and disconnects Spotify when service is killed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        webServer.stop();
        player.disconnect();
        this.unregisterReceiver(serviceBroadcastReceiver);
        debugToast("Stopping Web Server ");

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}