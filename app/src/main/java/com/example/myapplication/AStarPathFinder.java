package com.example.myapplication;
import com.skt.Tmap.TMapPoint;
import java.util.*;

public class AStarPathFinder {
    private static final int DIAGONAL_COST = 14;
    private static final int V_H_COST = 10;

    private PriorityQueue<Node> openList;
    private Set<Node> closedSet;
    private TMapPoint[][] map;
    private Node startNode;
    private Node endNode;

    public AStarPathFinder(TMapPoint[][] map, TMapPoint startPoint, TMapPoint endPoint) {
        this.openList = new PriorityQueue<>();
        this.closedSet = new HashSet<>();
        this.map = map;
        this.startNode = new Node(startPoint.getLatitude(), startPoint.getLongitude());
        this.endNode = new Node(endPoint.getLatitude(), endPoint.getLongitude());
        this.startNode.setG(0);
        this.startNode.setH(manhattanDistance(this.startNode, this.endNode));
        this.startNode.setF(this.startNode.getG() + this.startNode.getH());
        this.openList.add(this.startNode);
    }

    public List<Node> findPath() {
        while (!openList.isEmpty()) {
            Node current = openList.poll();
            closedSet.add(current);

            if (current.equals(endNode)) {
                return getPath(current);
            }

            for (Node neighbor : getNeighbors(current)) {
                double tentativeGScore = current.getG() + getDistance(current, neighbor);

                if (closedSet.contains(neighbor) && tentativeGScore >= neighbor.getG()) {
                    continue;
                }

                if (!openList.contains(neighbor) || tentativeGScore < neighbor.getG()) {
                    neighbor.setParent(current);
                    neighbor.setG(tentativeGScore);
                    neighbor.setH(manhattanDistance(neighbor, endNode));
                    neighbor.setF(neighbor.getG() + neighbor.getH());

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    private double manhattanDistance(Node node1, Node node2) {
        double dx = Math.abs(node1.getX() - node2.getX());
        double dy = Math.abs(node1.getY() - node2.getY());
        return V_H_COST * (dx + dy);
    }

    private double diagonalDistance(Node node1, Node node2) {
        double dx = Math.abs(node1.getX() - node2.getX());
        double dy = Math.abs(node1.getY() - node2.getY());
        return DIAGONAL_COST * Math.min(dx, dy) + V_H_COST * (dx + dy - 2 * Math.min(dx, dy));
    }

    private double getDistance(Node node1, Node node2) {
        return node1.getX() == node2.getX() || node1.getY() == node2.getY() ? V_H_COST : DIAGONAL_COST;
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();

        for (int x = (int) node.getX() - 1; x <= (int) node.getX() + 1; x++) {
            for (int y = (int) node.getY() - 1; y <= (int) node.getY() + 1; y++) {
                if (x == (int) node.getX() && y == (int) node.getY()) {
                    continue;
                }

                if (x >= 0 && x < map.length && y >= 0 && y < map[x].length) {
                    neighbors.add(new Node(x, y));
                }
            }
        }

        return neighbors;
    }

    private List<Node> getPath(Node targetNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = targetNode;

        while (currentNode != null) {
            path.add(currentNode);
            currentNode = currentNode.getParent();
        }

        Collections.reverse(path);
        return path;
    }
}