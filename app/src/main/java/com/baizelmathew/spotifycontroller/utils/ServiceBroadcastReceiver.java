package com.baizelmathew.spotifycontroller.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.baizelmathew.spotifycontroller.service.ForeGroundServerService;
import com.baizelmathew.spotifycontroller.web.WebServer;

import java.util.Objects;

/**
 * Helper class to stop and start services
 */
public class ServiceBroadcastReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (Objects.requireNonNull(intent.getAction())) {
            case ForeGroundServerService.ACTION_STOP_FOREGROUND_SERVICE:
                killForeGroundService(context);
                break;
            case ForeGroundServerService.ACTION_START_FOREGROUND_SERVICE:
                startForeGroundService(context);
            case WebServer.ACTION_PAUSE_ALL_INCOMING_REQUEST:
                WebServer.setIncomingPaused(intent.getBooleanExtra(WebServer.EXTRA_PAUSE_INCOMING_REQUEST_STATE,false));
            default:
                break;
        }
    }

    private void startForeGroundService(Context context) {
        context.startService(new Intent(context, ForeGroundServerService.class));
    }

    private void killForeGroundService(Context context) {
        context.stopService(new Intent(context, ForeGroundServerService.class));
        Toast.makeText(context, "Killing Service", Toast.LENGTH_LONG).show();
    }
}
