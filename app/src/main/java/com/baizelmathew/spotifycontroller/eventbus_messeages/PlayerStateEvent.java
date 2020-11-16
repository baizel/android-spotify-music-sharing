package com.baizelmathew.spotifycontroller.eventbus_messeages;

import com.baizelmathew.spotifycontroller.spotify_wrapper.UserQueue;
import com.baizelmathew.spotifycontroller.utils.Util;
import com.spotify.protocol.types.PlayerState;

public class PlayerStateEvent {
    String jsonData;

    public PlayerStateEvent(PlayerState playerState, UserQueue queue) {
        this.jsonData = Util.getJsonFormatOfPlayerState(playerState, queue);
    }

    public String getJsonData() {
        return jsonData;
    }
}
