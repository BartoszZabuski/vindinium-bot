package gamesys.casino.hostapp.domain;


import static com.google.common.collect.Lists.newArrayList;
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

import dev.challenge.vindinium.decision.Node;
import dev.challenge.vindinium.decision.Node.NodeType;
import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.infrastructure.messages.BotMove;

public class SmallBoardUTest {

    public static final int MY_HERO_ID = 1;
    private static String smallBoard = "##@1    ####  @4  ##      ########              ####            []        []    $-    ##    ##    $-$-    ##    ##    $-    []        []            ####  @3          ########      ##  @2  ####      ##";
    private static String smallBoardWithTakenMines = "##@1    ####  @4  ##      ########              ####            []        []    $1    ##    ##    $2$3    ##    ##    $-    []        []            ####  @3          ########      ##  @2  ####      ##";

    private static Integer smallBoardSize = 10;

    public static void main(String args[]) {
        printMap();

        Map<Integer, Node> positionOnBoardStringToMines = new HashMap<>();
        Map<Position, Node> boardMap = initializeGraph(positionOnBoardStringToMines);
        Position heroPosition = new Position(0, 1);
        Position minePosition = new Position(5, 9);
        Queue<BotMove> pathTo = findPath(boardMap.get(heroPosition), boardMap.get(minePosition));

        updateGraphWithRecentMineOwnerships(smallBoardWithTakenMines, positionOnBoardStringToMines);
        System.out.println();
    }

    private static void updateGraphWithRecentMineOwnerships(String updatedBoardString, Map<Integer, Node> positionOnBoardStringToMines) {
        for (Entry<Integer, Node> positionNodeEntry : positionOnBoardStringToMines.entrySet()) {
            Node mineToBeUpdated = positionNodeEntry.getValue();
            mineToBeUpdated.updateType(NodeType.parseTile(updatedBoardString.substring(positionNodeEntry.getKey(), positionNodeEntry.getKey() + 2), MY_HERO_ID));
        }
    }

    private static Queue<BotMove> findPath(Node hero, Node mine) {
        Queue<Node> searchQueue = new LinkedList<>();
        List<Node> visited = new ArrayList<>();

        searchQueue.add(hero);

        while (!searchQueue.isEmpty()) {
            Node current = searchQueue.remove();
//            visited.add(current);

            Optional<BotMove> isNextToGoal = current.isNeighbourWith(mine.getPosition());
            if (!isNextToGoal.isPresent()) {
                List<Node> childrenToBeVisited = current.emptyNeighbourListExcludingVisited(visited);
                for (Node child : childrenToBeVisited) {
                    searchQueue.add(child);
                    visited.add(child);
                    child.setParentNode(current);
                }
            } else {
                mine.setParentNode(current);
                break;
            }
        }

//            establish path
        LinkedList<BotMove> reversedPath = new LinkedList();
        Node next = mine;
        while (true) {
            if (next != hero) {
                Node parentNode = next.getParentNode();
                Optional<BotMove> stepBack = next.isNeighbourWith(parentNode.getPosition());
                if (!stepBack.isPresent()) {
                    throw new RuntimeException("something went wrong in establishing path....");
                }
                reversedPath.add(stepBack.get().reverse());
                next = parentNode;
            } else {
                break;
            }
        }
        return new LinkedList<>(newArrayList(reversedPath.descendingIterator()));
    }

    public static void printMap() {
        for (int row = 0; row < smallBoardSize; row++) {
            int beginningOfRow = row * smallBoardSize * 2;
            System.out.println(smallBoard.substring(beginningOfRow, beginningOfRow + (smallBoardSize * 2)));
        }
    }

    public static Map<Position, Node> initializeGraph(Map<Integer, Node> positionOnBoardStringToMines) {
        Map<Position, Node> boardMap = new HashMap<>();
        int boardSizeInChar = smallBoardSize * 2;
        for (int row = 0; row < smallBoardSize; row++) {
            int beginningOfRow = row * boardSizeInChar;
            String rowString = smallBoard.substring(beginningOfRow, beginningOfRow + boardSizeInChar);
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
                        boardMap.put(position, new Node(position, TAVERN));
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

//    private void findNearestMine(Position heroPosition) {
//
////        only if finished job
//        if (movesQueue.isEmpty()) {
//            List<BotMove> directions = new LinkedList();
//            Map<Node, Node> prev = new HashMap<>();
//
//            Position startNodePos = heroPosition;
//            Position goalPosition = positionOnBoardStringToMines.values().iterator().next().getPosition();
//            Node goalNode = boardGraph.get(goalPosition);
//
//            Node startNode = boardGraph.get(startNodePos);
//
//            Optional<BotMove> neighbourWith = startNode.isNeighbourWith(goalPosition);
//            if (neighbourWith.isPresent()) {
//                System.out.println("Goal Node Found!");
//                return neighbourWith.get();
//            }
//
//            Queue<Node> queue = new LinkedList<>();
//            ArrayList<Node> explored = new ArrayList<>();
//            queue.add(startNode);
//            explored.add(startNode);
//
//            while (!queue.isEmpty()) {
//                Node current = queue.remove();
//                Optional<BotMove> neighbourDirection = current.isNeighbourWith(goalPosition);
//                if (neighbourDirection.isPresent()) {
//                    BotMove lastDirection = neighbourDirection.get();
//                    for (Node node = goalNode; node != null; node = prev.get(node)) {
//                        directions.add(node.get);
//                    }
////                attack mode :P really bad way...
//                    directions.add(lastDirection);
//                    directions.add(lastDirection);
//                    directions.add(lastDirection);
//                    movesQueue.addAll(directions.stream().map(node -> node.));
////                return lastDirection;
//                } else {
//                    current.emptyNeighbourListExcludingVisited(explored).forEach(node -> {
//                        queue.add(node);
//                        prev.put(node, current);
//                    });
//                }
//                explored.add(current);
//            }
//        }
//
//        return BotMove.STAY;
//    }

    private static Integer calculatePositionOnBoardString(int row, int col) {
        return row * smallBoardSize * 2 + (col * 2);
    }

    private static Integer calculatePositionOnBoardString(Position position) {
        return position.getX() * smallBoardSize * 2 + (position.getY() * 2);
    }

    private static Map<BotMove, Node> getNeighboursFor(Map<Position, Node> boardMap, final Node node) {
        Map<BotMove, Node> neighbours = new HashMap<>();
        final Position nodePos = node.getPosition();

        neighbours.put(EAST, boardMap.get(nodePos.moveOne(EAST)));
        neighbours.put(WEST, boardMap.get(nodePos.moveOne(WEST)));
        neighbours.put(NORTH, boardMap.get(nodePos.moveOne(NORTH)));
        neighbours.put(SOUTH, boardMap.get(nodePos.moveOne(SOUTH)));

        return neighbours;
    }


}
