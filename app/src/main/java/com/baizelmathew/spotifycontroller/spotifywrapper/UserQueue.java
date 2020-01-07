package com.baizelmathew.spotifycontroller.spotifywrapper;

import com.spotify.protocol.types.Track;

import java.util.LinkedList;
import java.util.List;

public class UserQueue {
    private LinkedList<String> queue = new LinkedList<>();
    private int currentPosition = 0;

    public UserQueue() {

    }

    public String updatePosAndGetNextTrackUri(){
        currentPosition++;
        if (queue.size() > currentPosition){
            return queue.get(currentPosition);
        } else {
            currentPosition = queue.size() -1;
        }
        return null;

    }

    public String updatePosAndGePreviousTrackUri(){
        currentPosition--;
        if (currentPosition > 0){
            return queue.get(currentPosition);
        } else {
            currentPosition = 0;
        }
        return null;

    }

    public void addToQueue(String uri){
        queue.add(uri);
    }

    public void remove(String uri){
        queue.remove(uri);
    }

    public List<String> getqueue() {
        return queue;
    }
}
