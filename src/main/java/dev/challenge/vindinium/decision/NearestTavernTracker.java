package dev.challenge.vindinium.decision;

import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.TAVERN;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.EAST;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.NORTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.SOUTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.WEST;
import static java.lang.String.format;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.Procedure;
import dev.challenge.vindinium.decision.DecisonMaker.Initialize;
import dev.challenge.vindinium.domain.GameState.Hero;
import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.infrastructure.messages.BotMove;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class NearestTavernTracker extends UntypedActor {

    private Map<Position, Node> boardGraph;
    private Node nearestTawern;
    private Hero myHero;

    private ActorRef decisonMaker;

    private NearestTavernTracker(ActorRef decisonMaker) {
        this.decisonMaker = decisonMaker;
    }

    private Procedure<Object> running = new Procedure<Object>() {
        @Override
        public void apply(Object message) throws Exception {
            if (message instanceof UpdateHeroPosition) {
                myHero = ((UpdateHeroPosition) message).getHero();
                nearestTawern = findNearestTavernTo(myHero.getPos());
                decisonMaker.tell(new UpdateNearestTavern(nearestTawern), self());
            } else {
                unhandled(message);
            }
        }
    };

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Initialize) {
            String boardString = ((Initialize) message).getBoard().getTiles();
            Integer boardSize = ((Initialize) message).getBoard().getSize();
            boardGraph = generateBoardGraph(boardString, boardSize);
            System.out.println(NearestTavernTracker.class.getSimpleName() + " board graph initialized");
            getContext().become(running);
        } else {
            unhandled(message);
        }
    }

    private Node findNearestTavernTo(Position heroPos) {
        Node found = null;
        Queue<Node> searchQueue = new LinkedList<>();
        List<Node> visited = new ArrayList<>();

        Node hero = boardGraph.get(heroPos);
        searchQueue.add(hero);

        while (!searchQueue.isEmpty()) {
            Node current = searchQueue.remove();

            if (current.isTavern()) {
                found = current;
                break;
            } else {
                List<Node> childrenToBeVisited = current.neighboursExcludingVisited(visited);
                searchQueue.addAll(childrenToBeVisited);
                visited.addAll(childrenToBeVisited);
            }
        }
        return found;
    }

    private Map<Position, Node> generateBoardGraph(String board, Integer boardSize) {
        Map<Position, Node> boardMap = new HashMap<>();
        int boardSizeInChar = boardSize * 2;
        for (int row = 0; row < boardSize; row++) {
            int beginningOfRow = row * boardSizeInChar;
            String rowString = board.substring(beginningOfRow, beginningOfRow + boardSizeInChar);
            for (int col = 0; col < boardSizeInChar; col += 2) {
                String tile = rowString.substring(col, col + 2);
                if (!tile.equals("##")) {
                    System.out.println(format("pos: %s %s - %s", row, col / 2, tile));
                    Position position = new Position(row, col / 2);
                    if (tile.contains("$")) {
                        Node mineNode = new Node(position, EMPTY_MINE);
                        boardMap.put(position, mineNode);
                    } else if (tile.equals("[]")) {
                        Node tavern = new Node(position, TAVERN);
                        boardMap.put(position, tavern);
                    } else {
                        boardMap.put(position, new Node(position, EMPTY));
                    }
                }
            }
        }
        for (Node node : boardMap.values()) {
            node.updateNeighbours(getNeighboursFor(boardMap, node));
        }
        return boardMap;
    }

    private Map<BotMove, Node> getNeighboursFor(Map<Position, Node> boardMap, final Node node) {
        Map<BotMove, Node> neighbours = new HashMap<>();
        final Position nodePos = node.getPosition();

        neighbours.put(EAST, boardMap.get(nodePos.moveOne(EAST)));
        neighbours.put(WEST, boardMap.get(nodePos.moveOne(WEST)));
        neighbours.put(NORTH, boardMap.get(nodePos.moveOne(NORTH)));
        neighbours.put(SOUTH, boardMap.get(nodePos.moveOne(SOUTH)));

        return neighbours;
    }

    @Getter
    @EqualsAndHashCode
    public static class UpdateHeroPosition implements Serializable {

        private final Hero hero;

        public UpdateHeroPosition(Hero hero) {
            this.hero = hero;
        }
    }


    @Getter
    @EqualsAndHashCode
    public class UpdateNearestTavern {

        private final Node nearestTavern;

        public UpdateNearestTavern(Node nearestTavern) {
            this.nearestTavern = nearestTavern;
        }
    }
}
