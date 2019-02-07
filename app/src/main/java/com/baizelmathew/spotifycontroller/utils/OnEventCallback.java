package com.baizelmathew.spotifycontroller.utils;

import com.spotify.protocol.types.PlayerState;

public interface OnEventCallback {
    void onEvent(PlayerState playerState);
}
