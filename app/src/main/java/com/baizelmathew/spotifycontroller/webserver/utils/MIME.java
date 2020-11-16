package com.baizelmathew.spotifycontroller.webserver.utils;

public enum MIME {
    TXT("text/plain"),
    JS("text/javascript"),
    HTML("text/html"),
    CSS("text/css"),
    PNG("image/png"),
    JSON("application/json");

    private String value;

    MIME(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
