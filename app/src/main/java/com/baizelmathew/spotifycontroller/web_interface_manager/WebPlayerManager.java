package com.baizelmathew.spotifycontroller.web_interface_manager;

import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.ServerRouter;
import com.baizelmathew.spotifycontroller.webserver.HTTPServer;
import com.baizelmathew.spotifycontroller.websocket.WebSocket;
import com.baizelmathew.spotifycontroller.websocket.WebSocketCallback;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WebPlayerManager {

    private static HTTPServer httpServer;
    private static WebSocket webSocketServer;
    private ServerRouter router;

    public WebPlayerManager() throws IOException {
        router = new ServerRouter();
        httpServer = HTTPServer.getInstance(8080, router);
        webSocketServer = WebSocket.getInstance(httpServer.getBindAddr(), 6969);
        router.setWsAddr(webSocketServer.getWsAddress());
        webSocketServer.registerCallback(getWebSocketCallBack());
        httpServer.start();
        webSocketServer.start();
        subscribeToSpotifyStateChange();
    }

    public void stop() {
        try {
            httpServer.stop();
            webSocketServer.stop(1000); //timeout 1000 milliseconds for each peer
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            httpServer = null;
            webSocketServer = null;
            router.setWsAddr(null);
        }
    }

    public String getURL() {
        return httpServer.getHttpUrl();
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
                //ignore?
            }
        };
    }

    private void subscribeToSpotifyStateChange() {
        Player.getInstance().getSubscriptionPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
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
        Player m = Player.getInstance();
        m.getCurrentPlayerState(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                flush(getJsonFormatOfPlayerState(playerState));
            }
        });
    }


    private String getJsonFormatOfPlayerState(PlayerState playerState) {
        JsonElement json = new Gson().toJsonTree(playerState);
        json.getAsJsonObject().addProperty("queue", new Gson().toJson(Player.getInstance().getCustomQueue().getQueue()));
        json.getAsJsonObject().addProperty("queueCurrentPos", new Gson().toJson(Player.getInstance().getCustomQueue().getCurrentPosition()));
        return json.toString();
    }

    private void handleRequestAction(org.java_websocket.WebSocket conn, String message) throws JSONException {
        final Player player = Player.getInstance();
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
        }
    }

}
