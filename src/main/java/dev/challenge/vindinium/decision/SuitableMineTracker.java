package dev.challenge.vindinium.decision;

import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.TAVERN;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.EAST;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.NORTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.SOUTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.WEST;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.Procedure;
import dev.challenge.vindinium.decision.DecisonMaker.Initialize;
import dev.challenge.vindinium.decision.Node.NodeType;
import dev.challenge.vindinium.domain.GameState.Board;
import dev.challenge.vindinium.domain.GameState.Hero;
import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.infrastructure.messages.BotMove;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class SuitableMineTracker extends UntypedActor {

    private Board board;
    private Map<Integer, Node> positionOnBoardStringToMines = new HashMap<>();
    private Map<Position, Node> boardGraph;
    private Node nearestMine;
    private Hero myHero;

    private ActorRef decisionMaker;

    private SuitableMineTracker(ActorRef decisionMaker) {
        this.decisionMaker = decisionMaker;
    }

    private Procedure<Object> running = new Procedure<Object>() {
        @Override
        public void apply(Object message) throws Exception {
            if (message instanceof UpdateHeroAndBoardInfo) {
                System.err.println(SuitableMineTracker.class.getSimpleName() + " on UpdateHeroAndBoardInfo 1");
                board = ((UpdateHeroAndBoardInfo) message).getBoard();
                myHero = ((UpdateHeroAndBoardInfo) message).getHero();
                updateGraphWithRecentMineOwnerships(((UpdateHeroAndBoardInfo) message).getBoard().getTiles(), positionOnBoardStringToMines);
                nearestMine = findNearestSuitableMineTo(myHero.getPos());
                decisionMaker.tell(new UpdateNearestSuitableMine(Optional.ofNullable(nearestMine)), self());
                System.err.println(SuitableMineTracker.class.getSimpleName() + " on UpdateHeroAndBoardInfo 2");
            } else {
                unhandled(message);
            }
        }
    };

    private void updateGraphWithRecentMineOwnerships(String updatedBoardString, Map<Integer, Node> positionOnBoardStringToMines) {
        for (Entry<Integer, Node> positionNodeEntry : positionOnBoardStringToMines.entrySet()) {
            Node mineToBeUpdated = positionNodeEntry.getValue();
            mineToBeUpdated.updateType(NodeType.parseTile(updatedBoardString.substring(positionNodeEntry.getKey(), positionNodeEntry.getKey() + 2), myHero.getId()));
        }
    }

    private Integer calculatePositionOnBoardString(int row, int col) {
        return row * board.getSize() * 2 + (col * 2);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Initialize) {
            board = ((Initialize) message).getBoard();
            String boardString = ((Initialize) message).getBoard().getTiles();
            Integer boardSize = ((Initialize) message).getBoard().getSize();
            boardGraph = generateBoardGraph(boardString, boardSize);
            System.out.println(SuitableMineTracker.class.getSimpleName() + " board graph initialized");
            getContext().become(running);
        } else {
            unhandled(message);
        }
    }

    private Node findNearestSuitableMineTo(Position heroPos) {
        Node found = null;
        Queue<Node> searchQueue = new LinkedList<>();
        List<Node> visited = new ArrayList<>();

        Node hero = boardGraph.get(heroPos);
        searchQueue.add(hero);

        while (!searchQueue.isEmpty()) {
            Node current = searchQueue.remove();

            if (current.isEmptyMine() || current.isEnemyMine()) {
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
                        positionOnBoardStringToMines.put(calculatePositionOnBoardString(row, col / 2), mineNode);
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
    public class UpdateNearestSuitableMine {

        private final Optional<Node> nearestSuitableMine;

        public UpdateNearestSuitableMine(Optional<Node> nearestSuitableMine) {
            this.nearestSuitableMine = nearestSuitableMine;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class UpdateHeroAndBoardInfo {

        private final Hero hero;
        private final Board board;

        public UpdateHeroAndBoardInfo(Hero hero, Board board) {
            this.hero = hero;
            this.board = board;
        }
    }

}
