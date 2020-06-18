/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.web;

import android.graphics.Bitmap;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.web.router.resource.BitmapResource;
import com.baizelmathew.spotifycontroller.web.utils.DataInjector;
import com.baizelmathew.spotifycontroller.web.utils.FallbackErrorPage;
import com.baizelmathew.spotifycontroller.web.utils.MIME;
import com.baizelmathew.spotifycontroller.web.utils.OnEventCallback;
import com.baizelmathew.spotifycontroller.web.utils.OnFailSocketCallBack;
import com.baizelmathew.spotifycontroller.web.router.resource.PersistentResource;
import com.baizelmathew.spotifycontroller.web.router.Router;
import com.baizelmathew.spotifycontroller.web.router.resource.AbstractResource;
import com.spotify.protocol.types.PlayerState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * Singleton class that hosts the web server
 */
public class WebServer extends NanoHTTPD {

    private static final int HTTP_PORT = 8081;
    private static final int SOCKET_PORT = 6968;
    private static final String PLAY_ARROW = "play_arrow"; // Icons name in HTML
    private static final String PAUSE = "PAUSE";// Icons name in HTML
    private static boolean isIpv4 = true;
    private static String httpAddress;
    private static WebServer server = null;

    private static boolean isIncomingPaused = false;
    private WebSocket webSocket;
    private HashMap<String, String> injectionData;
    public static final String ACTION_PAUSE_ALL_INCOMING_REQUEST = "pause_all_incoming_requests";
    public static final String EXTRA_PAUSE_INCOMING_REQUEST_STATE = "EXTRA_PAUSE_INCOMING_REQUEST_STATE";
    private static final Router ROUTER = new Router();

    public static WebServer getInstance(OnFailSocketCallBack callBack) throws NoRouteToHostException {
        if (server == null) {
            return server = new WebServer(callBack);
        }
        return server;
    }

    /**
     * Starts listening to any changes broadcasted by the Spotify SDK
     */
    public void startListening() {
        webSocket.startListiningToState();
    }

    /**
     * Creates the web server instance and stores in the static variable server.
     * This also handles a ny incoming web socket requests
     */
    private WebServer(final OnFailSocketCallBack onFailSocketCallBack) throws NoRouteToHostException {
        super(getIPAddress(isIpv4), HTTP_PORT);
        String ipAddress = getIPAddress(isIpv4);

        httpAddress = "http://" + ipAddress + ":" + HTTP_PORT;
        //Calls back to handle incoming requests
        initWebSocketCallbacks(onFailSocketCallBack);
    }

    public static boolean getIncomingPaused() {
        return isIncomingPaused;
    }

    public static void setIncomingPaused(boolean bool) {
        isIncomingPaused = bool;
    }

    public void startServer() throws IOException {
        webSocket.start();
        this.start();

    }

    /**
     * Get ip addres of the current device
     * Code from StackOverflow
     * https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     *
     * @param useIPv4
     * @return
     */
    private static String getIPAddress(boolean useIPv4) throws NoRouteToHostException {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        throw new NoRouteToHostException("No IP Address found");
    }

    public String getHttpAddress() {
        return httpAddress;
    }

    /**
     * The response to any request for a webpage
     * If the Data injector fails here then a fall back page will b returned
     *
     * @param session
     * @return
     */
    @Override
    public Response serve(IHTTPSession session) {
        AbstractResource resource = ROUTER.route(session.getUri());
        if (resource instanceof PersistentResource) {
            return handlePersistentResource((PersistentResource) resource);
        } else if (resource instanceof BitmapResource) {
            return handleBitmapResource(resource);
        }
        return null; // TODO: handle default
    }

    private Response handleBitmapResource(AbstractResource resource) {
        try {
            return newChunkedResponse(Response.Status.OK, resource.getMime().getValue(), resource.getResourceInputStream());
        } catch (Exception e) {
            return getErrorResponse();
        }
    }

    private Response handlePersistentResource(PersistentResource resource) {
        try {
            if (resource.isDataInjectionNeeded()) {
                String injectedData = DataInjector.readFileAndInjectData(resource.getResourceInputStream(), refreshInjectionData());
                return newFixedLengthResponse(Response.Status.OK, resource.getMime().getValue(), injectedData);
            }
            return newChunkedResponse(Response.Status.OK, resource.getMime().getValue(), resource.getResourceInputStream());
        } catch (IOException | NullPointerException e) {
            return getErrorResponse();
        }
    }

    private Response getErrorResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME.HTML.getValue(), FallbackErrorPage.getErrorPage());
    }

    /**
     * Kill all servers and reset instance variables
     */
    @Override
    public void stop() {
        super.stop();
        try {
            if (webSocket != null) {
                webSocket.stop(3000);
            }
            webSocket = null;
            server = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initWebSocketCallbacks(final OnFailSocketCallBack onFailSocketCallBack) throws NoRouteToHostException {
        webSocket = new WebSocket(getIPAddress(isIpv4), SOCKET_PORT).registerCallback(new WebSocketCallback() {
            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
                onFailSocketCallBack.onClose(conn, code, reason, remote);
            }

            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
                handleIncomingRequest(conn, message);
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
                handleOnError(onFailSocketCallBack, conn, ex);
            }
        });
    }

    private HashMap<String, String> refreshInjectionData() {
        Player playerInstance = Player.getInstance();
        String songName = "Loading..";
        String sonDesc = "Loading..";
        injectionData = new HashMap<>();
        injectionData.put("token", Player.getAccessToken());
        injectionData.put("PlayIcon", PLAY_ARROW);
        injectionData.put("SongName", songName);
        injectionData.put("SongDescription", sonDesc);
        injectionData.put("socket", webSocket.getWsAddress());
        injectionData.put("InitialState", playerInstance.getInitialPlayerState());
        return injectionData;
    }

    private void handleIncomingRequest(org.java_websocket.WebSocket conn, String message) {
        if (!isIncomingPaused) {
            try {
                handleRequestAction(conn, message);
            } catch (JSONException ignoredRequest) {
                ignoredRequest.printStackTrace();
            }
        }
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
                player.getPlayerState(new OnEventCallback() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        if (playerState.isPaused)
                            player.resume();
                        else
                            player.pause();
                    }
                });
            case "playUri":
                String uri = msg.getString("uri");
                player.addToQueue(uri);
        }
    }

    private void handleOnError(OnFailSocketCallBack onFailSocketCallBack, org.java_websocket.WebSocket conn, Exception ex) {
        onFailSocketCallBack.onError(conn, ex);
        if (conn != null)
            conn.close();
        httpAddress = "Error on WebSocket: " + ex.getLocalizedMessage();
    }
}
