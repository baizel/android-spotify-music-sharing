/*
  @Author: Baizel Mathew
 */
package com.baizelmathew.spotifycontroller.web;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.utils.DataInjector;
import com.baizelmathew.spotifycontroller.utils.FallbackErrorPage;
import com.baizelmathew.spotifycontroller.utils.OnEventCallback;
import com.spotify.protocol.types.PlayerState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * Singleton class that hosts the web server
 */
public class WebServer extends NanoHTTPD {

    private static final int HTTP_PORT = 8080;
    private static final int SOCKET_PORT = 6969;
    private static final String PLAY_ARROW = "play_arrow"; // Icons name in HTML
    private static final String PAUSE = "PAUSE";// Icons name in HTML

    private static boolean isIpv4 = true;
    private static String httpAddress;
    private static WebServer server = null;

    private static boolean isIncomingPaused = false;
    private WebSocket webSocket;

    public static final String ACTION_PAUSE_ALL_INCOMING_REQUEST = "pause_all_incoming_requests";
    public static final String EXTRA_PAUSE_INCOMING_REQUEST_STATE = "EXTRA_PAUSE_INCOMING_REQUEST_STATE";


    public static WebServer getInstance() {
        if (server == null) {
            return server = new WebServer();
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
    private WebServer() {
        super(getIPAddress(isIpv4), HTTP_PORT);
        String ipAddress = getIPAddress(isIpv4);

        httpAddress = "http://" + ipAddress + ":" + HTTP_PORT;
        //Calls back to handle incoming requests
        webSocket = new WebSocket(ipAddress, SOCKET_PORT).registerCallback(new WebSocketCallback() {
            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {

            }
            //The payloads are defined in the HTML page
            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
                if (!isIncomingPaused) {
                    try {
                        final Player player = Player.getInstance();
                        JSONObject msg = new JSONObject(message);
                        switch (msg.getString("payload")) {
                            case "next":
                                player.getSpotifyAppRemote().getPlayerApi().skipNext();
                                break;
                            case "previous":
                                player.getSpotifyAppRemote().getPlayerApi().skipPrevious();
                                break;
                            case "play":
                                player.getPlayerState(new OnEventCallback() {
                                    @Override
                                    public void onEvent(PlayerState playerState) {
                                        if (playerState.isPaused)
                                            player.getSpotifyAppRemote().getPlayerApi().resume();
                                        else
                                            player.getSpotifyAppRemote().getPlayerApi().pause();
                                    }
                                });
                            case "playUri":
                                String uri = msg.getString("uri");
                                player.getSpotifyAppRemote().getPlayerApi().queue(uri);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
                //ignore it nothing needed
            }
        });
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
    private static String getIPAddress(boolean useIPv4) {
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
        return null;
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
        //Dta to be injected into the page
        HashMap<String, String> data = new HashMap<>();
        data.put("token", Player.getAccessToken());
        data.put("PlayIcon", PLAY_ARROW);
        data.put("SongName", "Loading..");
        data.put("SongDescription", "Loading..");
        data.put("socket", webSocket.getWsAddress());
        data.put("InitialState", Player.getInstance().getInitialPlayerState());

        String page;
        try {
            page = new DataInjector().injectData("index.html", data);
        } catch (IOException e) {
            e.printStackTrace();
            page = FallbackErrorPage.getErrorPage();

        } catch (NullPointerException n) {
            n.printStackTrace();
            page = FallbackErrorPage.getErrorPage();
        }
        return newFixedLengthResponse(page);
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

}
