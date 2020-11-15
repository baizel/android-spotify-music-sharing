package com.baizelmathew.spotifycontroller.utils;

import com.baizelmathew.spotifycontroller.spotify_wrapper.UserQueue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotify.protocol.types.PlayerState;

public class Util {
    public static String getJsonFormatOfPlayerState(PlayerState playerState, UserQueue queue) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.convertValue(playerState, ObjectNode.class);
        node.set("queue", mapper.convertValue(queue.getQueue(), JsonNode.class));
        node.set("queueCurrentPos", mapper.convertValue(queue.getCurrentPosition(), JsonNode.class));
        return node.toString();
    }
}
