package com.example.java_tarea_mapa;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    @SerializedName("status")
    public String status;
}

class Route {
    @SerializedName("legs")
    public List<Leg> legs;

    @SerializedName("overview_polyline")
    public OverviewPolyline overviewPolyline;
}

class Leg {
    @SerializedName("distance")
    public Distance distance;

    @SerializedName("duration")
    public Duration duration;
}

class Distance {
    @SerializedName("text")
    public String text;

    @SerializedName("value")
    public int value;
}

class Duration {
    @SerializedName("text")
    public String text;

    @SerializedName("value")
    public int value;
}

class OverviewPolyline {
    @SerializedName("points")
    public String points;
}