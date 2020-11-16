package com.baizelmathew.spotifycontroller.spotify_wrapper;

import com.spotify.protocol.types.PlayerState;

import java.util.LinkedList;
import java.util.List;

public class UserQueue {
    private LinkedList<String> queue = new LinkedList<>();
    private int currentPosition = 0;
    private final static int Q_DISPLAY_BUFFER = 1;

    public UserQueue() {

    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void onPlayerState(PlayerState ps) {
        String currentTrack = ps.track.uri;
        if (queue.indexOf(currentTrack) >= 0) {
            remove(currentTrack);
        }
    }

    public void addToQueue(String uri) {
        queue.add(uri);
    }

    public void remove(String uri) {
        queue.remove(uri);
    }

    public List<String> getQueue() {
        return queue;
    }
}
