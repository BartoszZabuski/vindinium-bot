package dev.challenge.vindinium;


import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedProducerActor;
import dev.challenge.vindinium.domain.GameState;
import dev.challenge.vindinium.domain.messages.Move;

public class GenericGameStateRetriever extends UntypedProducerActor {

    private final Gson gson = new Gson();

    private static final Map<String, Object> HEADERS = new HashMap<>();

    {
        HEADERS.put("Content-Type", "application/json");
    }

    private String playUrl;

    private GenericGameStateRetriever(String playUrl) {
        this.playUrl = playUrl;
    }

    @Override
    public String getEndpointUri() {
        return playUrl;
    }

    @Override
    public Object onTransformOutgoingMessage(Object message) {
        if (message instanceof Move) {
            String body = gson.toJson(message);
            System.out.println("sending.. -> " + body);
            return new CamelMessage(body, HEADERS);
        } else {
            return null;
        }
    }

    @Override
    public void onRouteResponse(Object message) {
        if (message instanceof CamelMessage) {
            final String bodyString = ((CamelMessage) message).getBodyAs(String.class, getCamelContext());
            sender().forward(gson.fromJson(bodyString, GameState.class), getContext());
        }
    }


}
