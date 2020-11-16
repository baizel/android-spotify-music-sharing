package com.baizelmathew.spotifycontroller.eventbus_messeages.enums;

public enum PlayerActions {
    TOGGLE_PLAY("play"),
    PREVIOUS("previous"),
    NEXT("next"),
    ADD_TO_QUEUE("playUri"),
    SEEK_TO("seek"),
    UNKNOWN("unknown");

    private String val;

    PlayerActions(String val) {
        this.val = val;
    }

    public String getVal() {
        return this.val;
    }

    public static PlayerActions fromString(String text) {
        for (PlayerActions playerActions : PlayerActions.values()) {
            if (playerActions.val.equalsIgnoreCase(text)) {
                return playerActions;
            }
        }
        return UNKNOWN;
    }
}
