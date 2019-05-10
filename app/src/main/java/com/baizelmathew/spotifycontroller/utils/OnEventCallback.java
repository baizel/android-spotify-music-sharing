package com.baizelmathew.spotifycontroller.utils;

import com.spotify.protocol.types.PlayerState;

/**
 * helper class for play back event change callbacks
 */
public interface OnEventCallback {
    void onEvent(PlayerState playerState);
}
