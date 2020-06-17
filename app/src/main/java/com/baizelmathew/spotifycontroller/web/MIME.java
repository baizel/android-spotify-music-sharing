package com.baizelmathew.spotifycontroller.web;

public enum MIME {
    TXT("text/plain"),
    JS("text/javascript"),
    HTML("text/html"),
    CSS("text/css") ;
    private String value;

    MIME(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
