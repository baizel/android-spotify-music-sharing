package com.baizelmathew.spotifycontroller.web.router.resource;

import com.baizelmathew.spotifycontroller.web.utils.MIME;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class PersistentResource extends AbstractResource {
    String path;
    private boolean isDataInjectionNeeded;

    public PersistentResource(String path, Boolean isDataInjectionNeeded, MIME mime) {
        super(mime);
        this.path = path;
        this.isDataInjectionNeeded = isDataInjectionNeeded;
    }

    public PersistentResource(String path, Boolean isDataInjectionNeeded) {
        this(path, isDataInjectionNeeded, MIME.TXT);
    }

    public InputStream getResourceInputStream() throws FileNotFoundException {
        InputStream inputStream = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream(path);
        if (inputStream != null) {
            return inputStream;
        }
        throw new FileNotFoundException("File could not be converted to a stream " + path);
    }

    public boolean isDataInjectionNeeded() {
        return isDataInjectionNeeded;
    }
}
