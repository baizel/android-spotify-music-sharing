package com.baizelmathew.spotifycontroller.web;

import android.util.Log;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.google.gson.Gson;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Package protected Web socket class.
 * Can only be accessed by the web server.
 * it can connect, send and receive messages from different clients.
 * Can also register callbacks so any message received can be passed forward
 */
class WebSocket extends WebSocketServer {
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

    /**
     * broadcasts the state of the of the spotify player when update occurs
     */
    public void startListiningToState(){
        Player player = Player.getInstance();
        player.getSpotifyAppRemote().getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                String state = new Gson().toJson(playerState);
                broadcast(state);
                Log.d(TAG, "broadcast state " + state);
            }
        });
    }
    @Override
    public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "new connection to " + conn.getRemoteSocketAddress());
        broadcastState();
    }

    @Override
    public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "Socket Closed " + conn.getRemoteSocketAddress() + " Reason: " + reason);
        callback.onClose(conn, code, reason, remote);
    }

    @Override
    public void onMessage(org.java_websocket.WebSocket conn, String message) {
        Log.d(TAG, "Got message " + message);
        callback.onMessage(conn, message);

    }

    @Override
    public void onError(org.java_websocket.WebSocket conn, Exception ex) {
        Log.d(TAG, "Error" + ex.getLocalizedMessage());
        callback.onError(conn, ex);
    }

    @Override
    public void onStart() {
        broadcastState();
        Log.d(TAG, "Start web socket server");

    }

    /**
     * Broadcasts the state to anything listing
     */
    private void broadcastState() {
        Player m = Player.getInstance();
        m.getPlayerState(new OnEventCallback() {
            @Override
            public void onEvent(PlayerState playerState) {
                String state = new Gson().toJson(playerState);
                broadcast(state);
                Log.d(TAG, "broadcast state " + state);
            }
        });
    }

}
