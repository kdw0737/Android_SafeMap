package com.example.myapplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class safeCountCalculator {

    // Map을 사용하여 이미 계산된 노드와 CCTV 간의 거리를 저장
    private Map<String, Integer> distanceMap = new HashMap<>(); //key : nodeId , value : cctv 개수 + 비상벨 개수

    // 안전 관련 가중치 계산 (cctv)
    public Map<String, Integer> calculateCctvWeights(RoadGraphBuilder.Graph graph, List<seoulCctv> cctvList) {
        // 모든 노드에 대해 반복하여 cctv, bell 개수 계산
        for (RoadGraphBuilder.Node node : graph.getNodes().values()) {
            String nodeId = node.getNodeId();
            int cctvCount = calculateCctvCount(node, cctvList);
            distanceMap.put(nodeId, cctvCount);
        }
        return distanceMap;
    }

    // 안전 관련 가중치 계산 (bell)
    public Map<String, Integer> calculateBellWeights(RoadGraphBuilder.Graph graph, List<seoulBell> bellList) {
        // 모든 노드에 대해 반복하여 cctv, bell 개수 계산
        for (RoadGraphBuilder.Node node : graph.getNodes().values()) {
            String nodeId = node.getNodeId();
            int bellCount = calculateBellCount(node, bellList);
            // 기존에 저장된 값이 있는지 확인하고 없으면 초기값인 0으로 설정
            int currentCount = distanceMap.getOrDefault(nodeId, 0);

            // 기존 값에 bellCount를 더하여 업데이트
            distanceMap.put(nodeId, currentCount + bellCount);
        }
        return distanceMap;
    }


    // 노드 주변의 20m 반경 내에 존재하는 cctv 개수 계산
    private int calculateCctvCount(RoadGraphBuilder.Node node, List<seoulCctv> cctvList) {
        double nodeLat = node.getLatitude();
        double nodeLng = node.getLongitude();
        int count = 0;

        // 모든 cctv에 대해 반복하여 20m 반경 내에 존재하는지 확인
        for (seoulCctv cctv : cctvList) {
            double cctvLat = Double.parseDouble(cctv.getLatitude());
            double cctvLng = Double.parseDouble(cctv.getLongitude());
            double distance = calculateDistance(nodeLat, nodeLng, cctvLat, cctvLng);

            if (distance <= 20) { // 20m 반경 내에 존재하는 경우
                count++;
            }
        }
        return count;
    }

    // 노드 주변의 20m 반경 내에 존재하는 bell 개수 계산
    private int calculateBellCount(RoadGraphBuilder.Node node, List<seoulBell> bellList) {
        double nodeLat = node.getLatitude();
        double nodeLng = node.getLongitude();
        int count = 0;

        // 모든 cctv에 대해 반복하여 20m 반경 내에 존재하는지 확인
        for (seoulBell bell : bellList) {
            double bellLat = Double.parseDouble(bell.getLatitude());
            double bellLng = Double.parseDouble(bell.getLongitude());
            double distance = calculateDistance(nodeLat, nodeLng, bellLat, bellLng);

            if (distance <= 20) { // 20m 반경 내에 존재하는 경우
                count++;
            }
        }
        return count;
    }

    // 두 지점 간의 거리를 계산하는 메서드
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (단위: km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance * 1000;
    }
}