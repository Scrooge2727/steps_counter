package com.example.stepscounter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

public class WeatherManager {

    private Context context;
    private LocationManager locationManager;
    private WeatherListener weatherListener;

    public WeatherManager(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    public interface WeatherListener {
        void onWeatherDataReceived(String weatherData);

        void onWeatherUpdated(String weatherInfo);
    }
}
