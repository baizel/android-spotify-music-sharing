package com.baizelmathew.spotifycontroller.web_interface_manager.router.resource;

import com.baizelmathew.spotifycontroller.webserver.utils.MIME;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class PersistentResource extends AbstractResource {
    String path;

    public PersistentResource(String path, MIME mime) {
        super(mime);
        this.path = path;
    }

    public InputStream getResourceInputStream() throws FileNotFoundException {
        InputStream inputStream = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream(path);
        if (inputStream != null) {
            return inputStream;
        }
        throw new FileNotFoundException("File could not be converted to a stream " + path);
    }
}
