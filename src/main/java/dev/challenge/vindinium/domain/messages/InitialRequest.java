package dev.challenge.vindinium.domain.messages;

import com.google.api.client.util.Key;

import lombok.Getter;

@Getter
public class InitialRequest {

    @Key
    private final Integer turns;
    @Key
    private final String key;
    @Key
    private final String map;

    public InitialRequest(String key, Integer numberOfTurns, String map) {
        this.key = key;
        this.turns = numberOfTurns;
        this.map = map;
    }
}
