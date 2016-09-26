package dev.challenge.vindinium;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class GamesManagerActor extends UntypedActor {

    private ActorRef mainActor = getContext().actorOf(Props.create(MainActor.class));
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof GameFinished) {
            getContext().stop(mainActor);
            mainActor = getContext().actorOf(Props.create(MainActor.class));
            log.debug("!!!!!!! restarting game !!!!!!");
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class GameFinished {

        public GameFinished() {
        }
    }
}
