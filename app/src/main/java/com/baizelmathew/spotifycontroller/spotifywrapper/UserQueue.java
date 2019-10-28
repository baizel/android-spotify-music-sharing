package com.baizelmathew.spotifycontroller.spotifywrapper;

import com.spotify.protocol.types.Track;

import java.util.LinkedList;

public class UserQueue {
    private LinkedList<Track> queue = new LinkedList<>();
    private int currentPosition = 0;

    public UserQueue() {

    }

    public Track updatePosAndGetNextTrack(){
        currentPosition++;
        if (queue.size() > currentPosition){
            return queue.get(currentPosition);
        } else {
            currentPosition = queue.size() -1;
        }
        return null;

    }

    public Track updatePosAndGePreviousTrack(){
        currentPosition--;
        if (currentPosition > 0){
            return queue.get(currentPosition);
        } else {
            currentPosition = 0;
        }
        return null;

    }

    public void addToQueue(Track t){
        queue.add(t);
    }

    public void remove(Track t){
        queue.remove(t);
    }
}
