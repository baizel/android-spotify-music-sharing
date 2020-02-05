package com.baizelmathew.spotifycontroller.utils;

public interface OnFailSocketCallBack {
    void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote);
    void onError(org.java_websocket.WebSocket conn, Exception ex);
}
