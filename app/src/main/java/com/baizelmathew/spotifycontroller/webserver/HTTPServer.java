package com.baizelmathew.spotifycontroller.webserver;

import com.baizelmathew.spotifycontroller.web_interface_manager.router.Router;

import java.net.NoRouteToHostException;

import fi.iki.elonen.NanoHTTPD;

import static com.baizelmathew.spotifycontroller.webserver.utils.IpAddressHelper.getIPAddress;

public class HTTPServer extends NanoHTTPD {

    private static HTTPServer instance;

    private Router router;
    private static final boolean IS_IPV4 = true;
    private String addr;
    private int port;

    private HTTPServer(int port, Router router) throws NoRouteToHostException {
        super(getIPAddress(IS_IPV4), port);
        this.router = router;
        this.addr = getIPAddress(IS_IPV4);
        this.port = port;
    }

    public static synchronized HTTPServer getInstance(int port, Router router) throws NoRouteToHostException {
        if (instance == null) {
            instance = new HTTPServer(port, router);
        }
        return instance;
    }

    public String getHttpUrl() {
        return "http://" + addr + ":" + port;
    }

    public String getBindAddr() {
        return addr;
    }

    @Override
    public Response serve(IHTTPSession session) {
        return router.serve(session.getUri());
    }
}
