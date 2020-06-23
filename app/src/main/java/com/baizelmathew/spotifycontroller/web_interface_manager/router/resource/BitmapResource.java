package com.baizelmathew.spotifycontroller.web_interface_manager.router.resource;

import android.graphics.Bitmap;

import com.baizelmathew.spotifycontroller.webserver.utils.MIME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class BitmapResource extends AbstractResource {
    private Bitmap img;

    public BitmapResource(Bitmap img) {
        super(MIME.PNG);
        this.img = img;
    }

    @Override
    public InputStream getResourceInputStream() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (img != null) {
            img.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            return new ByteArrayInputStream(bos.toByteArray());
        }
        throw new FileNotFoundException("Bitmap is not initialized");
    }
}
