package com.baizelmathew.spotifycontroller.eventbus_messeages;

import com.baizelmathew.spotifycontroller.eventbus_messeages.enums.PlayerActions;

public class PlayerActionEvent {
    PlayerActions playerActions;
    String payload;

    public PlayerActionEvent(PlayerActions playerActions, String payload) {
        this.playerActions = playerActions;
        this.payload = payload;
    }

    public PlayerActions getPlayerActions() {
        return playerActions;
    }

    public String getPayload() {
        return payload;
    }
}
