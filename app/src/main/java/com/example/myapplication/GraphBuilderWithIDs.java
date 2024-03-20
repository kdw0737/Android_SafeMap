package com.example.myapplication;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBuilderWithIDs {
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

    // JSON을 기반으로 그래프 생성하는 메서드
    public static Graph createGraphFromJSON(String nodeJson, String edgeJson) {
        Graph graph = new Graph();
        Map<Integer, Node> nodeMap = new HashMap<>();
        Map<String, Edge> edgeMap = new HashMap<>();

        try {
            JSONArray nodeArray = new JSONArray(nodeJson);
            JSONArray edgeArray = new JSONArray(edgeJson);

            // 노드 추가
            for (int i = 0; i < nodeArray.length(); i++) {
                JSONObject nodeObject = nodeArray.getJSONObject(i);
                String geometry = nodeObject.getString("geometry");
                // 좌표 문자열 파싱
                String[] parts = geometry.split("[(),\\s]+"); // "(", ")", " "을 기준으로 분리
                double latitude = Double.parseDouble(parts[2]);
                double longitude = Double.parseDouble(parts[1]);
                int nodeId = i + 1; // 노드 번호 부여
                Node node = new Node(nodeId, latitude, longitude);
                graph.addNode(node);
                nodeMap.put(nodeId, node);
            }

            // 간선 추가
            int linkIdCounter = 1; // LinkId를 1부터 시작하기 위한 카운터 변수
            for (int i = 0; i < edgeArray.length(); i++) {
                JSONObject edgeObject = edgeArray.getJSONObject(i);
                String geometry = edgeObject.getString("geometry");
                double length = edgeObject.getDouble("length");

                // LinkId를 카운터 변수 값으로 지정하고 카운터를 증가시킴
                String linkId = String.valueOf(linkIdCounter++);

                List<Coordinate> coordinates = parseCoordinates(geometry);
                Edge edge = new Edge(linkId, geometry, length);
                graph.addEdge(edge);
                edgeMap.put(linkId, edge);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return graph;
    }

    // parseCoordinates 메서드 수정
    public static List<Coordinate> parseCoordinates(String geometry) {
        List<Coordinate> coordinates = new ArrayList<>();
        try {
            String[] coordinateStrings = geometry.split("\\s*\\(\\s*")[1].split("\\s*,\\s*|\\s*\\)\\s*");
            for (String coordinateString : coordinateStrings) {
                String[] parts = coordinateString.trim().split("\\s+"); // 좌표값에서 공백 제거 후 분리
                double longitude = Double.parseDouble(parts[0]);
                double latitude = Double.parseDouble(parts[1]);
                coordinates.add(new Coordinate(latitude, longitude));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    // 그래프 클래스
    static class Graph {
        private List<Node> nodes;
        private List<Edge> edges;

        public Graph() {
            nodes = new ArrayList<>();
            edges = new ArrayList<>();
        }

        public void addNode(Node node) {
            nodes.add(node);
        }

        public void addEdge(Edge edge) {
            edges.add(edge);
            // 해당 간선의 출발 노드에 연결된 간선 리스트에 추가
//            edge.getStartNode().addEdge(edge);
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        @Override
        public String toString() {
            return "Graph{" +
                    "nodes=" + nodes +
                    ", edges=" + edges +
                    '}';
        }
    }

    // 노드 클래스
    static class Node {
        private int id;
        private double latitude;
        private double longitude;
        private List<Edge> edges; // 노드에 연결된 간선 리스트

        public Node(int id, double latitude, double longitude) {
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
            this.edges = new ArrayList<>(); // 간선 리스트 초기화
        }

        // 노드에 간선 추가하는 메서드
        public void addEdge(Edge edge) {
            edges.add(edge);
        }

        // 노드에 연결된 간선 리스트 반환하는 메서드
        public List<Edge> getEdges() {
            return edges;
        }

        public int getId() {
            return id;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "id=" + id +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }
    }


    // 간선 클래스
    public static class Edge {
        private String linkId;
        private String geometry;
        private double length;

        public Edge(String linkId, String geometry, double length) {
            this.linkId = linkId;
            this.geometry = geometry;
            this.length = length;
        }

        public String getLinkId() {
            return linkId;
        }

        public String getGeometry() {
            return geometry;
        }

        public double getLength() {
            return length;
        }

        public List<Coordinate> getCoordinates() {
            return parseCoordinates(geometry);
        }
    }

    // 좌표 클래스
    public static class Coordinate {
        private double latitude;
        private double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
