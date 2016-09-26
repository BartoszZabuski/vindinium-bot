package gamesys.casino.hostapp.domain;


import static dev.challenge.vindinium.infrastructure.messages.BotMove.EAST;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.NORTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.SOUTH;
import static dev.challenge.vindinium.infrastructure.messages.BotMove.WEST;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.TAVERN;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;

import dev.challenge.vindinium.infrastructure.messages.BotMove;
import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.decision.Node;
import dev.challenge.vindinium.decision.Node.NodeType;

@Ignore
public class BoardUTest {

    private static String board = "##      ####                ####      ######$-      ##  ########  ##      $-########    $-                    $-    ########$-    $-  $-        $-  $-    $-##########    ##  []        []  ##    ##############    ####  ####  ####    ############      $-##    ####    ##$-      ########  @1####                ####@4  ######    ####  ##            ##  ####    ######    ##                    ##    ########    ##                    ##    ######    ####  ##            ##  ####    ######  @2####                ####@3  ########      $-##    ####    ##$-      ############    ####  ####  ####    ##############    ##  []        []  ##    ##########$-    $-  $-        $-  $-    $-########    $-                    $-    ########$-      ##  ########  ##      $-######      ####                ####      ##";
    private static String smallBoard = "##@1    ####  @4  ##      ########              ####            []        []    $1    ##    ##    $2$3    ##    ##    $-    []        []            ####  @3          ########      ##  @2  ####      ##";
    private static String smallBoardWithTakenMines = "##@1    ####  @4  ##      ########              ####            []        []    $-    ##    ##    $-$-    ##    ##    $-    []        []            ####  @3          ########      ##  @2  ####      ##";

    private static Integer smallboardSize = 10;
    private static Integer boardSize = 20;

    @Test
    public void printMap() {
        for (int row = 0; row < boardSize; row++) {
            int beginningOfRow = row * boardSize * 2;
            System.out.println(board.substring(beginningOfRow, beginningOfRow + (boardSize * 2)));
        }
    }

    @Test
    public void printMap22() {
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
                        boardMap.put(position, new Node(position, EMPTY_MINE));
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
        System.out.println();

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


    @Test
    public void establishMinesPositions() {
        printMap();

//        positionOnBoardStringToMines = new ArrayList<>();
        Map<Integer, Node> positionOnBoardStringToMines = new HashMap<>();

        List<Position> taverns = new ArrayList<>();
        Map<Position, Node> boardMap = new HashMap<>();
        int boardSizeInChar = smallboardSize * 2;
        for (int row = 0; row < smallboardSize; row++) {
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
                        positionOnBoardStringToMines.put(calculatePositionOnBoardString(row, col), mineNode);
                    } else if (tile.equals("[]")) {
                        boardMap.put(position, new Node(position, TAVERN));
                        taverns.add(position);
                    } else {
                        boardMap.put(position, new Node(position, EMPTY));
                    }
                }
            }
        }
        for (Node node : boardMap.values()) {
            node.updateNeighbours(getNeighboursFor(boardMap, node));
        }
        System.out.println();

//        update mines
        for (Entry<Integer, Node> integerNodeEntry : positionOnBoardStringToMines.entrySet()) {
            NodeType nodeType = NodeType.parseTile(smallBoardWithTakenMines.substring(integerNodeEntry.getKey(), integerNodeEntry.getKey() + 2), 1);
            integerNodeEntry.getValue().updateType(nodeType);
            System.out.println("-- updating " + nodeType.toString());
        }
        System.out.println();
    }

    private Integer calculatePositionOnBoardString(int row, int col) {
        return row * boardSize + col;
    }

//    establish mines positions on the string board so you can keep updating if whether they changed the owner or not and direct hero to next one

}
