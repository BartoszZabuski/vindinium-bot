package dev.challenge.vindinium.domain.messages;

import com.google.api.client.util.Key;

import akka.http.javadsl.model.RequestEntity;

/**
 * Represents a move response to a server
 * <p/>
 * This is the full response sent back to the server, not to be confused with BotMove,
 * which only really represents the direction.
 */
public class Move{

    @Key
    private final String key;

    @Key
    private final String dir;

    public Move(String key, String dir) {
        this.key = key;
        this.dir = dir;
    }

    public String getKey() {
        return key;
    }

    public String getDir() {
        return dir;
    }
}
