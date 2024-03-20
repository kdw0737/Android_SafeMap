package com.example.myapplication;

import static com.skt.Tmap.MapUtils.getDistance;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TMapView tMapView;
    private EditText etStart;
    private EditText etDestination;
    private Button btnStartSearch;
    private Button btnDestinationSearch;
    private Button btnSearchRoute;
    private TMapData tMapData;
    private double startLng;
    private double startLat;
    private double endLng;
    private double endLat;
    private TMapPolyLine tMapPolyLine;
    private boolean isFirstLocation = true; // 최초 위치 플래그
    private static final String TAG = "MainActivity";
    private RoadGraphBuilder.Graph graph;
    private AStar aStar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout frameLayoutTmap = (FrameLayout) findViewById(R.id.frameLayoutTmap);
        tMapView = new TMapView(this);

        tMapView.setSKTMapApiKey("ijsBhAoLqVaMPUYRAssJc5S8iwaicpXuatXwmk6f");
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN); // 언어 설정
        frameLayoutTmap.addView(tMapView);

        // LocationManager와 Criteria 객체 생성
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Button button = findViewById(R.id.my_location_button); // 버튼 객체 생성
        final int LOCATION_PERMISSION_REQUEST_CODE = 1;

        /*GraphBuilderWithIDs graphBuilderWithIDs = new GraphBuilderWithIDs();

        // JSON 파일 읽기
        String nodeJson = graphBuilderWithIDs.loadJSONFromAsset(this, "node.json");
        String edgeJson = graphBuilderWithIDs.loadJSONFromAsset(this, "edge.json");

        if (nodeJson != null && edgeJson != null) {
            // JSON 파싱 및 그래프 생성
            graph = graphBuilderWithIDs.createGraphFromJSON(nodeJson, edgeJson);
            System.out.println("그래프 생성 완료");

            // 그래프 간선 테스트 로그
            System.out.println("Edges:");
            for (GraphBuilderWithIDs.Edge edge : graph.getEdges()) {
                System.out.println("Link ID: " + edge.getLinkId() + ", Length: " + edge.getLength());
                System.out.println("Coordinates:");
                for (GraphBuilderWithIDs.Coordinate coordinate : edge.getCoordinates()) {
                    System.out.println("Latitude: " + coordinate.getLatitude() + ", Longitude: " + coordinate.getLongitude());
                }
            }
            // 각 노드의 좌표 테스트 로그
            System.out.println("\nNodes:");
            for (GraphBuilderWithIDs.Node node : graph.getNodes()) {
                System.out.println("Node ID: " + node.getId() + ", Latitude: " + node.getLatitude() + ", Longitude: " + node.getLongitude());
            }
        } else {
            System.err.println("Failed to load JSON from asset");
        }

        GraphBuilderWithIDs.Node nodeA = graph.getNodes().get(0); // A노드를 가져옴
        List<GraphBuilderWithIDs.Edge> edgesFromNodeA = nodeA.getEdges(); // A노드에 연결된 간선들을 가져옴
        System.out.println("edgesFromNodeA = " + edgesFromNodeA);;

*/

        //Map을 이용해서 그래프 생성
        RoadGraphBuilder roadGraphBuilder = new RoadGraphBuilder();

        //Astar
        aStar = new AStar();

        // JSON 파일 읽기
        String json = roadGraphBuilder.loadJSONFromAsset(this, "sungbook_road.json");

        if (json != null) {
            // JSON 파싱 및 그래프 생성
            graph = roadGraphBuilder.createGraphFromJSON(json);
            Log.d(TAG, "그래프 생성 완료");
/*            // 그래프 간선 테스트 로그
            for (RoadGraphBuilder.Edge edge : graph.getEdges()) {
                Log.d(TAG, "Edge: " + edge.getStartNodeId() + " -> " + edge.getEndNodeId() + ", Length: " + edge.getLength());

                // 엣지에 포함된 좌표들 테스트 출력
                List<RoadGraphBuilder.Node> coordinates = graph.getEdgeCoordinates(edge.getEdgeId());
                Log.d(TAG, "Coordinates Size:" + coordinates.size());
                if (coordinates != null) {
                    for (RoadGraphBuilder.Node coordinate : coordinates) {
                        Log.d(TAG, "Latitude: " + coordinate.getLatitude() + ", Longitude: " + coordinate.getLongitude());
                    }
                } else {
                    Log.d(TAG, "No coordinates found for edge: " + edge.getEdgeId());
                }
            }

            // 각 노드의 좌표 테스트 로그
            for (Map.Entry<String, RoadGraphBuilder.Node> entry : graph.getNodes().entrySet()) {
                RoadGraphBuilder.Node node = entry.getValue();
                Log.d(TAG, "Node: " + node.getNodeId() + ", Latitude: " + node.getLatitude() + ", Longitude: " + node.getLongitude());
            }*/
        } else {
            Log.e(TAG, "Failed to load JSON from asset");
        }


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 권한 요청
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    return;
                }

                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        double lat = location.getLatitude(); // 위도
                        double lng = location.getLongitude(); // 경도

                        // TMapMarkerItem 객체 생성
                        TMapMarkerItem markerItem = new TMapMarkerItem();

                        // 마커 좌표 설정
                        TMapPoint point = new TMapPoint(lat, lng);
                        markerItem.setTMapPoint(point);

                        // 현재 위치 아이콘 표시
                        tMapView.setIconVisibility(true);

                        // 지도를 현재 위치로 이동
                        if (isFirstLocation) {
                            tMapView.setCenterPoint(lng, lat);
                            isFirstLocation = false;
                        }

                        // 내장 마커 이미지로 Bitmap 생성
                        Bitmap bitmap = markerItem.getIcon();

                        // 마커 이미지 설정
                        markerItem.setIcon(bitmap);

                        // 기존 마커 삭제
                        tMapView.removeMarkerItem("marker");

                        // 마커 추가
                        tMapView.addMarkerItem("marker", markerItem);
                    }

                    // ... (다른 LocationListener 메서드 생략)
                };

                String provider = locationManager.getBestProvider(criteria, true);
                Location location = locationManager.getLastKnownLocation(provider);

                if (location != null) {
                    locationListener.onLocationChanged(location);
                } else {
                    // 현재 위치를 얻어올 수 없는 경우 처리 코드 작성
                    System.out.println("현재 위치를 얻어올 수 없습니다.");
                }

                // 위치 업데이트 요청
                locationManager.requestLocationUpdates(provider, 1000, 1, locationListener);
            }
        });


        etStart = findViewById(R.id.et_start);
        btnStartSearch = findViewById(R.id.btn_start_search);
        etDestination = findViewById(R.id.et_destination);
        btnDestinationSearch = findViewById(R.id.btn_destination_search);

        tMapData = new TMapData();

        // 검색 결과를 담을 리스트 생성
        ArrayList<TMapPOIItem> poiItems = new ArrayList<>();

        //출발지 검색버튼을 눌렀을때
        btnStartSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etStart.getText().toString();
                // 기존 검색 결과와 마커를 모두 지웁니다.
                poiItems.clear();
                tMapView.removeAllMarkerItem();

                // TMapPOIItem 을 이용하여 검색어(keyword)를 포함한 장소 검색
                tMapData.findTitlePOI(keyword, new TMapData.FindTitlePOIListenerCallback() {
                    @Override
                    public void onFindTitlePOI(ArrayList<TMapPOIItem> arrayList) {
                        // 검색 결과가 있다면 poiItems 에 추가
                        if (arrayList != null) {
                            poiItems.addAll(arrayList);
                        }

                        // 검색 결과를 지도에 마커로 표시
                        for (int i = 0; i < poiItems.size(); i++) {
                            TMapPOIItem item = poiItems.get(i);
                            TMapPoint point = item.getPOIPoint();
                            TMapMarkerItem markerItem = new TMapMarkerItem();

                            markerItem.setTMapPoint(point);
                            markerItem.setName(item.getPOIName());
                            markerItem.setVisible(TMapMarkerItem.VISIBLE);

                            Bitmap bitmap = markerItem.getIcon(); // 내장된 마커 아이콘을 가져옵니다.
                            markerItem.setIcon(bitmap);

                            setBalloonView(markerItem, item.getPOIName(), item.getPOIAddress(), etStart, true); // 마커에 풍선뷰 설정

                            tMapView.addMarkerItem(item.getPOIName(), markerItem);
                        }

                        // 검색 결과 중 첫 번째 장소로 지도 화면을 이동시킵니다.
                        if (poiItems.size() > 0) {
                            TMapPOIItem firstItem = poiItems.get(0);
                            TMapPoint firstPoint = firstItem.getPOIPoint();
                            tMapView.setCenterPoint(firstPoint.getLongitude(), firstPoint.getLatitude());
                        }
                    }
                });
            }
        });

        //도착지 검색버튼을 눌렀을때
        btnDestinationSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etDestination.getText().toString();

                // 기존 검색 결과와 마커를 모두 지웁니다.
                poiItems.clear();
                tMapView.removeAllMarkerItem();

                // TMapPOIItem 을 이용하여 검색어(keyword)를 포함한 장소 검색
                tMapData.findTitlePOI(keyword, new TMapData.FindTitlePOIListenerCallback() {
                    @Override
                    public void onFindTitlePOI(ArrayList<TMapPOIItem> arrayList) {
                        // 검색 결과가 있다면 poiItems 에 추가
                        if (arrayList != null) {
                            poiItems.addAll(arrayList);
                        }

                        // 검색 결과를 지도에 마커로 표시
                        for (int i = 0; i < poiItems.size(); i++) {
                            TMapPOIItem item = poiItems.get(i);
                            TMapPoint point = item.getPOIPoint();
                            TMapMarkerItem markerItem = new TMapMarkerItem();

                            markerItem.setTMapPoint(point);
                            markerItem.setName(item.getPOIName());
                            markerItem.setVisible(TMapMarkerItem.VISIBLE);

                            Bitmap bitmap = markerItem.getIcon(); // 내장된 마커 아이콘을 가져옵니다.
                            markerItem.setIcon(bitmap);

                            setBalloonView(markerItem, item.getPOIName(), item.getPOIAddress(), etDestination, false); // 마커에 풍선뷰 설정

                            tMapView.addMarkerItem(item.getPOIName(), markerItem);
                        }

                        // 검색 결과 중 첫 번째 장소로 지도 화면을 이동시킵니다.
                        if (poiItems.size() > 0) {
                            TMapPOIItem firstItem = poiItems.get(0);
                            TMapPoint firstPoint = firstItem.getPOIPoint();
                            tMapView.setCenterPoint(firstPoint.getLongitude(), firstPoint.getLatitude());
                        }
                    }
                });
            }
        });

        btnSearchRoute = findViewById(R.id.btn_search_route);

        //경로탐색 검색버튼을 눌렀을 때
        btnSearchRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 기존 마커 삭제
                tMapView.removeAllMarkerItem();

                /*// 경로 탐색 코드
                TMapPoint startPoint = new TMapPoint(startLat, startLng);
                TMapPoint endPoint = new TMapPoint(endLat, endLng);
*/
                TMapData tMapData = new TMapData();

                // 출발지와 도착지에 가장 가까운 노드의 ID를 찾음
                String startNodeId = roadGraphBuilder.findClosestNodeId(graph, startLat, startLng);
                String targetNodeId = roadGraphBuilder.findClosestNodeId(graph, endLat, endLng);

                List<RoadGraphBuilder.Node> adjacentNodes = graph.getAdjacentNodes(startNodeId);

                // 주변 노드 출력
                if (adjacentNodes.isEmpty()) {
                    System.out.println("주변 노드가 없습니다.");
                } else {
                    System.out.println("시작 노드의 주변 노드:");
                    for (RoadGraphBuilder.Node node : adjacentNodes) {
                        System.out.println("Node ID: " + node.getNodeId());
                        System.out.println("Latitude: " + node.getLatitude());
                        System.out.println("Longitude: " + node.getLongitude());
                        System.out.println();
                    }
                }

                // A* 알고리즘을 이용하여 최단 경로 찾기
                List<String> shortestPath = aStar.findShortestPath(graph, startNodeId, targetNodeId);

                // 최단 경로가 없을 경우
                if (shortestPath.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                /*// 출발지와 도착지에 가장 가까운 노드 ID 찾기
                String startNodeId = roadGraphBuilder.findClosestNodeId(graph, startLat, startLng);
                Log.d(TAG, "startNodeId = " + startNodeId);
                String targetNodeId = roadGraphBuilder.findClosestNodeId(graph, endLat, endLng);
                Log.d(TAG, "targetNodeId = " + targetNodeId);

                // A* 알고리즘을 사용하여 최단 경로 탐색
                AStar aStar = new AStar();
                List<String> shortestPath = aStar.findShortestPath(graph, startNodeId, targetNodeId);

                // Tmap Polyline 그리기
                for (int i = 0; i < shortestPath.size() - 1; i++) {
                    RoadGraphBuilder.Node startNode = graph.getNode(shortestPath.get(i));
                    RoadGraphBuilder.Node endNode = graph.getNode(shortestPath.get(i + 1));

                    TMapPolyLine polyline = new TMapPolyLine();
                    polyline.setLineColor(Color.RED);
                    polyline.setLineWidth(2);

                    TMapPoint pathStartPoint = new TMapPoint(startNode.getLatitude(), startNode.getLongitude());
                    TMapPoint pathEndPoint = new TMapPoint(endNode.getLatitude(), endNode.getLongitude());

                    polyline.addLinePoint(pathStartPoint);
                    polyline.addLinePoint(pathEndPoint);

                    tMapView.addTMapPolyLine("path" + i, polyline);
                }*/

//                //보행자 경로 탐색 ( 기존 Tmap 제공 최단거리 경로 탐색 )
//                tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
//                    @Override
//                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
//                        // 보행자 경로 표시
//                        tMapPolyLine.setLineColor(Color.RED);
//                        tMapPolyLine.setLineWidth(5);
//                        tMapView.addTMapPath(tMapPolyLine);
//                        System.out.println("보행자 경로 표시 완료");
//
//                        TMapMarkerItem startMarker = new TMapMarkerItem();
//                        startMarker.setTMapPoint(startPoint);
//                        startMarker.setName("출발지");
//                        startMarker.setVisible(TMapMarkerItem.VISIBLE);
//                        tMapView.addMarkerItem("startMarker", startMarker);
//
//                        TMapMarkerItem endMarker = new TMapMarkerItem();
//                        endMarker.setTMapPoint(endPoint);
//                        endMarker.setName("도착지");
//                        endMarker.setVisible(TMapMarkerItem.VISIBLE);
//                        tMapView.addMarkerItem("endMarker", endMarker);
//
//                        // 출발지, 도착지 중심으로 지도 이동
//                        double minLat = tMapPolyLine.getLinePoint().get(0).getLatitude();
//                        double maxLat = tMapPolyLine.getLinePoint().get(0).getLatitude();
//                        double minLng = tMapPolyLine.getLinePoint().get(0).getLongitude();
//                        double maxLng = tMapPolyLine.getLinePoint().get(0).getLongitude();
//
//                        for (TMapPoint point : tMapPolyLine.getLinePoint()) {
//                            if (minLat > point.getLatitude()) minLat = point.getLatitude();
//                            if (maxLat < point.getLatitude()) maxLat = point.getLatitude();
//                            if (minLng > point.getLongitude()) minLng = point.getLongitude();
//                            if (maxLng < point.getLongitude()) maxLng = point.getLongitude();
//                        }
//
//                        double centerLat = (minLat + maxLat) / 2;
//                        double centerLng = (minLng + maxLng) / 2;
//                        TMapPoint centerPoint = new TMapPoint(centerLat, centerLng);
//                        tMapView.setCenterPoint(centerPoint.getLongitude(), centerPoint.getLatitude());
//
//                        // 축척 조정
//                        TMapInfo mapInfo = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
//                        int zoomLevel = mapInfo.getTMapZoomLevel();
//                        tMapView.setZoomLevel(zoomLevel);
//
//                        // 경로 상의 CCTV 마커 추가
//                        addCCTVMarkersOnPath(tMapPolyLine);
//                    }
//                });
            }
        });
    }

    // 마커에 풍선뷰 설정하는 함수
    private void setBalloonView(TMapMarkerItem marker, String title, String address, EditText text, boolean status) {
        marker.setCanShowCallout(true);

        if (marker.getCanShowCallout()) {
            marker.setCalloutTitle(title);
            marker.setCalloutSubTitle(address);

            RightButtonClickEvent(marker,text, status);
        }
    }

    //마커의 오른쪽버튼을 눌렀을때 발생하는 이벤트 함수
    private void RightButtonClickEvent(TMapMarkerItem marker,EditText text, boolean status){
        // 오른쪽 버튼 이미지 생성
        Bitmap rightButtonImage = BitmapFactory.decodeResource(getResources(), R.drawable.right_button_image);

        // 오른쪽 버튼 클릭 이벤트 설정
        marker.setCalloutRightButtonImage(rightButtonImage);
        tMapView.setOnCalloutRightButtonClickListener(new TMapView.OnCalloutRightButtonClickCallback() {
            @Override
            public void onCalloutRightButton(TMapMarkerItem markerItem) {
                String name = markerItem.getName();
                text.setText(name); // 해당 장소의 이름을 출발지 EditText에 설정


                String address = markerItem.getCalloutSubTitle();

                TMapPoint point = markerItem.getTMapPoint();
                double latitude = point.getLatitude();
                double longitude = point.getLongitude();

                if (status) {
                    startLat = latitude;
                    startLng = longitude;
                    System.out.println("startLat = " + startLat);
                    System.out.println("startLng = " + startLng);
                } else {
                    endLat = latitude; // 출발지 위도 변수에 저장
                    endLng = longitude; // 출발지 경도 변수에 저장
                    System.out.println("endLng = " + endLng);
                    System.out.println("endLat = " + endLat);
                }
                tMapView.setCenterPoint(point.getLongitude(), point.getLatitude()); // 해당 장소로 지도 화면 이동
            }
        });
    }

    private void loadCCTVLocations() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("seoul_cctv");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    seoulCctv list = dataSnapshot.getValue(seoulCctv.class);

                    double lat = list.getWGS84위도();
                    double lng = list.getWGS84경도();
                    int num = list.get번호();

                    // Tmap API를 이용하여 지도에 마커 추가
                    TMapMarkerItem markerItem = new TMapMarkerItem();
                    TMapPoint point = new TMapPoint(lat, lng);
                    markerItem.setTMapPoint(point);
                    markerItem.setName("CCTV " + num);

                    // 마커 아이콘 설정
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.cctv);
                    markerItem.setIcon(icon);

                    tMapView.addMarkerItem("markerItem" + num, markerItem);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "loadCCTVLocations:onCancelled", error.toException());
            }
        });
    }

    // 출발지와 도착지 사이에 있는 노드인지 확인하는 함수
    private boolean isBetween(double startLat, double startLng, double endLat, double endLng,
                              double nodeLat, double nodeLng) {
        // 출발지와 도착지의 위도 경도를 기준으로 사각형 영역 내에 있는지 확인
        return (nodeLat >= Math.min(startLat, endLat) &&
                nodeLat <= Math.max(startLat, endLat) &&
                nodeLng >= Math.min(startLng, endLng) &&
                nodeLng <= Math.max(startLng, endLng));
    }


    private void loadBellLocations() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("seoul_bell");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    seoulBell list = dataSnapshot.getValue(seoulBell.class);

                    double lat = list.getWGS84위도();
                    double lng = list.getWGS84경도();
                    int num = list.get번호();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "loadBellLocations:onCancelled", error.toException());
            }
        });
    }
    private void addCCTVMarkersOnPath(TMapPolyLine path) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("seoul_cctv");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 경로상의 좌표 추출
                List<TMapPoint> points = path.getLinePoint();

                // 추출할 좌표 간격 설정 (미터 단위)
                int interval = 20;

                // 추출된 좌표 주변의 CCTV 마커 추가
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    seoulCctv list = dataSnapshot.getValue(seoulCctv.class);

                    double lat = list.getWGS84위도();
                    double lng = list.getWGS84경도();
                    int num = list.get번호();

                    TMapPoint cctvPoint = new TMapPoint(lat, lng);

                    for (int i = 0; i < points.size() - 1; i++) {
                        TMapPoint start = points.get(i);
                        TMapPoint end = points.get(i + 1);
                        double distance = getDistance(start, end);

                        List<TMapPoint> linePoints = path.getLinePoint();

                        for (int j = 0; j < distance; j += interval) {
                            TMapPoint point = linePoints.get(i);
                            double distanceToCCTV = getDistance(point, cctvPoint);
                            if (distanceToCCTV <= interval) { // 경로로부터 20미터 이내에 존재하는 경우에만 마커 추가
                                TMapMarkerItem markerItem = new TMapMarkerItem();
                                markerItem.setTMapPoint(cctvPoint);
                                markerItem.setName("CCTV " + num);

                                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.cctv);
                                markerItem.setIcon(icon);

                                tMapView.addMarkerItem("markerItem" + num, markerItem);
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "addCCTVMarkersOnPath:onCancelled", error.toException());
            }
        });
    }
}