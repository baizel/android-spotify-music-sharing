package com.baizelmathew.spotifycontroller.web.router;

import com.baizelmathew.spotifycontroller.spotifywrapper.Player;
import com.baizelmathew.spotifycontroller.web.router.resource.AbstractResource;
import com.baizelmathew.spotifycontroller.web.router.resource.BitmapResource;
import com.baizelmathew.spotifycontroller.web.router.resource.PersistentResource;
import com.baizelmathew.spotifycontroller.web.utils.MIME;

public class Router {
    private final static String BASE_PATH = "res/raw/";

    public Router() {
    }

    public AbstractResource route(String path) {
        switch (path) {
            case "/clientcommunication.js":
                return new PersistentResource(BASE_PATH + "clientcommunication.js", true, MIME.JS);
            case "/jquery_min.js":
                return new PersistentResource(BASE_PATH + "jquery_min.js", false, MIME.JS);
            case "/materialize.js":
                return new PersistentResource(BASE_PATH + "materialize.js", false, MIME.JS);
            case "/materialize_min.css":
                return new PersistentResource(BASE_PATH + "materialize_min.css", false, MIME.CSS);
            case "/icon.css":
                return new PersistentResource(BASE_PATH + "icon.css", false, MIME.CSS);
            case "/api/current-image":
                return new BitmapResource(Player.getInstance().getCurrentTrackImage());
            default:
                return new PersistentResource(BASE_PATH + "newui.html", true, MIME.HTML);
        }
    }


}

