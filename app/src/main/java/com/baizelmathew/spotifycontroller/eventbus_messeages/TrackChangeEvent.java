package com.baizelmathew.spotifycontroller.eventbus_messeages;

import android.graphics.Bitmap;

import com.baizelmathew.spotifycontroller.spotify_wrapper.Player;
import com.spotify.protocol.types.Track;

import java.util.concurrent.TimeUnit;

public class TrackChangeEvent {
    Track currentTrack;

    public TrackChangeEvent(Track currentTrack) {
        this.currentTrack = currentTrack;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

}
