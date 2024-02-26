package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import static com.skt.Tmap.MapUtils.getDistance;

import java.util.ArrayList;
import java.util.List;

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

                // 경로 탐색 코드
                TMapPoint startPoint = new TMapPoint(startLat, startLng);
                TMapPoint endPoint = new TMapPoint(endLat, endLng);

                TMapPoint[][] map = getMapData(startPoint,endPoint);  // 지도 데이터를 가져오는 메서드를 구현
                AStarPathFinder pathFinder = new AStarPathFinder(map, startPoint,endPoint);
                List<Node> path = pathFinder.findPath();

                if (path != null) {
                    // 경로가 존재하는 경우

                    // 보행자 경로 표시
                    TMapPolyLine tMapPolyLine = new TMapPolyLine();
                    for (Node node : path) {
                        TMapPoint point = new TMapPoint(node.getX(), node.getY());
                        tMapPolyLine.addLinePoint(point);
                    }
                    tMapPolyLine.setLineColor(Color.BLUE);
                    tMapPolyLine.setLineWidth(5);
                    tMapView.addTMapPath(tMapPolyLine);

                    // 출발지, 도착지 마커 생성
                    TMapMarkerItem startMarker = new TMapMarkerItem();
                    startMarker.setTMapPoint(startPoint);
                    startMarker.setName("출발지");
                    startMarker.setVisible(TMapMarkerItem.VISIBLE);
                    tMapView.addMarkerItem("startMarker", startMarker);

                    TMapMarkerItem endMarker = new TMapMarkerItem();
                    endMarker.setTMapPoint(endPoint);
                    endMarker.setName("도착지");
                    endMarker.setVisible(TMapMarkerItem.VISIBLE);
                    tMapView.addMarkerItem("endMarker", endMarker);

                    // 출발지, 도착지 중심으로 지도 이동
                    double minLat = startPoint.getLatitude();
                    double maxLat = startPoint.getLatitude();
                    double minLng = startPoint.getLongitude();
                    double maxLng = startPoint.getLongitude();

                    for (Node node : path) {
                        TMapPoint point = new TMapPoint(node.getX(), node.getY());
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
                    addCCTVMarkersOnPath(tMapPolyLine);
                } else {
                    // 경로가 존재하지 않는 경우에 대한 처리
                    System.out.println(" 경로가 존재하지 않음 . Error");
                }

                TMapData tMapData = new TMapData();

                //보행자 경로 탐색
                tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        // 보행자 경로 표시
                        tMapPolyLine.setLineColor(Color.RED);
                        tMapPolyLine.setLineWidth(5);
                        tMapView.addTMapPath(tMapPolyLine);

                        /* 출발지, 도착지 마커 생성
                        TMapMarkerItem startMarker = new TMapMarkerItem();
                        startMarker.setTMapPoint(startPoint);
                        startMarker.setName("출발지");
                        startMarker.setVisible(TMapMarkerItem.VISIBLE);
                        tMapView.addMarkerItem("startMarker", startMarker);

                        TMapMarkerItem endMarker = new TMapMarkerItem();
                        endMarker.setTMapPoint(endPoint);
                        endMarker.setName("도착지");
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
                        tMapView.setZoomLevel(zoomLevel); */

                        // 경로 상의 CCTV 마커 추가
                        addCCTVMarkersOnPath(tMapPolyLine);
                    }
                });
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

    // 출발지와 도착지를 매개변수로 받아서 지도 데이터를 생성하는 getMapData() 메서드
    private TMapPoint[][] getMapData(TMapPoint startPoint, TMapPoint endPoint) {
        // 출발지와 도착지의 좌표를 얻어옴
        double startX = startPoint.getLatitude();
        double startY = startPoint.getLongitude();
        double endX = endPoint.getLatitude();
        double endY = endPoint.getLongitude();

        // 출발지와 도착지 간의 가로, 세로 거리 계산
        double distanceX = Math.abs(endX - startX);
        double distanceY = Math.abs(endY - startY);

        // 지도 데이터를 구성하는 그리드 크기 설정
        double gridSize = 0.0005; // 임의로 설정한 그리드 크기

        // 가로와 세로 그리드 개수 계산
        int gridCountX = (int) (distanceX / gridSize) + 1;
        int gridCountY = (int) (distanceY / gridSize) + 1;

        // TMapPoint 배열을 생성하여 초기화
        TMapPoint[][] mapData = new TMapPoint[gridCountY + 1][gridCountX + 1];

        // 지도 데이터 구성
        for (int y = 0; y <= gridCountY; y++) {
            for (int x = 0; x <= gridCountX; x++) {
                // 각 좌표에 해당하는 TMapPoint 객체 생성
                double latitude = startY + (endY - startY) * y / gridCountY;
                double longitude = startX + (endX - startX) * x / gridCountX;
                mapData[y][x] = new TMapPoint(latitude, longitude);
            }
        }
        return mapData;
    }
}