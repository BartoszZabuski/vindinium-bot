package dev.challenge.vindinium;

import static dev.challenge.vindinium.decision.SuitableMineTracker.UpdateHeroAndBoardInfo;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.STAY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.Serializable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import dev.challenge.vindinium.decision.DecisonMaker;
import dev.challenge.vindinium.decision.DecisonMaker.Decision;
import dev.challenge.vindinium.decision.DecisonMaker.FindNextMove;
import dev.challenge.vindinium.decision.DecisonMaker.Initialize;
import dev.challenge.vindinium.decision.NearestTavernTracker;
import dev.challenge.vindinium.decision.NearestTavernTracker.UpdateHeroPosition;
import dev.challenge.vindinium.decision.SuitableMineTracker;
import dev.challenge.vindinium.domain.GameState;
import dev.challenge.vindinium.domain.messages.Move;
import dev.challenge.vindinium.infrastructure.messages.ApiKey;
import lombok.Data;
import scala.concurrent.duration.FiniteDuration;


public class MainActor extends UntypedActor {
    //  official
    private static final ApiKey API_KEY = new ApiKey("unqlnah7");

    //    local
//    private static final ApiKey API_KEY = new ApiKey("qupy3kop");
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final ActorRef initialGameRetriever;

    private GameState gameState;

    private ActorRef gameTurnsStateRetriever;
    private ActorRef decisonMaker;
    private ActorRef nearestTavernTracker;
    private ActorRef nearestSuitableMineTracker;

    private String nextDirection = STAY.toString();
    private Procedure<Object> gameTurnsMode = (message) -> {
        if (message instanceof MakeAMoveCommand) {
            makeAMove();
            if (gameState.getGame().isFinished()) {
                log.debug("---> sending GameFinished from within MakeAMoveCommand to  " + getContext().parent());
                getContext().parent().tell(new GamesManagerActor.GameFinished(), self());
            }
        } else if (message instanceof GameState) {
                gameState = (GameState) message;
                nearestTavernTracker.tell(new UpdateHeroPosition(gameState.getHero()), self());
                nearestSuitableMineTracker.tell(new UpdateHeroAndBoardInfo(gameState.getHero(), gameState.getGame().getBoard()), self());
//            nearestSuitableMineTracker.tell(new DecisonMaker.UpdateBoardInfo(gameState.getGame().getBoard()), self());
                decisonMaker.tell(new FindNextMove(gameState.getHero()), self());
                log.debug("---> updating gameState. turn: " + gameState.getGame().getTurn());
        } else if (message instanceof Decision) {
            nextDirection = ((Decision) message).getNextDirection().toString();
            log.debug("---> updating next direction to :" + nextDirection);
        } else {
            unhandled(message);
        }
    };

    private void makeAMove() {
        if (!gameState.getGame().isFinished() && !gameState.getHero().isCrashed()) {
            log.debug("making a move!");
            gameTurnsStateRetriever.tell(new Move(API_KEY.getKey(), nextDirection), self());
        }
    }

    public static Props props() {
        return Props.create(MainActor.class,
                () -> new MainActor());
    }

    private MainActor() throws InterruptedException {
        initialGameRetriever = context().actorOf(Props.create(InitialGameStateRetriever.class), "initialGameRetriever");
        Thread.sleep(2000L);
//        initialGameRetriever.tell(new InitialRequest(API_KEY.getKey(), 100, "m4"), self());
        initialGameRetriever.tell(API_KEY, self());
        decisonMaker = context().actorOf(Props.create(DecisonMaker.class));

        nearestTavernTracker = context().actorOf(Props.create(NearestTavernTracker.class, decisonMaker));
        nearestSuitableMineTracker = context().actorOf(Props.create(SuitableMineTracker.class, decisonMaker));
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof GameState) {
            log.debug("--> initializing gameState");
            gameState = (GameState) message;
            final Initialize initializeMsg = new Initialize(gameState.getGame().getBoard(), gameState.getHero().getId());
            decisonMaker.tell(initializeMsg, self());
            nearestSuitableMineTracker.tell(initializeMsg, self());
            nearestSuitableMineTracker.tell(new UpdateHeroAndBoardInfo(gameState.getHero(), gameState.getGame().getBoard()), self());
            nearestTavernTracker.tell(initializeMsg, self());
            gameTurnsStateRetriever = context().actorOf(Props.create(GenericGameStateRetriever.class, gameState.getPlayUrl()));
            getContext().become(gameTurnsMode);
            getContext().system().scheduler().schedule(new FiniteDuration(750L, MILLISECONDS),
                    new FiniteDuration(750L, MILLISECONDS), self(), new MakeAMoveCommand(),
                    context().dispatcher(), self());

            log.debug("--> view url: " + gameState.getViewUrl());
            printMap(gameState.getGame().getBoard().getSize(), gameState.getGame().getBoard().getTiles());
        } else {
            unhandled(message);
        }
    }

    @Data
    static final class MakeAMoveCommand implements Serializable {
        private static final long serialVersionUID = 2979862921003429585L;
    }

    private void printMap(Integer boardSize, String board) {
        for (int row = 0; row < boardSize; row++) {
            int beginningOfRow = row * boardSize * 2;
            System.out.println(board.substring(beginningOfRow, beginningOfRow + (boardSize * 2)));
        }
    }

}
