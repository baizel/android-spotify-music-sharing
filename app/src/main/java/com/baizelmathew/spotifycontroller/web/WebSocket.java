package com.baizelmathew.spotifycontroller.web;

import android.util.Log;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.google.gson.Gson;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

public class WebSocket extends WebSocketServer {
    private static String TAG = "MySocket";

    private String wsAddress;
    private WebSocketCallback callback;

    public WebSocket(String host, int port) {
        super(new InetSocketAddress(host, port));
        wsAddress = "ws://" + host + ":" + port;
        Log.d(TAG, "Constructed");
    }

    public String getWsAddress() {
        return wsAddress;
    }

    public WebSocket registerCallback(WebSocketCallback callback) {
        this.callback = callback;
        return this;
    }


    @Override
    public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "new connection to " + conn.getRemoteSocketAddress());
        brodcastState();
    }

    @Override
    public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "Socket Closed " + conn.getRemoteSocketAddress() + " Reason: " + reason);
        callback.onClose(conn, code, reason, remote);
    }

    @Override
    public void onMessage(org.java_websocket.WebSocket conn, String message) {
        Log.d(TAG, "Got message " + message);
        brodcastState();
        callback.onMessage(conn, message);

    }

    @Override
    public void onError(org.java_websocket.WebSocket conn, Exception ex) {
        Log.d(TAG, "Error" + ex.getLocalizedMessage());
        callback.onError(conn, ex);
    }

    @Override
    public void onStart() {
        brodcastState();
        Log.d(TAG, "Start web socket server");

    }

    private void brodcastState() {
        Player m = Player.getInstance();
        m.getPlayerState(new OnEventCallback() {
            @Override
            public void onEvent(PlayerState playerState) {
                String state = new Gson().toJson(playerState);
                broadcast(state);
                Log.d(TAG, state);
            }
        });
    }

}
