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
import com.baizelmathew.spotifycontroller.eventbus_messeages.ServerAddressEvent;
import com.baizelmathew.spotifycontroller.eventbus_messeages.enums.ServerState;
import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web_interface_manager.WebPlayerManager;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.PlayerState;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

/**
 * This class is responsible for managing the web server in the background.
 */
public class ForeGroundServerService extends Service {

    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public static final String ACTION_SERVICE_STOPPED = ForeGroundServerService.class.getName() + "Stopped";

    private static final String NOTIFICATION_CHANNEL_ID = "dev.baizel.spot";
    private static final int ONGOING_NOTIFICATION_ID = 1234;
    private static final String ACTION_STOP_FOREGROUND_SERVICE_ID = "ACTION_STOP_FOREGROUND_SERVICE_ID";

    private WebPlayerManager playerManager;
    private ServiceBroadcastReceiver serviceBroadcastReceiver = new ServiceBroadcastReceiver();

    public ForeGroundServerService() {
        //https://developer.android.com/guide/components/services
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
        initSpotifyPlayerConnection();
        startForegroundService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playerManager != null) {
            playerManager.stop();
            playerManager = null;
        }
        Player.close();
        this.unregisterReceiver(serviceBroadcastReceiver);
        debugToast("Stopping Web Server");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    private void stopService() {
        Intent intent = new Intent(ACTION_SERVICE_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        Intent snoozeIntent = new Intent(this, ServiceBroadcastReceiver.class);
        snoozeIntent.setAction(ACTION_STOP_FOREGROUND_SERVICE);
        snoozeIntent.putExtra(ACTION_STOP_FOREGROUND_SERVICE_ID, 0);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);
        return new NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_tap_and_play_black_24dp)
                .setContentTitle("Server running in background")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(R.drawable.ic_stop_black_24dp, "Stop Server", stopPendingIntent)
                .build();
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(ONGOING_NOTIFICATION_ID, buildNotification());
    }

    private void startWebService() {
        try {
            playerManager = new WebPlayerManager(new OnEventCallback<Empty>() {
                @Override
                public void onResult(Empty result) {
                    //ignore
                }

                @Override
                public void onFailure(Throwable ex) {
                    postSeverEvent(ServerState.STOPPED, "Error with server " + ex.getLocalizedMessage());
                    stopService();
                }
            });
            postSeverEvent(ServerState.STARTED, playerManager.getURL());
        } catch (IOException e) {
            postSeverEvent(ServerState.STOPPED, "Error with server " + e.getLocalizedMessage());
            stopService();
        }
    }

    private void initSpotifyPlayerConnection() {
        Player.start(this.getApplicationContext(), new OnEventCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState result) {
                startWebService();
            }

            @Override
            public void onFailure(Throwable throwable) {
                debugToast("Service Not Started " + throwable.getLocalizedMessage());
                postSeverEvent(ServerState.STOPPED, "Error with server " + throwable.getLocalizedMessage());
                stopService();
            }
        });
    }

    private void debugToast(String s) {
        Toast toast = Toast.makeText(this.getApplicationContext(), s, Toast.LENGTH_LONG);
        toast.show();
    }

    private void startMyOwnForeground() {
        String channelName = "Web Server";

        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;

        manager.createNotificationChannel(chan);
        Notification notification = buildNotification();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void postSeverEvent(ServerState state, String data) {
        EventBus.getDefault().postSticky(new ServerAddressEvent(state, data));

    }
}