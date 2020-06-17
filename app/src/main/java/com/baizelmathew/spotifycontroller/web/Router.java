package com.baizelmathew.spotifycontroller.web;

import java.util.HashMap;
import java.util.Map;

public class Router {
    private Map<String, Route> routes = new HashMap<>();
    private final static String BASE_PATH = "res/raw/";

    public Router() {
        routes.put("/", new Route(BASE_PATH + "index.html", true, MIME.HTML));
        routes.put("/clientcommunication.js", new Route(BASE_PATH + "clientcommunication.js", true, MIME.JS));
        routes.put("/jquery_min.js", new Route(BASE_PATH + "jquery_min.js", false, MIME.JS));
        routes.put("/materialize.js", new Route(BASE_PATH + "materialize.js", false, MIME.JS));
        routes.put("/materialize_min.css", new Route(BASE_PATH + "materialize_min.css", false, MIME.CSS));
        routes.put("/icon.css", new Route(BASE_PATH + "icon.css", false, MIME.CSS));
    }

    public Route route(String path) {
        return routes.getOrDefault(path, new Route(BASE_PATH + "index.html", true));
    }
}

