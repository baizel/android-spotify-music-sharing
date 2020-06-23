/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.websocket;

import android.util.Log;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Package protected Web socket class.
 * Can only be accessed by the web server.
 * it can connect, send and receive messages from different clients.
 * Can also register callbacks so any message received can be passed forward
 */
public class WebSocket extends WebSocketServer {
    enum Status {
        OPEN,
        CLOSED,
        ERROR,
        UNKNOWN
    }

    private static String TAG = "MySocket";
    private static WebSocket instance;
    private String wsAddress;
    private WebSocketCallback callback;
    private HashMap<InetSocketAddress, Status> allConnectionStatus;

    private WebSocket(String host, int port) {
        super(new InetSocketAddress(host, port));
        allConnectionStatus = new HashMap<>();
        wsAddress = "ws://" + host + ":" + port;
        Log.d(TAG, "Constructed");
    }

    public static synchronized WebSocket getInstance(String host, int port) {
        if (instance == null) {
            instance = new WebSocket(host, port);
        }
        return instance;
    }


    public String getWsAddress() {
        return wsAddress;
    }

    public WebSocket registerCallback(WebSocketCallback callback) {
        this.callback = callback;
        return this;
    }

//    /**
//     * broadcasts the state of the of the spotify player when update occurs
//     */
//    public void startBroadcastingSpotifyEvents() {
//        Player.getInstance().getSubscriptionPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
//            @Override
//            public void onEvent(PlayerState playerState) {
//                initiateBroadCastOfState(playerState);
//            }
//        });
//    }

    @Override
    public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, "new connection to " + conn.getRemoteSocketAddress());
        allConnectionStatus.put(conn.getRemoteSocketAddress(), Status.OPEN);
        callback.onOpen(conn,handshake);
//        broadcastState();
    }

    @Override
    public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "Socket Closed " + conn.getRemoteSocketAddress() + " Reason: " + reason);
        allConnectionStatus.put(conn.getRemoteSocketAddress(), Status.CLOSED);
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
        allConnectionStatus.put(conn.getRemoteSocketAddress(), Status.ERROR);
        callback.onError(conn, ex);
    }

    @Override
    public void onStart() {
//        broadcastState();
        Log.d(TAG, "Start web socket server");
    }

    public HashMap<InetSocketAddress, Status> getAllConnectionStatus() {
        return allConnectionStatus;
    }

    public Status getConnectionStatus(InetSocketAddress address) {
        return allConnectionStatus.getOrDefault(address, Status.UNKNOWN);
    }

}
