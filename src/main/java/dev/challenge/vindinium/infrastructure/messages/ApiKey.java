package dev.challenge.vindinium.infrastructure.messages;

import com.google.api.client.util.Key;


public class ApiKey {

    @Key
    private final String key;

    public ApiKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
