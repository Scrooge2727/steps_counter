// OpenWeatherApi.java
package com.example.stepscounter;

import com.example.stepscounter.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherApi {
    String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    String OPEN_WEATHER_API_KEY = "339dc348b3110231629d658ddd448c02";
    @GET("weather")
    Call<WeatherResponse> getWeather(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}
