/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.web;

/**
 * Helper class for callbacks
 */
public interface WebSocketCallback {
    void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote);

    void onMessage(org.java_websocket.WebSocket conn, String message);

    void onError(org.java_websocket.WebSocket conn, Exception ex);
}
