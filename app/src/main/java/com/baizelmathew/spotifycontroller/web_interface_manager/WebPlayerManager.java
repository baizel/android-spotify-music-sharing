package com.baizelmathew.spotifycontroller.web_interface_manager;

import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.ServerRouter;
import com.baizelmathew.spotifycontroller.webserver.HTTPServer;
import com.baizelmathew.spotifycontroller.websocket.WebSocket;
import com.baizelmathew.spotifycontroller.websocket.WebSocketCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.mappers.JsonMappingException;
import com.spotify.protocol.mappers.jackson.JacksonMapper;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.PlayerState;

import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WebPlayerManager {
    private static HTTPServer httpServer;
    private static WebSocket webSocketServer;
    private ServerRouter router;
    private OnEventCallback<Empty> onWebSocketError;
    private Player player;

    public WebPlayerManager(OnEventCallback<Empty> onWebSocketErrorCallback) throws IOException {
        try {
            router = new ServerRouter();
            player = Player.getInitializedInstance();
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
        subscribeToSpotifyStateChange();
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

    private void subscribeToSpotifyStateChange() {
        player.getSubscriptionPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                flush(getJsonFormatOfPlayerState(playerState));
            }
        });
    }

    private void flush(String data) {
        webSocketServer.broadcast(data);
    }

    private void broadcastState() {
        player.getCurrentPlayerState(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                flush(getJsonFormatOfPlayerState(playerState));
            }
        });
    }


    private String getJsonFormatOfPlayerState(PlayerState playerState) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.convertValue(playerState, ObjectNode.class);
        node.set("queue", mapper.convertValue(player.getCustomQueue().getQueue(), JsonNode.class));
        node.set("queueCurrentPos", mapper.convertValue(player.getCustomQueue().getCurrentPosition(), JsonNode.class));
        return node.toString();
    }

    private void handleRequestAction(org.java_websocket.WebSocket conn, String message) throws JSONException {
        JSONObject msg = new JSONObject(message);
        switch (msg.getString("payload")) {
            case "plsrespond":
                conn.send("is this working");
            case "next":
                player.nextTrack();
                break;
            case "previous":
                player.previousTrack();
                break;
            case "play":
                player.getCurrentPlayerState(new CallResult.ResultCallback<PlayerState>() {
                    @Override
                    public void onResult(PlayerState playerState) {
                        if (playerState.isPaused)
                            player.resume();
                        else
                            player.pause();
                    }
                });
                break;
            case "playUri":
                String uri = msg.getString("uri");
                player.addToQueue(uri);
                break;
            case "seek":
                long pos = msg.getLong("position");
                player.seekTo(pos);
        }
    }

}
