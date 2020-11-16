package com.baizelmathew.spotifycontroller.web_interface_manager.router;

import fi.iki.elonen.NanoHTTPD;

public interface Router {
    NanoHTTPD.Response serve(String uri);
}
