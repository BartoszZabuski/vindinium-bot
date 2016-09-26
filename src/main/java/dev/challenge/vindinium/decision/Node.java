package dev.challenge.vindinium.decision;

import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY;
import static dev.challenge.vindinium.decision.Node.NodeType.EMPTY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.ENEMY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.MY_MINE;
import static dev.challenge.vindinium.decision.Node.NodeType.TAVERN;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import dev.challenge.vindinium.domain.GameState.Position;
import dev.challenge.vindinium.infrastructure.messages.BotMove;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(exclude = {"nodeType", "neighbors", "pathParent"})
public class Node {

    @Getter
    private final Position position;
    @Getter
    private NodeType nodeType;

    private Map<BotMove, Node> neighbors;
    @Getter
    @Setter
    private Node parentNode;

    public Node(Position position, NodeType nodeType) {
        this.position = position;
        this.nodeType = nodeType;
    }

    public void updateNeighbours(Map<BotMove, Node> neighbours) {
        this.neighbors = neighbours;
    }

    public Optional<BotMove> isNeighbourWith(Position position) {
        for (Entry<BotMove, Node> entry : neighbors.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getPosition().equals(position)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public List<Node> emptyNeighbourListExcludingVisited(List<Node> visited) {
        return neighbors.values().stream().filter(node -> node != null).filter(node -> node.onlyEmpty()).filter(node -> !visited.contains(node)).collect(toList());
    }

    public List<Node> neighboursExcludingVisited(List<Node> visited) {
        return neighbors.values().stream().filter(node -> node != null).filter(node -> !visited.contains(node)).collect(toList());
    }

    public void updateType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    private boolean onlyEmpty() {
        return nodeType.equals(EMPTY);
    }

    public boolean isMyMine() {
        return nodeType.equals(MY_MINE);
    }

    public boolean isEmptyMine() {
        return nodeType.equals(EMPTY_MINE);
    }

    public boolean isEnemyMine() {
        return nodeType.equals(ENEMY_MINE);
    }

    public boolean isTavern() {
        return nodeType.equals(TAVERN);
    }

    public enum NodeType {
        EMPTY_MINE, MY_MINE, ENEMY_MINE, TAVERN, EMPTY;

        public static NodeType parseTile(String tile, Integer myHeroId) {
            NodeType node = null;
            if (tile.equals("$-")) {
                node = NodeType.EMPTY_MINE;
            } else if (tile.equals("$" + myHeroId)) {
                node = MY_MINE;
            } else if (tile.contains("$")) {
                node = NodeType.ENEMY_MINE;
            } else {
                new RuntimeException("unprasable mine!!!!!!!!");
            }
            return node;
        }
    }
}
