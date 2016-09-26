package dev.challenge.vindinium;


import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedProducerActor;
import dev.challenge.vindinium.domain.GameState;
import dev.challenge.vindinium.domain.messages.InitialRequest;
import dev.challenge.vindinium.infrastructure.messages.ApiKey;

public class InitialGameStateRetriever extends UntypedProducerActor {
//            private final static String TRAINING_URL = "http://vindinium.org/api/training";
        private final static String COMPETITION_URL = "http://vindinium.org/api/arena";

    private final Gson gson = new Gson();

    private static final Map<String, Object> HEADERS = new HashMap<>();

    {
        HEADERS.put("Content-Type", "application/json");
    }

//    private final static String TRAINING_URL = "http://vindinium.org/api/training";

    private final static String LOCAL_TRAINING_URL = "http://127.0.0.1:9000/api/training";
//    private final static String LOCAL_COMPETITION_URL = "http://localhost:9000/api/arena";

    public static String URL = COMPETITION_URL;

    @Override
    public String getEndpointUri() {
        return  COMPETITION_URL;
    }

    @Override
    public Object onTransformOutgoingMessage(Object message) {
        if (message instanceof InitialRequest) {
            return new CamelMessage(gson.toJson(message), HEADERS);
        } else if (message instanceof ApiKey) {
            return new CamelMessage(gson.toJson(message), HEADERS);
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
