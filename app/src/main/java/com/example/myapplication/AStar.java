package com.example.myapplication;

import java.util.*;

public class AStar {
    // A* 알고리즘을 이용하여 최단 경로 탐색
    public List<String> findShortestPath(RoadGraphBuilder.Graph graph, String startNodeId, String targetNodeId) {
        // 시작 노드와 목표 노드의 정보를 가져옴
        RoadGraphBuilder.Node startNode = graph.getNode(startNodeId);
        RoadGraphBuilder.Node targetNode = graph.getNode(targetNodeId);

        // 시작 노드부터의 예상 비용을 저장하는 맵
        Map<String, Double> estimatedCosts = new HashMap<>();
        // 시작 노드부터의 실제 비용을 저장하는 맵
        Map<String, Double> actualCosts = new HashMap<>();
        // 방문한 노드를 저장하는 집합
        Set<String> visited = new HashSet<>();
        // 이전 노드를 저장하는 맵 (최단 경로 추적용)
        Map<String, String> previousNodes = new HashMap<>();
        // 시작 노드의 예상 비용을 0으로 초기화
        estimatedCosts.put(startNodeId, 0.0);
        // 시작 노드부터의 실제 비용을 0으로 초기화
        actualCosts.put(startNodeId, 0.0);

        // 우선순위 큐를 이용하여 예상 비용이 낮은 노드를 우선적으로 방문
        PriorityQueue<String> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(node -> estimatedCosts.getOrDefault(node, Double.POSITIVE_INFINITY)));
        priorityQueue.offer(startNodeId);

        while (!priorityQueue.isEmpty()) {
            String currentNodeId = priorityQueue.poll();

            // 목표 노드에 도착하면 탐색 종료
            if (currentNodeId.equals(targetNodeId)) {
                break;
            }

            visited.add(currentNodeId);

            // 현재 노드의 이웃 노드들을 가져옴
            List<RoadGraphBuilder.Edge> neighbors = graph.getNeighbors(currentNodeId);
            for (RoadGraphBuilder.Edge neighbor : neighbors) {
                String neighborNodeId = neighbor.getEndNodeId();
                // 방문하지 않은 이웃 노드에 대해서만 처리
                if (!visited.contains(neighborNodeId)) {
                    // 현재 노드까지의 실제 비용을 계산
                    double actualCost = actualCosts.get(currentNodeId) + neighbor.getLength();
                    // 이웃 노드까지의 실제 비용이 현재까지 계산된 실제 비용보다 작을 경우
                    if (actualCost < actualCosts.getOrDefault(neighborNodeId, Double.POSITIVE_INFINITY)) {
                        actualCosts.put(neighborNodeId, actualCost);
                        // 이웃 노드까지의 예상 비용을 계산 (실제 비용 + 목표 노드까지의 예상 비용)
                        double estimatedCost = actualCost + calculateHeuristic(graph.getNode(neighborNodeId), targetNode);
                        estimatedCosts.put(neighborNodeId, estimatedCost);
                        // 이전 노드를 갱신
                        previousNodes.put(neighborNodeId, currentNodeId);
                        // 우선순위 큐에 이웃 노드 추가
                        priorityQueue.offer(neighborNodeId);
                    }
                }
            }
        }

        // 최단 경로를 역추적하여 저장
        List<String> shortestPath = new ArrayList<>();
        String currentNode = targetNodeId;
        while (previousNodes.containsKey(currentNode)) {
            shortestPath.add(currentNode);
            currentNode = previousNodes.get(currentNode);
        }
        shortestPath.add(startNodeId);
        Collections.reverse(shortestPath);

        return shortestPath;
    }

    // 휴리스틱 함수: 현재 노드부터 목표 노드까지의 직선 거리
    private double calculateHeuristic(RoadGraphBuilder.Node currentNode, RoadGraphBuilder.Node targetNode) {
        double dx = targetNode.getLatitude() - currentNode.getLatitude();
        double dy = targetNode.getLongitude() - currentNode.getLongitude();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
