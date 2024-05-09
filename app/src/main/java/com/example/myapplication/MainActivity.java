package com.example.myapplication;

import static com.skt.Tmap.MapUtils.getDistance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapInfo;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final long MIN_TIME_BETWEEN_UPDATES = 10 * 1000; // 10초
    private static final float MIN_DISTANCE_FOR_UPDATES = 10.0f; // 10미터
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private TMapView tMapView;
    private EditText etStart;
    private EditText etDestination;
    private Button btnStartSearch;
    private Button btnDestinationSearch;
    private Button sirenButton;
    private Button btnSearchRoute;
    private Button myLocation;
    private Button shortestRouteButton;
    private Button safeRouteButton;
    private GestureDetector gestureDetector;
    // 더블클릭 상태를 나타내는 변수
    private boolean doubleClick = false;
    private MediaPlayer mediaPlayer;
    private TMapData tMapData;
    private double startLng;
    private double startLat;
    private double endLng;
    private double endLat;
    private TMapMarkerItem startM;
    private TMapMarkerItem endM;
    private boolean isFirstLocation = true; // 최초 위치 플래그
    private static final String TAG = "MainActivity";
    private RoadGraphBuilder.Graph graph;
    private AStar aStar;
    private List<seoulCctv> cctvList;
    private List<seoulBell> bellList;

    private Map<String, Integer> safetyCountMap;

    private DatabaseReference cctvRef;
    private DatabaseReference bellRef;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout frameLayoutTmap = (FrameLayout) findViewById(R.id.frameLayoutTmap);
        tMapView = new TMapView(this);

        tMapView.setSKTMapApiKey("ijsBhAoLqVaMPUYRAssJc5S8iwaicpXuatXwmk6f");
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN); // 언어 설정
        frameLayoutTmap.addView(tMapView);

        // LocationManager와 Criteria 객체 생성locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Button button = findViewById(R.id.my_location_button); // 버튼 객체 생성
        final int LOCATION_PERMISSION_REQUEST_CODE = 1;

        //Map을 이용해서 그래프 생성
        RoadGraphBuilder roadGraphBuilder = new RoadGraphBuilder();

        // JSON 파일 읽기
        String json = roadGraphBuilder.loadJSONFromAsset(this, "sungbook_road.json");

        if (json != null) {
            // JSON 파싱 및 그래프 생성
            graph = roadGraphBuilder.createGraphFromJSON(json);
            Log.d(TAG, "그래프 생성 완료");
        } else {
            Log.e(TAG, "Failed to load JSON from asset");
        }

        // Firebase 데이터베이스 레퍼런스 설정
        cctvRef = FirebaseDatabase.getInstance().getReference("sungbook_cctv");
        cctvList = new ArrayList<>();

        //cctvList 데이터 생성
        cctvRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 데이터베이스에서 CCTV 데이터를 가져와서 리스트에 저장
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // 데이터 스냅샷으로부터 seoulCctv 객체를 가져와서 cctvList에 추가
                    seoulCctv cctv = new seoulCctv();
                    cctv.setLatitude(dataSnapshot.child("Latitude").getValue().toString().replaceAll("\"", ""));
                    cctv.setLongitude(dataSnapshot.child("Longitude").getValue().toString().replaceAll("\"", ""));
                    cctv.setNum(dataSnapshot.child("Num").getValue().toString().replaceAll("\"", ""));
                    cctvList.add(cctv);
                }

                //노드별 근처 cctv 개수 저장
                safeCountCalculator safeCountCalculator = new safeCountCalculator();
                safetyCountMap = safeCountCalculator.calculateCctvWeights(graph, cctvList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "onCancelled", error.toException());
            }
        });

        // Firebase 데이터베이스 레퍼런스 설정
        bellRef = FirebaseDatabase.getInstance().getReference("sungbook_bell");
        bellList = new ArrayList<>();

        // 데이터 생성
        bellRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 데이터베이스에서 Bell 데이터를 가져와서 리스트에 저장
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // 데이터 스냅샷으로부터 seoulBell 객체를 가져와서 추가
                    seoulBell bell = new seoulBell();
                    bell.setLatitude(dataSnapshot.child("Latitude").getValue().toString().replaceAll("\"", ""));
                    bell.setLongitude(dataSnapshot.child("Longitude").getValue().toString().replaceAll("\"", ""));
                    bell.setNum(dataSnapshot.child("Num").getValue().toString().replaceAll("\"", ""));
                    bellList.add(bell);
                }

                //노드별 근처 bell 개수 저장
                safeCountCalculator safeCountCalculator = new safeCountCalculator();
                safetyCountMap = safeCountCalculator.calculateBellWeights(graph, bellList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "onCancelled", error.toException());
            }
        });

        myLocation = findViewById(R.id.my_location_button);

        // 위치 제공자 얻기
        String provider = locationManager.getBestProvider(criteria, true);

        // 현재 내 위치 표시
        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 위치 제공자 얻기
                String provider = locationManager.getBestProvider(criteria, true);

                if (provider != null) {
                    // 권한 확인
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    }

                    // 현재 위치 가져오기
                    Location location = locationManager.getLastKnownLocation(provider);

                    if (location != null) {
                        // 현재 위치를 지도에 표시하기 위해 경도와 위도 가져오기
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // TMapMarkerItem 객체 생성
                        TMapMarkerItem markerItem = new TMapMarkerItem();

                        // 마커 좌표 설정
                        TMapPoint point = new TMapPoint(latitude, longitude);
                        markerItem.setTMapPoint(point);

                        // 마커 추가
                        tMapView.addMarkerItem("myLocationMarker", markerItem);

                        // 지도 중심을 현재 위치로 이동
                        tMapView.setCenterPoint(longitude, latitude);
                    }

                    // GPS 상태 확인
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        // GPS가 꺼져 있는 경우 알림 다이얼로그 표시
                        showGPSDisabledDialog();
                    } else {
                        // 위치 업데이트 요청
                        locationManager.requestLocationUpdates(provider, MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_FOR_UPDATES, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                // 새로운 위치 업데이트를 수신하면 이 메서드가 호출됩니다.
                                updateCurrentLocation(location);

                                // 위치가 변경될 때마다 지도를 업데이트하여 사용자의 실시간 이동을 표현
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // 현재 위치로 지도 이동
                                tMapView.setCenterPoint(longitude, latitude);
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                                // 위치 제공자가 사용 가능한 경우 호출됩니다.
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                                // 위치 제공자가 사용 불가능한 경우 호출됩니다.
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {
                                // 위치 제공자의 상태가 변경된 경우 호출됩니다.
                            }
                        });
                    }
                } else {
                    // 위치 제공자가 없는 경우 처리 코드 작성 ( gps 설정 화면으로 이동 )
                    showGPSDisabledDialog();
                }
            }
        });


        sirenButton = findViewById(R.id.siren_button);
        // 사이렌 소리 재생을 위한 MediaPlayer 초기화
        mediaPlayer = MediaPlayer.create(this, R.raw.siren);

        // 더블클릭 리스너 설정
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleEmergencySignal(); // 사이렌 소리를 재생하거나 정지합니다.
                return true;
            }
        });

        // 사이렌 버튼에 클릭 리스너 설정
        sirenButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // GestureDetector로 터치 이벤트 전달
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });



        etStart = findViewById(R.id.et_start);
        btnStartSearch = findViewById(R.id.btn_start_search);
        etDestination = findViewById(R.id.et_destination);
        btnDestinationSearch = findViewById(R.id.btn_destination_search);

        tMapData = new TMapData();

        // 검색 결과를 담을 리스트 생성
        ArrayList<TMapPOIItem> poiItems = new ArrayList<>();

        // 출발지 검색버튼을 눌렀을 때
        btnStartSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etStart.getText().toString();
                // 기존 검색 결과를 모두 지웁니다.
                poiItems.clear();

                // 기존 폴리라인 삭제
                tMapView.removeAllTMapPolyLine();

                // 기존의 경로 및 마커 삭제
                if (startM != null && endM != null) {
                    startM.setVisible(TMapMarkerItem.GONE);
                    endM.setVisible(TMapMarkerItem.GONE);
                }
                tMapView.removeTMapPath();
                tMapView.removeAllMarkerItem();


                // TMapPOIItem 을 이용하여 검색어(keyword)를 포함한 장소 검색
                tMapData.findTitlePOI(keyword, new TMapData.FindTitlePOIListenerCallback() {
                    @Override
                    public void onFindTitlePOI(final ArrayList<TMapPOIItem> arrayList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 검색 결과가 없을 경우
                                if (arrayList == null || arrayList.isEmpty()) {
                                    // AlertDialog를 통해 메시지를 표시하고 확인 버튼을 누르면 다이얼로그를 닫습니다.
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("검색 결과가 없습니다.")
                                            .setCancelable(false)
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss(); // 다이얼로그를 닫습니다.
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    // 검색 결과가 있다면 poiItems 에 추가
                                    poiItems.addAll(arrayList);

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
                            }
                        });
                    }
                });
            }
        });


        // 도착지 검색버튼을 눌렀을 때
        btnDestinationSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etDestination.getText().toString();

                // 기존 검색 결과를 모두 지웁니다.
                poiItems.clear();

                // 기존 폴리라인 삭제
                tMapView.removeAllTMapPolyLine();

                // 기존의 경로 및 마커 삭제
                if (startM != null && endM != null) {
                    startM.setVisible(TMapMarkerItem.GONE);
                    endM.setVisible(TMapMarkerItem.GONE);
                }
                tMapView.removeTMapPath();
                tMapView.removeAllMarkerItem();


                // TMapPOIItem 을 이용하여 검색어(keyword)를 포함한 장소 검색
                tMapData.findTitlePOI(keyword, new TMapData.FindTitlePOIListenerCallback() {
                    @Override
                    public void onFindTitlePOI(final ArrayList<TMapPOIItem> arrayList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 검색 결과가 없는 경우
                                if (arrayList == null || arrayList.isEmpty()) {
                                    // AlertDialog를 통해 메시지를 표시하고 확인 버튼을 누르면 다이얼로그를 닫습니다.
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("검색 결과가 없습니다.")
                                            .setCancelable(false)
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss(); // 다이얼로그를 닫습니다.
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    // 검색 결과가 있다면 poiItems 에 추가
                                    poiItems.addAll(arrayList);

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
                            }
                        });
                    }
                });
            }
        });

        btnSearchRoute = findViewById(R.id.btn_search_route);

        // 최단경로 버튼 초기화
        shortestRouteButton = findViewById(R.id.btn_shortest_route);

        //안전경로 버튼 초기화
        safeRouteButton = findViewById(R.id.btn_safety_route);

        // 경로탐색 검색버튼을 눌렀을 때
        btnSearchRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 경로 탐색 버튼 클릭 시 안전경로 및 최단경로 버튼을 보이도록 설정
                LinearLayout bottomButtonsLayout = findViewById(R.id.bottom_buttons_layout);
                bottomButtonsLayout.setVisibility(View.VISIBLE);

                // 안전경로 버튼을 선택한 상태로 설정
                Button safetyRouteButton = findViewById(R.id.btn_safety_route);
                safetyRouteButton.setSelected(true);

                // 최단경로 버튼은 선택되지 않은 상태로 설정
                Button shortestRouteButton = findViewById(R.id.btn_shortest_route);
                shortestRouteButton.setSelected(false);

                // 기존 폴리라인 삭제
                tMapView.removeAllTMapPolyLine();

                // 기존 마커 삭제
                tMapView.removeAllMarkerItem();

                // 출발지와 도착지에 가장 가까운 노드의 ID를 찾음
                String startNodeId = roadGraphBuilder.findClosestNodeId(graph, startLat, startLng);
                String targetNodeId = roadGraphBuilder.findClosestNodeId(graph, endLat, endLng);

                // 출발지에 마커 추가
                TMapPoint startPoint = new TMapPoint(graph.nodes.get(startNodeId).getLatitude(), graph.nodes.get(startNodeId).getLongitude());
                addMarker(startPoint, "출발지");

                // 도착지에 마커 추가
                TMapPoint endPoint = new TMapPoint(graph.nodes.get(targetNodeId).getLatitude(), graph.nodes.get(targetNodeId).getLongitude());
                addMarker(endPoint, "도착지");

                // 경로 탐색 및 Polyline 그리기 메소드 호출
                searchAndDrawRoute(startNodeId, targetNodeId);
            }
        });

        // 안전경로 버튼 클릭 시
        safeRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //기존의 경로 및 마커 삭제
                startM.setVisible(TMapMarkerItem.GONE);
                endM.setVisible(TMapMarkerItem.GONE);
                tMapView.removeTMapPath();
                tMapView.removeAllMarkerItem();

                // 출발지와 도착지에 가장 가까운 노드의 ID를 찾음
                String startNodeId = roadGraphBuilder.findClosestNodeId(graph, startLat, startLng);
                String targetNodeId = roadGraphBuilder.findClosestNodeId(graph, endLat, endLng);

                // 출발지에 마커 추가
                TMapPoint startPoint = new TMapPoint(graph.nodes.get(startNodeId).getLatitude(), graph.nodes.get(startNodeId).getLongitude());
                addMarker(startPoint, "출발지");

                // 도착지에 마커 추가
                TMapPoint endPoint = new TMapPoint(graph.nodes.get(targetNodeId).getLatitude(), graph.nodes.get(targetNodeId).getLongitude());
                addMarker(endPoint, "도착지");

                // 안전경로 버튼을 선택한 상태로 설정
                Button safetyRouteButton = findViewById(R.id.btn_safety_route);
                safetyRouteButton.setSelected(true);

                // 최단경로 버튼은 선택되지 않은 상태로 설정
                Button shortestRouteButton = findViewById(R.id.btn_shortest_route);
                shortestRouteButton.setSelected(false);

                // 경로 탐색 및 Polyline 그리기 메소드 호출
                searchAndDrawRoute(startNodeId, targetNodeId);
            }
        });

        // 최단경로 버튼 클릭 시
        shortestRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 최단경로 버튼 클릭 시 안전경로 버튼 선택 해제
                Button safetyRouteButton = findViewById(R.id.btn_safety_route);
                safetyRouteButton.setSelected(false);

                // 최단경로 버튼 선택 상태로 변경
                shortestRouteButton.setSelected(true);

                // 기존 폴리라인 삭제
                tMapView.removeAllTMapPolyLine();

                // 기존 마커 삭제
                tMapView.removeAllMarkerItem();

                // 보행자 경로 탐색 ( 기존 Tmap 제공 최단거리 경로 탐색 )
                TMapData tMapData = new TMapData();
                TMapPoint startPoint = new TMapPoint(startLat, startLng);
                TMapPoint endPoint = new TMapPoint(endLat, endLng);
                tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        // 보행자 경로 표시
                        tMapPolyLine.setLineColor(Color.RED);
                        tMapPolyLine.setLineWidth(5);
                        tMapView.addTMapPath(tMapPolyLine);
                        System.out.println("보행자 경로 표시 완료");

                        // 출발지, 도착지 마커 표시
                        TMapMarkerItem startMarker = new TMapMarkerItem();
                        startMarker.setTMapPoint(startPoint);
                        startMarker.setName("출발지");
                        startM = startMarker;
                        startMarker.setVisible(TMapMarkerItem.VISIBLE);
                        tMapView.addMarkerItem("startMarker", startMarker);

                        TMapMarkerItem endMarker = new TMapMarkerItem();
                        endMarker.setTMapPoint(endPoint);
                        endMarker.setName("도착지");
                        endM = endMarker;
                        endMarker.setVisible(TMapMarkerItem.VISIBLE);
                        tMapView.addMarkerItem("endMarker", endMarker);

                        // 출발지, 도착지 중심으로 지도 이동
                        double minLat = tMapPolyLine.getLinePoint().get(0).getLatitude();
                        double maxLat = tMapPolyLine.getLinePoint().get(0).getLatitude();
                        double minLng = tMapPolyLine.getLinePoint().get(0).getLongitude();
                        double maxLng = tMapPolyLine.getLinePoint().get(0).getLongitude();

                        for (TMapPoint point : tMapPolyLine.getLinePoint()) {
                            if (minLat > point.getLatitude()) minLat = point.getLatitude();
                            if (maxLat < point.getLatitude()) maxLat = point.getLatitude();
                            if (minLng > point.getLongitude()) minLng = point.getLongitude();
                            if (maxLng < point.getLongitude()) maxLng = point.getLongitude();
                        }

                        double centerLat = (minLat + maxLat) / 2;
                        double centerLng = (minLng + maxLng) / 2;
                        TMapPoint centerPoint = new TMapPoint(centerLat, centerLng);
                        tMapView.setCenterPoint(centerPoint.getLongitude(), centerPoint.getLatitude());

                        // 축척 조정
                        TMapInfo mapInfo = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePoint());
                        int zoomLevel = mapInfo.getTMapZoomLevel();
                        tMapView.setZoomLevel(zoomLevel);

                        // 경로 상의 CCTV 마커 추가
                        addCCTVMarkersOnPath(tMapPolyLine, cctvRef);
                    }
                });
            }
        });
    }


    // 경로 탐색 및 그리기
    private void searchAndDrawRoute(String startNodeId, String targetNodeId) {
        TMapData tMapData = new TMapData();
        aStar = new AStar();
        aStar.setCctvList(cctvList);
        aStar.setSafetyMap(safetyCountMap);

        try {
            // A* 알고리즘을 이용하여 최단 경로 찾기
            List<String> shortestPath = aStar.findShortestAndSafestPath(graph, startNodeId, targetNodeId);
            TMapPolyLine polyline = new TMapPolyLine();
            // Tmap Polyline 그리기
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                RoadGraphBuilder.Node startNode = graph.getNode(shortestPath.get(i));
                RoadGraphBuilder.Node endNode = graph.getNode(shortestPath.get(i + 1));

                polyline.setLineColor(Color.RED);
                polyline.setLineWidth(5);

                TMapPoint pathStartPoint = new TMapPoint(startNode.getLatitude(), startNode.getLongitude());
                TMapPoint pathEndPoint = new TMapPoint(endNode.getLatitude(), endNode.getLongitude());

                polyline.addLinePoint(pathStartPoint);
                polyline.addLinePoint(pathEndPoint);

                tMapView.addTMapPolyLine("path" + i, polyline);
            }

            // 출발지와 도착지가 모두 보이도록 축척 조정
            adjustZoomToRoute(shortestPath);

            //cctv 마커 추가
            addCCTVMarkersOnPath(polyline, cctvRef);

            //bell 마커 추가
            addBellMarkersOnPath(polyline, bellRef);
        } catch (Exception e) {
            e.printStackTrace();
            // 예외가 발생한 경우 AlertDialog 표시
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Failed to find route.")
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    // 출발지와 도착지가 모두 보이도록 축척을 조정하는 메서드
    private void adjustZoomToRoute(List<String> shortestPath) {
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (String nodeId : shortestPath) {
            RoadGraphBuilder.Node node = graph.getNode(nodeId);
            double lat = node.getLatitude();
            double lng = node.getLongitude();

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLng = Math.min(minLng, lng);
            maxLng = Math.max(maxLng, lng);
        }

        double centerLat = (minLat + maxLat) / 2;
        double centerLng = (minLng + maxLng) / 2;

        tMapView.setCenterPoint(centerLng, centerLat);
        tMapView.zoomToSpan(maxLng - minLng, maxLat - minLat);
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

    //경로에 존재하는 cctv를 지도상에 표시
    private void addCCTVMarkersOnPath(TMapPolyLine path, DatabaseReference ref) {
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

                    String lat = list.getLatitude();
                    String lng = list.getLongitude();
                    String num = list.getNum();

                    TMapPoint cctvPoint = new TMapPoint(Double.parseDouble(lat), Double.parseDouble(lng));

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

    //경로에 존재하는 bell을 지도상에 표시
    private void addBellMarkersOnPath(TMapPolyLine path, DatabaseReference ref) {
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

                    String lat = list.getLatitude();
                    String lng = list.getLongitude();
                    String num = list.getNum();

                    TMapPoint cctvPoint = new TMapPoint(Double.parseDouble(lat), Double.parseDouble(lng));

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
                                markerItem.setName("Bell " + num);

                                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.bell);
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

    // 출발지 마커 추가 메소드
    private void addStartMarker(double latitude, double longitude) {
        TMapMarkerItem startMarker = new TMapMarkerItem();
        TMapPoint startPoint = new TMapPoint(latitude, longitude);
        startMarker.setTMapPoint(startPoint);
        startMarker.setName("출발지");
        startMarker.setVisible(TMapMarkerItem.VISIBLE);
        tMapView.addMarkerItem("startMarker", startMarker);
    }

    // 도착지 마커 추가 메소드
    private void addEndMarker(double latitude, double longitude) {
        TMapMarkerItem endMarker = new TMapMarkerItem();
        TMapPoint endPoint = new TMapPoint(latitude, longitude);
        endMarker.setTMapPoint(endPoint);
        endMarker.setName("도착지");
        endMarker.setVisible(TMapMarkerItem.VISIBLE);
        tMapView.addMarkerItem("endMarker", endMarker);
    }

    // 마커 추가 메소드
    private void addMarker(TMapPoint point, String name) {
        TMapMarkerItem marker = new TMapMarkerItem();
        marker.setTMapPoint(point);
        marker.setName(name);
        marker.setVisible(TMapMarkerItem.VISIBLE);
        tMapView.addMarkerItem(name, marker);
    }

    // 사이렌 소리를 재생하거나 정지하는 메서드
    private void toggleEmergencySignal() {
        if (!doubleClick) {
            // 사이렌 소리 재생
            if (mediaPlayer != null) {
                mediaPlayer.start();
                Toast.makeText(MainActivity.this, "사이렌이 울립니다!", Toast.LENGTH_SHORT).show();
                doubleClick = true;
            }
        } else {
            // 사이렌 소리 정지
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0); // 재생 위치를 처음으로 되돌립니다.
                Toast.makeText(MainActivity.this, "사이렌이 정지되었습니다.", Toast.LENGTH_SHORT).show();
                doubleClick = false;
            }
        }
    }
    // updateCurrentLocation 메서드 정의
    private void updateCurrentLocation(Location location) {
        // 현재 위치를 받아와서 지도에 표시하는 코드 작성
        if (location != null) {
            // 현재 위치를 지도에 표시하기 위해 경도와 위도 가져오기
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            // TMapMarkerItem 객체 생성
            TMapMarkerItem markerItem = new TMapMarkerItem();

            // 마커 좌표 설정
            TMapPoint point = new TMapPoint(latitude, longitude);
            markerItem.setTMapPoint(point);

            // 마커 추가
            tMapView.addMarkerItem("myLocationMarker", markerItem);

            // 지도 중심을 현재 위치로 이동
            tMapView.setCenterPoint(longitude, latitude);
        } else {
            // 현재 위치를 얻어올 수 없는 경우 처리 코드 작성
            Toast.makeText(MainActivity.this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // GPS가 꺼져 있는 경우 알림 다이얼로그 표시
    private void showGPSDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("GPS가 꺼져 있습니다. GPS를 켜시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // GPS 설정 화면으로 이동
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}