package com.example.myapplication;

import java.util.*;

public class AStar {
    List<seoulCctv> cctvList;

    Map<String, Integer> safetyMap;

    public void setCctvList(List<seoulCctv> cctvList) {
        this.cctvList = cctvList;
    }

    public void setSafetyMap(Map<String, Integer> safetyMap) { this.safetyMap = safetyMap; }

    // A* 알고리즘을 이용하여 최단 경로 및 안전 경로 탐색
    public List<String> findShortestAndSafestPath(RoadGraphBuilder.Graph graph, String startNodeId, String targetNodeId) {
        System.out.println("Astar 알고리즘 시작");

        // 시작 노드와 목표 노드의 정보를 가져옴
        RoadGraphBuilder.Node startNode = graph.getNode(startNodeId);

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
            System.out.println("currentNodeId = " + currentNodeId);

            visited.add(currentNodeId);

            // 현재 노드의 이웃 노드들을 가져옴
            List<RoadGraphBuilder.Edge> neighbors = graph.getNeighbors(currentNodeId);
            for (RoadGraphBuilder.Edge neighbor : neighbors) {
                String neighborNodeId;
                if (currentNodeId.equals(neighbor.getStartNodeId())) {
                    neighborNodeId = neighbor.getEndNodeId(); // 현재 노드가 엣지의 시작 노드인 경우
                } else {
                    neighborNodeId = neighbor.getStartNodeId(); // 현재 노드가 엣지의 끝 노드인 경우
                }
                System.out.println("neighborNodeId = " + neighborNodeId);

                // 이웃 노드가 그래프의 노드에 존재하지 않는 경우, 계산 과정을 스킵
                if (!graph.nodes.containsKey(neighborNodeId)) {
                    continue;
                }

                // 방문하지 않은 이웃 노드에 대해서만 처리
                if (!visited.contains(neighborNodeId)) {
                    System.out.println("방문하지 않은 노드 처리 :" + neighborNodeId);
                    // 현재 노드까지의 실제 비용을 계산
                    double actualCost = actualCosts.get(currentNodeId) + neighbor.getLength();

                    // 이웃 노드까지의 실제 비용이 현재까지 계산된 실제 비용보다 작을 경우
                    if (actualCost < actualCosts.getOrDefault(neighborNodeId, Double.POSITIVE_INFINITY)) {
                        actualCosts.put(neighborNodeId, actualCost);

                        // 휴리스틱 함수: 현재 노드부터 목표 노드까지의 직선 거리
                        double heuristic = calculateHeuristic(graph, currentNodeId, targetNodeId);

                        // 안전 관련 가중치 계산
                        double safetyWeight = calculateSafetyWeight(neighborNodeId, safetyMap);

                        // 안전 경로를 고려한 예상 비용 계산
                        double estimatedCost = actualCost + heuristic - (actualCost + heuristic) * safetyWeight;
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
        List<String> shortestAndSafestPath = new ArrayList<>();
        String currentNode = targetNodeId;
        while (previousNodes.containsKey(currentNode)) {
            System.out.println("역추적 진행");
            shortestAndSafestPath.add(currentNode);
            currentNode = previousNodes.get(currentNode);
        }
        shortestAndSafestPath.add(startNodeId);
        Collections.reverse(shortestAndSafestPath);

        return shortestAndSafestPath;
    }

    // 안전 관련 가중치 계산
    private double calculateSafetyWeight(String nodeId, Map<String, Integer> cctvMap) {
        double safetyWeight = 0.0;

        // 노드에 대한 안전장치 개수 가져오기
        int cctvCount = cctvMap.getOrDefault(nodeId, 0);

        // 안전 가중치 계산
        safetyWeight = cctvCount * 0.8;

        return safetyWeight;
    }


    // 휴리스틱 함수 계산: 현재 노드부터 목표 노드까지의 직선 거리
    public double calculateHeuristic(RoadGraphBuilder.Graph graph, String currentId, String targetId) {
        RoadGraphBuilder.Node currentNode = graph.getNode(currentId);
        RoadGraphBuilder.Node targetNode = graph.getNode(targetId);

        double currentLat = currentNode.getLatitude();
        double currentLon = currentNode.getLongitude();
        double targetLat = targetNode.getLatitude();
        double targetLon = targetNode.getLongitude();

        // 위도와 경도를 이용하여 두 지점 간의 거리 계산
        double distance = calculateDistance(currentLat, currentLon, targetLat, targetLon);

        return distance;
    }


    // 두 지점 간의 거리 계산
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (단위: km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }
}
