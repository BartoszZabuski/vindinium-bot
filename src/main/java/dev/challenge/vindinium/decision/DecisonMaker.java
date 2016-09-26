package dev.challenge.vindinium.decision;

import static com.google.common.collect.Lists.newArrayList;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.TAVERN;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.EAST;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.NORTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.SOUTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.STAY;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.WEST;
import static java.lang.String.format;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import akka.actor.UntypedActor;
import dev.challenge.vindinium.decision.NearestTavernTracker.UpdateNearestTavern;
import dev.challenge.vindinium.decision.SuitableMineTracker.UpdateNearestSuitableMine;
import dev.challenge.vindinium.domain.GameState.Board;
import dev.challenge.vindinium.domain.GameState.Hero;
import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.infrastructure.messages.BotMove;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public class DecisonMaker extends UntypedActor {

    //    private Board board;
    private Map<Position, Node> boardGraph;
    //    private List<Node> taverns;
    private Hero myHero;
    private Integer heroId;

    private Queue<BotMove> movesQueue = new LinkedList<>();

    private Node nearestTawern;
    private Optional<Node> nearestSuitableMine;

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof Initialize) {
            String boardString = ((Initialize) message).getBoard().getTiles();
            Integer boardSize = ((Initialize) message).getBoard().getSize();
            heroId = ((Initialize) message).getHeroId();
            generateBoardGraph(boardString, boardSize);
            System.out.println(DecisonMaker.class.getSimpleName() + " board graph initialized");
        } else if (message instanceof FindNextMove) {
            myHero = ((FindNextMove) message).getHero();
            System.err.println("-------------- LIFE --- " + myHero.getLife());
            if (myHero.getLife() <= 45) {
                movesQueue.clear();
                List<BotMove> botMoves = findShortestPath(boardGraph.get(myHero.getPos()), nearestTawern, true);
                movesQueue.addAll(botMoves);
            }
            if (movesQueue.isEmpty()) {
                System.err.println("---> about to perform search");
                if (nearestSuitableMine.isPresent()) {
                    List<BotMove> botMoves = findShortestPath(boardGraph.get(myHero.getPos()), nearestSuitableMine.get(), false);
                    movesQueue.addAll(botMoves);
                }
            }
            BotMove poll = movesQueue.peek() == null ? STAY : movesQueue.remove();
            sender().tell(new Decision(poll), self());
        } else if (message instanceof NearestTavernTracker.UpdateNearestTavern) {
            nearestTawern = ((UpdateNearestTavern) message).getNearestTavern();
            System.err.println("---> nearest tavern updated to " + nearestTawern.getPosition());
        } else if (message instanceof SuitableMineTracker.UpdateNearestSuitableMine) {
            nearestSuitableMine = ((UpdateNearestSuitableMine) message).getNearestSuitableMine();
            if(nearestSuitableMine.isPresent()) {
                System.err.println("---> nearest mine updated to " + nearestSuitableMine.get().getPosition());
            }
        } else {
            unhandled(message);
        }
    }

    private void printMovesQueue() {
        StringBuilder concat = new StringBuilder();
        for (BotMove botMove : movesQueue) {
            concat.append(", " + botMove.toString());

        }
        System.err.println(concat.toString());
    }

    private List<BotMove> findShortestPath(Node hero, Node target, boolean tavernMode) {
        Queue<Node> searchQueue = new LinkedList<>();
        List<Node> visited = new ArrayList<>();

        searchQueue.add(hero);

        while (!searchQueue.isEmpty()) {
            Node current = searchQueue.remove();
//            visited.add(current);

            Optional<BotMove> isNextToGoal = current.isNeighbourWith(target.getPosition());
            if (!isNextToGoal.isPresent()) {
                List<Node> childrenToBeVisited = current.emptyNeighbourListExcludingVisited(visited);
                for (Node child : childrenToBeVisited) {
                    searchQueue.add(child);
                    visited.add(child);
                    child.setParentNode(current);
                }
            } else {
                target.setParentNode(current);
                break;
            }
        }

//            establish path
        LinkedList<BotMove> reversedPath = new LinkedList();
        Node next = target;
        while (true) {
            if (next != hero) {
                Node parentNode = next.getParentNode();
                Optional<BotMove> stepBack = next.isNeighbourWith(parentNode.getPosition());
                if (!stepBack.isPresent()) {
                    throw new RuntimeException("something went wrong in establishing path....");
                }
                reversedPath.add(stepBack.get().reverse());
                if (tavernMode) {
                    reversedPath.add(stepBack.get().reverse());
                }
                next = parentNode;
            } else {
                break;
            }
        }
        reversedPath = new LinkedList<>(newArrayList(reversedPath.descendingIterator()));
//        return new LinkedList<>(newArrayList(reversedPath.iterator()));
//        List<BotMove> temp = newArrayList(reversedPath.descendingIterator());
//        return temp;
        return reversedPath;
    }

    private void generateBoardGraph(String board, Integer boardSize) {
//        taverns = new ArrayList<>();
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
//                        taverns.add(tavern);
                    } else {
                        boardMap.put(position, new Node(position, EMPTY));
                    }
                }
            }
        }
        for (Node node : boardMap.values()) {
            node.updateNeighbours(getNeighboursFor(boardMap, node));
        }
        boardGraph = boardMap;

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
    public static class FindNextMove implements Serializable {

        private final Hero hero;

        public FindNextMove(Hero hero) {
            this.hero = hero;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class Decision implements Serializable {

        private final BotMove nextDirection;

        public Decision(BotMove nextDirection) {
            this.nextDirection = nextDirection;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class Initialize implements Serializable {

        private final Board board;
        private final int heroId;

        public Initialize(Board board, int heroId) {
            this.heroId = heroId;
            this.board = board;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class UpdateBoardInfo implements Serializable {

        private final Board board;

        public UpdateBoardInfo(Board board) {
            this.board = board;
        }
    }
}
