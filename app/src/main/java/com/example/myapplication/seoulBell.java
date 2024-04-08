package com.example.myapplication;

public class seoulBell {
    private String Longitude;
    private String Latitude;
    private String Num;

    public seoulBell() {}

    public seoulBell(String Longitude, String Latitude, String Num) {
        this.Longitude = Longitude;
        this.Latitude = Latitude;
        this.Num = Num;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        this.Longitude = longitude;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        this.Latitude = latitude;
    }

    public String getNum() {
        return Num;
    }

    public void setNum(String num) {
        this.Num = num;
    }
}
