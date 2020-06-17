package com.baizelmathew.spotifycontroller.web;

public class Route {
    String path;
    MIME mime;

    public MIME getMime() {
        return mime;
    }

    boolean isDataInjectionNeeded;

    public Route(String path, Boolean isDataInjectionNeeded, MIME mime) {
        this.path = path;
        this.isDataInjectionNeeded = isDataInjectionNeeded;
        this.mime = mime;
    }

    public Route(String path, Boolean isDataInjectionNeeded) {
        this(path, isDataInjectionNeeded, MIME.TXT);
    }

    public String getPath() {
        return path;
    }

    public boolean isDataInjectionNeeded() {
        return isDataInjectionNeeded;
    }
}
