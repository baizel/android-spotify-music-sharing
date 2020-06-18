package com.baizelmathew.spotifycontroller.web.router.resource;

import android.graphics.Bitmap;

import com.baizelmathew.spotifycontroller.web.utils.MIME;

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
