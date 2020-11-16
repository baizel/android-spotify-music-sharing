package com.baizelmathew.spotifycontroller.web_interface_manager.router.resource;

import com.baizelmathew.spotifycontroller.webserver.utils.MIME;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class JSONResource extends AbstractResource {
    private String json;

    public JSONResource(String json) {
        super(MIME.JSON);
        this.json = json;
    }

    @Override
    public InputStream getResourceInputStream() throws Exception {
        return new ByteArrayInputStream(json.getBytes());
    }
}
