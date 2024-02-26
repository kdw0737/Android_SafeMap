package com.example.myapplication;

public class seoulCctv {
    private double WGS84경도;
    private double WGS84위도;
    private int 번호;

    public seoulCctv() {}

    public seoulCctv(double WGS84경도, double WGS84위도, int 번호) {
        this.WGS84경도 = WGS84경도;
        this.WGS84위도 = WGS84위도;
        this.번호 = 번호;
    }

    public double getWGS84경도() {
        return WGS84경도;
    }

    public void setWGS84경도(double WGS84경도) {
        this.WGS84경도 = WGS84경도;
    }

    public double getWGS84위도() {
        return WGS84위도;
    }

    public void setWGS84위도(double WGS84위도) {
        this.WGS84위도 = WGS84위도;
    }

    public int get번호() {
        return 번호;
    }

    public void set번호(int 번호) {
        this.번호 = 번호;
    }
}
