package com.example.myapplication;

public class seoulCctv {
    private String Latitude;
    private String Longitude;
    private String Num;

    public seoulCctv() {
    }

    public seoulCctv(String latitude, String longitude, String num) {
        Latitude = latitude;
        Longitude = longitude;
        Num = num;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getNum() {
        return Num;
    }

    public void setNum(String num) {
        Num = num;
    }
}
