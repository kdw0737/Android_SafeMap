package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoadGraphBuilder {
    private static final String TAG = "RoadGraphBuilder";

    // Assets 폴더에 있는 JSON 파일 읽기
    public String loadJSONFromAsset(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    // JSON을 기반으로 그래프 생성
    public Graph createGraphFromJSON(String json) {
        Graph graph = new Graph();
        Gson gson = new Gson();
        try {
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String category = jsonObject.optString("CATEGORY");

                if (category.equals("NODE")) {
                    String nodeId = jsonObject.optString("NODE_ID");
                    double latitude = extractLatitudeFromWKT(jsonObject.optString("NODE_WKT"));
                    double longitude = extractLongitudeFromWKT(jsonObject.optString("NODE_WKT"));

                    // 노드 정보를 그래프에 추가
                    graph.addNode(nodeId, latitude, longitude);
                }  else if (category.equals("LINK")) {
                    String edgeId = jsonObject.optString("LINK_ID");
                    String startNodeId = removeDecimal(jsonObject.optString("START_NODE_ID")); // 소수점 제거
                    String endNodeId = removeDecimal(jsonObject.optString("END_NODE_ID")); // 소수점 제거
                    double length = jsonObject.optDouble("LENGTH");
                    String linkWKT = jsonObject.optString("LINK_WKT");

                    List<Node> edgeCoordinates = parseCoordinatesFromWKT(linkWKT);
                    graph.addEdgeCoordinates(edgeId, edgeCoordinates); // 엣지 좌표 추가

                    // 엣지 생성 및 그래프에 추가
                    Edge edge = new Edge(startNodeId, endNodeId, length, edgeId);
                    graph.addEdge(edge);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return graph;
    }

    // WKT 형식의 문자열에서 위도(latitude)를 추출하는 메서드
    private double extractLatitudeFromWKT(String wkt) {
        String[] parts = wkt.split("[\\(\\)]")[1].split("\\s+");
        return Double.parseDouble(parts[1]);
    }

    // WKT 형식의 문자열에서 경도(longitude)를 추출하는 메서드
    private double extractLongitudeFromWKT(String wkt) {
        String[] parts = wkt.split("[\\(\\)]")[1].split("\\s+");
        return Double.parseDouble(parts[0]);
    }

    private List<Node> parseCoordinatesFromWKT(String wkt) {
        List<Node> edgeCoordinates = new ArrayList<>();
        String[] parts = wkt.replace("LINESTRING(", "").replace(")", "").split(",");
        for (String part : parts) {
            String[] coordinates = part.trim().split("\\s*,\\s*");
            for (String coordinate : coordinates) {
                String[] latLng = coordinate.trim().split("\\s+");
                try {
                    double latitude = Double.parseDouble(latLng[1]);
                    double longitude = Double.parseDouble(latLng[0]);
                    edgeCoordinates.add(new Node("", latitude, longitude));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return edgeCoordinates;
    }

    public static class Graph {
        public Map<String, List<Edge>> adjacencyList; // key : NODE_ID , value : 연결된 간선 리스트
        public Map<String, Node> nodes; // key : NODE_ID
        private Map<String, List<Node>> edgeCoordinatesMap; // 엣지의 좌표 맵 key : EDGE_ID

        public Graph() {
            adjacencyList = new HashMap<>();
            nodes = new HashMap<>();
            edgeCoordinatesMap = new HashMap<>();
        }

        public void addEdge(Edge edge) {
            String startNodeId = edge.getStartNodeId();
            String endNodeId = edge.getEndNodeId();

            if (!adjacencyList.containsKey(startNodeId)) {
                adjacencyList.put(startNodeId, new ArrayList<Edge>());
            }
            adjacencyList.get(startNodeId).add(edge);

            if (!adjacencyList.containsKey(endNodeId)) {
                adjacencyList.put(endNodeId, new ArrayList<Edge>());
            }
            adjacencyList.get(endNodeId).add(edge);
        }

        public List<Edge> getEdges() {
            List<Edge> edges = new ArrayList<>();
            for (List<Edge> edgeList : adjacencyList.values()) {
                edges.addAll(edgeList);
            }
            return edges;
        }

        public void addNode(String nodeId, double latitude, double longitude) {
            nodes.put(nodeId, new Node(nodeId, latitude, longitude));
        }

        public Node getNode(String nodeId) {
            return nodes.get(nodeId);
        }

        public Map<String, Node> getNodes() {
            return nodes;
        }

        public List<Edge> getNeighbors(String nodeId) {
            return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
        }

        // 엣지 좌표 맵에 좌표 리스트 추가
        public void addEdgeCoordinates(String edgeId, List<Node> coordinates) {
            edgeCoordinatesMap.put(edgeId, coordinates);
        }

        // 엣지 좌표 맵에서 좌표 리스트 가져오기
        public List<Node> getEdgeCoordinates(String edgeId) {
            return edgeCoordinatesMap.get(edgeId);
        }
    }

    public static class Node {
        private String nodeId;
        private double latitude;
        private double longitude;

        public Node(String nodeId, double latitude, double longitude) {
            this.nodeId = nodeId;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getNodeId() {
            return nodeId;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    public static class Edge {
        private String startNodeId;
        private String endNodeId;
        private double length;
        private String edgeId;

        public Edge(String startNodeId, String endNodeId, double length, String edgeId) {
            this.startNodeId = startNodeId;
            this.endNodeId = endNodeId;
            this.length = length;
            this.edgeId = edgeId;
        }

        public String getStartNodeId() {
            return startNodeId;
        }

        public String getEndNodeId() {
            return endNodeId;
        }

        public double getLength() {
            return length;
        }

        public String getEdgeId() {
            return edgeId;
        }
    }

    // 입력받은 좌표에 가장 가까운 노드의 ID를 반환하는 메서드
    public String findClosestNodeId(Graph graph, double latitude, double longitude) {
        double minDistance = Double.MAX_VALUE;
        String closestNodeId = null;

        for (Map.Entry<String, Node> entry : graph.getNodes().entrySet()) {
            Node node = entry.getValue();
            double distance = calculateDistance(latitude, longitude, node.getLatitude(), node.getLongitude());
            if (distance < minDistance) {
                minDistance = distance;
                closestNodeId = node.getNodeId();
            }
        }

        return closestNodeId;
    }

    // 두 지점 간의 거리를 계산하는 메서드
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344 * 1000; // km to meters
        return dist;
    }

    // 소수점 제거 메서드
    private String removeDecimal(String nodeId) {
        // 소수점 이후의 문자열을 제거하여 소수점을 제거합니다.
        int dotIndex = nodeId.indexOf('.');
        if (dotIndex != -1) {
            return nodeId.substring(0, dotIndex);
        } else {
            return nodeId;
        }
    }
/*
    private int getCCTVCountBetweenNodes(Graph graph, String startNodeId, String endNodeId) {
        // 노드 간의 거리가 10m 이하인 경우
        if (graph.getNode(startNodeId) == null || graph.getNode(endNodeId) == null) {
            System.out.println("getCctvCount메서드 에서 Node가 존재하지 않습니다.");
            System.out.println("startNodeId = " + startNodeId);
            System.out.println("endNodeId = " + endNodeId);
            return 0; // 시작 노드 또는 도착 노드가 존재하지 않는 경우 0 반환
        }

        double edgeLength = getEdgeLength(graph, startNodeId, endNodeId);

        // 노드 간의 거리가 10m 이하인 경우
        if (edgeLength <= 10.0) {
            // 도착 노드를 기준으로만 CCTV 개수를 카운트
            return graph.getNode(endNodeId).getCctvCount();
        } else {
            // 20m 이상인 경우에는 엣지를 적절히 분할하여 각각의 구간에서 CCTV 개수를 계산
            int totalCCTVCount = 0;
            List<Edge> edges = graph.getNeighbors(startNodeId);
            for (Edge edge : edges) {
                String neighborNodeId = edge.getEndNodeId().equals(startNodeId) ? edge.getStartNodeId() : edge.getEndNodeId();
                double divisionLength = edge.getLength() / Math.ceil(edgeLength / 10.0); // 간선을 10m 간격으로 분할
                for (int i = 1; i < Math.ceil(edgeLength / 10.0); i++) {
                    double intermediateLatitude = graph.getNode(startNodeId).getLatitude() +
                            (graph.getNode(neighborNodeId).getLatitude() - graph.getNode(startNodeId).getLatitude()) * i * 10.0 / edgeLength;
                    double intermediateLongitude = graph.getNode(startNodeId).getLongitude() +
                            (graph.getNode(neighborNodeId).getLongitude() - graph.getNode(startNodeId).getLongitude()) * i * 10.0 / edgeLength;
                    // 분할된 지점에서 CCTV 개수 확인
                    if (hasCCTV(graph, intermediateLatitude, intermediateLongitude)) {
                        totalCCTVCount++;
                    }
                }
            }
            return totalCCTVCount;
        }
    }

    // 엣지의 길이를 반환하는 메서드
    private double getEdgeLength(Graph graph, String startNodeId, String endNodeId) {
        List<Edge> edges = graph.getNeighbors(startNodeId);
        for (Edge edge : edges) {
            if ((edge.getStartNodeId().equals(startNodeId) && edge.getEndNodeId().equals(endNodeId)) ||
                    (edge.getStartNodeId().equals(endNodeId) && edge.getEndNodeId().equals(startNodeId))) {
                return edge.getLength();
            }
        }
        return 0.0; // 해당 엣지가 존재하지 않는 경우 0 반환
    }

    // 해당 지점 주변에 10m 이내에 CCTV가 존재하는지 확인하는 메서드
    private boolean hasCCTV(Graph graph, double latitude, double longitude) {
        // 각 노드를 기준으로 10m 이내에 CCTV가 있는지 확인
        for (Map.Entry<String, Node> entry : graph.getNodes().entrySet()) {
            Node node = entry.getValue();
            if (calculateDistance(latitude, longitude, node.getLatitude(), node.getLongitude()) <= 10.0) {
                if (node.getCctvCount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }*/

}
