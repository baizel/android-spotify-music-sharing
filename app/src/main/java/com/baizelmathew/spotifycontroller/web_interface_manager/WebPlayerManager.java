package com.baizelmathew.spotifycontroller.web_interface_manager;

import com.baizelmathew.spotifycontroller.eventbus_messeages.enums.PlayerActions;
import com.baizelmathew.spotifycontroller.eventbus_messeages.PlayerActionEvent;
import com.baizelmathew.spotifycontroller.eventbus_messeages.PlayerStateEvent;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.ServerRouter;
import com.baizelmathew.spotifycontroller.webserver.HTTPServer;
import com.baizelmathew.spotifycontroller.websocket.WebSocket;
import com.baizelmathew.spotifycontroller.websocket.WebSocketCallback;
import com.spotify.protocol.types.Empty;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WebPlayerManager {
    private static HTTPServer httpServer;
    private static WebSocket webSocketServer;
    private final ServerRouter router;
    private final OnEventCallback<Empty> onWebSocketError;

    public WebPlayerManager(OnEventCallback<Empty> onWebSocketErrorCallback) throws IOException {
        try {
            router = new ServerRouter();
            startService();
            this.onWebSocketError = onWebSocketErrorCallback;
        } catch (IOException e) {
            stop();
            throw e;
        }
    }

    public void stop() {
        try {
            if (httpServer != null)
                httpServer.stop();
            if (webSocketServer != null)
                webSocketServer.stop(1000); //timeout 1000 milliseconds for each peer
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            httpServer = null;
            webSocketServer = null;
            router.setWsAddr(null);
            EventBus.getDefault().unregister(this);
        }
    }

    public void restartService() throws IOException {
        stop();
        startService();
    }


    public String getURL() {
        return httpServer.getHttpUrl();
    }

    private void startService() throws IOException {
        httpServer = HTTPServer.getInstance(8080, router);
        webSocketServer = WebSocket.getInstance(httpServer.getBindAddr(), 6969);
        router.setWsAddr(webSocketServer.getWsAddress());
        webSocketServer.registerCallback(getWebSocketCallBack());
        httpServer.start();
        webSocketServer.start();
        registerToEventBus();
    }

    private WebSocketCallback getWebSocketCallBack() {
        return new WebSocketCallback() {
            @Override
            public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
                broadcastState();
            }

            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
                //ignore?
            }

            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
                try {
                    handleRequestAction(conn, message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
                onWebSocketError.onFailure(ex);
            }
        };
    }

    private void registerToEventBus() {
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void subscribeToPlayerState(PlayerStateEvent event) {
        flush(event.getJsonData());
    }

    private void flush(String data) {
        if (webSocketServer != null)
            webSocketServer.broadcast(data);
    }

    private void broadcastState() {
        PlayerStateEvent state = EventBus.getDefault().getStickyEvent(PlayerStateEvent.class);
        if (state != null) {
            flush(state.getJsonData());
        }
    }

    private void handleRequestAction(org.java_websocket.WebSocket conn, String message) throws JSONException {
        JSONObject msg = new JSONObject(message);
        PlayerActions playerActions = PlayerActions.fromString(msg.getString("payload"));
        String payload;
        switch (playerActions) {
            case ADD_TO_QUEUE:
                payload = msg.getString("uri");
                break;
            case SEEK_TO:
                payload = msg.getString("position");
                break;
            default:
                payload = "";
        }
        PlayerActionEvent event = new PlayerActionEvent(playerActions, payload);
        EventBus.getDefault().post(event);
    }

}
