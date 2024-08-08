package com.example.stepscounter;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {
    @SerializedName("coord")
    private Coord coord;

    @SerializedName("weather")
    private List<Weather> weatherList;

    @SerializedName("main")
    private Main main;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("name")
    private String name;

    @SerializedName("rain")
    private Rain rain; // Добавляем информацию о дожде

    @SerializedName("visibility")
    private int visibility;

    public int getVisibility() {
        return visibility;
    }
    public Coord getCoord() {
        return coord;
    }

    public List<Weather> getWeatherList() {
        return weatherList;
    }

    public Main getMain() {
        return main;
    }

    public Wind getWind() {
        return wind;
    }
    public Rain getRain() {
        return rain;
    }

    public String getName() {
        return name;
    }

    public static class Coord {
        @SerializedName("lat")
        private double lat;

        @SerializedName("lon")
        private double lon;

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }
    }

    public static class Weather {
        @SerializedName("description")
        private String description;

        @SerializedName("main")
        private String main;
        public String getDescription() {
            return description;
        }
        public String getMain() {
            return main;
        }
    }
    public static class Rain {
        @SerializedName("3h")
        private double volume; // Объём осадков за последние 3 часа (можете использовать другое название поля в зависимости от API)

        public double getVolume() {
            return volume;
        }
    }
    public static class Main {
        @SerializedName("temp")
        private double temp;

        public double getTemp() {
            return temp;
        }
    }

    public static class Wind {
        @SerializedName("speed")
        private double speed;

        public double getSpeed() {
            return speed;
        }
    }
}
