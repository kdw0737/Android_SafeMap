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
                } else if (category.equals("LINK")) {
                    String edgeId = jsonObject.optString("LINK_ID");
                    String startNodeId = jsonObject.optString("START_NODE_ID");
                    String endNodeId = jsonObject.optString("END_NODE_ID");
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
        public Map<String, List<Edge>> adjacencyList;
        private Map<String, Node> nodes;
        private Map<String, List<Node>> edgeCoordinatesMap; // 엣지의 좌표 맵

        public Graph() {
            adjacencyList = new HashMap<>();
            nodes = new HashMap<>();
            edgeCoordinatesMap = new HashMap<>();
        }

        public void addEdge(Edge edge) {
            String startNodeId = edge.getStartNodeId();
            String endNodeId = edge.getEndNodeId();

            if (!adjacencyList.containsKey(startNodeId)) {
                adjacencyList.put(startNodeId, new ArrayList<>());
            }
            adjacencyList.get(startNodeId).add(edge);

            if (!adjacencyList.containsKey(endNodeId)) {
                adjacencyList.put(endNodeId, new ArrayList<>());
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

        public List<Node> getAdjacentNodes(String nodeId) {
            List<Node> adjacentNodes = new ArrayList<>();
            List<Edge> edges = adjacencyList.get(nodeId);
            for (int i = 0; i < edges.size(); i++) {
                System.out.println("edges = " + edges.get(i));
            }
            if (edges != null) {
                for (Edge edge : edges) {
                    String adjacentNodeId;
                    if (edge.getStartNodeId().equals(nodeId)) {
                        adjacentNodeId = edge.getEndNodeId();
                    } else {
                        adjacentNodeId = edge.getStartNodeId();
                    }
                    Node adjacentNode = nodes.get(adjacentNodeId);
                    if (adjacentNode != null) {
                        adjacentNodes.add(adjacentNode);
                    }
                }
            } else {
                Log.d(TAG, "edges가 null값입니다.");
            }

            return adjacentNodes;
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
}
