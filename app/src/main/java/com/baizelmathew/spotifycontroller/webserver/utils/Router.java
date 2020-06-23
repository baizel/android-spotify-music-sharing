package com.baizelmathew.spotifycontroller.webserver.utils;

import fi.iki.elonen.NanoHTTPD;

public interface Router {
    NanoHTTPD.Response serve(String uri);
}
