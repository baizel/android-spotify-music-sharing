package com.baizelmathew.spotifycontroller.eventbus_messeages;

import com.baizelmathew.spotifycontroller.eventbus_messeages.enums.ServerState;

public class ServerAddressEvent {
    String serverAddr;
    ServerState state;

    public ServerAddressEvent(ServerState state, String serverAddr) {
        this.serverAddr = serverAddr;
        this.state = state;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public ServerState getState() {
        return state;
    }
}
