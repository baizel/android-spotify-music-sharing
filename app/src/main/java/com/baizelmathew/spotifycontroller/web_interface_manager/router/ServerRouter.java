package com.baizelmathew.spotifycontroller.web_interface_manager.router;

import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.resource.AbstractResource;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.resource.BitmapResource;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.resource.JSONResource;
import com.baizelmathew.spotifycontroller.web_interface_manager.router.resource.PersistentResource;
import com.baizelmathew.spotifycontroller.webserver.utils.FallbackErrorPage;
import com.baizelmathew.spotifycontroller.webserver.utils.MIME;
import com.baizelmathew.spotifycontroller.webserver.utils.Router;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.newChunkedResponse;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ServerRouter implements Router {
    private final static String BASE_PATH = "res/raw/";
    private String wsAddr;

    public ServerRouter() {
    }

    private AbstractResource route(String path) throws Throwable {
        //TODO: extract string to a different class with paths
        switch (path) {
            case "/":
                return new PersistentResource(BASE_PATH + "newui.html", MIME.HTML);
            case "/clientcommunication.js":
                return new PersistentResource(BASE_PATH + "clientcommunication.js", MIME.JS);
            case "/jquery_min.js":
                return new PersistentResource(BASE_PATH + "jquery_min.js", MIME.JS);
            case "/materialize.js":
                return new PersistentResource(BASE_PATH + "materialize.js", MIME.JS);
            case "/materialize_min.css":
                return new PersistentResource(BASE_PATH + "materialize_min.css", MIME.CSS);
            case "/icon.css":
                return new PersistentResource(BASE_PATH + "icon.css", MIME.CSS);
            case "/api/spotify/cached-album-cover":
                return new BitmapResource(Player.getInstance().getCurrentImageOfTrackBlocking(5, TimeUnit.SECONDS));
            case "/api/spotify/state":
                return new JSONResource(Player.getInstance().getPlayerStateBlocking(5, TimeUnit.SECONDS));
            case "/api/web/addr":
                if (wsAddr != null) {
                    return new JSONResource("{\"result\": \"" + wsAddr + "\"}");
                }
                throw new NoSuchFieldException("No websocket address found");
            default:
                throw new NoSuchFieldException("No route for path");
        }
    }

    public NanoHTTPD.Response serve(String uri) {
        String errorMsg = "Resource not handled";
        try {
            AbstractResource resource = route(uri);
            if (resource instanceof PersistentResource) {
                return handlePersistentResource((PersistentResource) resource);
            } else if (resource instanceof BitmapResource) {
                return handleBitmapResource(resource);
            } else if (resource instanceof JSONResource) {
                return newChunkedResponse(NanoHTTPD.Response.Status.OK, resource.getMime().getValue(), resource.getResourceInputStream());
            }
        } catch (Throwable e) {
            errorMsg = e.toString();
        }
        return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, MIME.JSON.getValue(), "{\"error\":\"" + errorMsg + "\"}");
    }

    private NanoHTTPD.Response handleBitmapResource(AbstractResource resource) {
        try {
            return newChunkedResponse(NanoHTTPD.Response.Status.OK, resource.getMime().getValue(), resource.getResourceInputStream());
        } catch (Exception e) {
            return getErrorResponse();
        }
    }

    private NanoHTTPD.Response handlePersistentResource(PersistentResource resource) {
        try {
            return newChunkedResponse(NanoHTTPD.Response.Status.OK, resource.getMime().getValue(), resource.getResourceInputStream());
        } catch (IOException | NullPointerException e) {
            return getErrorResponse();
        }
    }

    private NanoHTTPD.Response getErrorResponse() {
        return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME.HTML.getValue(), FallbackErrorPage.getErrorPage());
    }

    public void setWsAddr(String wsAddress) {
        this.wsAddr = wsAddress;
    }

}

