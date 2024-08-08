package com.example.stepscounter;

import static com.example.stepscounter.StepResetScheduler.scheduleStepReset;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements SensorEventListener, WeatherManager.WeatherListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int EXACT_ALARM_PERMISSION_REQUEST_CODE = 2;
    private static final int POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE = 3;

    private SensorManager mSensorManager = null;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private int previousTotalSteps = 0;
    private long startTime = 0;
    private long endTime;
    private ProgressBar progressBar;
    private TextView steps;
    private TextView locationAndTemperature;
    private TextView loadingText;
    private TextView speedSteps;
    private LocationManager locationManager;
    private long lastStepTime;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final long CHECK_INTERVAL = 1000; // Интервал проверки в миллисекундах
    private static final long TIMEOUT_INTERVAL = 5000; // Интервал времени без шагов для сброса скорости
    private int stepCountInInterval = 0; // Количество шагов за интервал
    private List<Long> stepTimestamps = new ArrayList<>();
    private WeatherManager weatherManager;
    // Добавляем статическое поле
    private static MainActivity instance;

    // Добавляем статический метод для получения экземпляра MainActivity
    // Добавляем статический метод для получения экземпляра MainActivity
    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        progressBar = findViewById(R.id.progressBar);
        steps = findViewById(R.id.steps);
        locationAndTemperature = findViewById(R.id.locationAndTemperature);
        speedSteps = findViewById(R.id.speedSteps);
        loadingText = findViewById(R.id.loadingText);
        loadingText.setVisibility(View.VISIBLE); // Показываем сообщение о загрузке

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canScheduleExactAlarms()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, EXACT_ALARM_PERMISSION_REQUEST_CODE);
            } else {
                scheduleStepReset(this);  // Передаем context явно
            }
        } else {
            scheduleStepReset(this);  // Передаем context явно
        }

        NotificationHelper.createNotificationChannel(this);

        resetSteps();
        loadData();
        startSpeedCheck();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        weatherManager = new WeatherManager(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }
    private void startSpeedCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (stepCountInInterval > 0 && currentTime - lastStepTime <= TIMEOUT_INTERVAL) {
                    // Рассчитываем скорость
                    long elapsedTimeInMillis = currentTime - startTime;
                    double elapsedTimeInHours = elapsedTimeInMillis / (1000.0 * 60 * 60);
                    double speed = stepCountInInterval / elapsedTimeInHours; // Скорость в шагах/час
                    int roundedSpeed = (int) Math.round(speed); // Округление до целого числа
                    speedSteps.setText("Скорость ходьбы: " + roundedSpeed + " шагов/час");
                } else {
                    // Сбрасываем скорость, если нет шагов в течение TIMEOUT_INTERVAL
                    speedSteps.setText("Скорость ходьбы: 0 шагов/час");
                    stepCountInInterval = 0;
                    startTime = currentTime;
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        }, CHECK_INTERVAL);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXACT_ALARM_PERMISSION_REQUEST_CODE) {
            if (canScheduleExactAlarms()) {
                scheduleStepReset(this);  // Передаем context явно
            } else {
                Toast.makeText(this, "Exact alarm permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            Log.e("Location", "Permission denied");
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d("Location", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
            getWeatherData(location.getLatitude(), location.getLongitude());
        }
    };

    private void getWeatherData(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OpenWeatherApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenWeatherApi openWeatherApi = retrofit.create(OpenWeatherApi.class);

        Call<WeatherResponse> call = openWeatherApi.getWeather(latitude, longitude, OpenWeatherApi.OPEN_WEATHER_API_KEY, "metric");
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse != null) {
                        double temperature = weatherResponse.getMain().getTemp();
                        double windSpeed = weatherResponse.getWind().getSpeed();
                        int visibility = weatherResponse.getVisibility();
                        DecimalFormat df = new DecimalFormat("#.###");
                        String formattedLatitude = df.format(latitude);
                        String formattedLongitude = df.format(longitude);
                        double rainVolume = (weatherResponse.getRain() != null) ? weatherResponse.getRain().getVolume() : 0; // Получаем объём осадков
                        String locationAndTempText = "Местоположение: " + formattedLatitude + "; " + formattedLongitude + "\n" +
                                "Температура: " + temperature + "°C\n" +
                                "Скорость ветра: " + windSpeed + " м/с\n" +
                                "Видимость: " + visibility + " м\n" +
                                "Осадки за последние 3 часа: " + rainVolume + " мм";
                        locationAndTemperature.setText(locationAndTempText);
                        loadingText.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("Weather", "Failed to get weather data: " + t.getMessage());
                loadingText.setVisibility(View.GONE); // Скрываем сообщение о загрузке при ошибке
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Уведомление можно будет отправлять позже
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == stepSensor) {
            totalSteps = (int) event.values[0];
            SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("steps", totalSteps);
            editor.apply();
            int currentSteps = totalSteps - previousTotalSteps;

            steps.setText(String.valueOf(currentSteps));
            progressBar.setProgress(currentSteps);

            // Обновление времени последнего шага
            lastStepTime = System.currentTimeMillis();

            // Увеличиваем счетчик шагов и обновляем время начала отсчета, если это первый шаг
            if (stepCountInInterval == 0) {
                startTime = lastStepTime;
            }
            stepCountInInterval++;


            // Пример уведомления о достижении 6000 шагов
            if (currentSteps >= 6000) {
                NotificationHelper.showNotification(this, "Цель достигнута!", "Вы достигли 6000 шагов!");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Обработка изменения точности сенсора
    }

    private void resetSteps() {
        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Long press to reset steps", Toast.LENGTH_SHORT).show();
            }
        });
        steps.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                previousTotalSteps = totalSteps; // Установка предыдущего значения шагов на текущее значение
                steps.setText("0");
                progressBar.setProgress(0);
                saveData();
                return true;
            }
        });
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("key1", previousTotalSteps);

        editor.putInt("steps", totalSteps);
        editor.apply();
        Log.e("SaveSteps", "steps=" + previousTotalSteps + "totalSteps=" + totalSteps);
    }

    private void loadData() {
        SharedPreferences sharedPref = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        int savedNumber = sharedPref.getInt("key1", 0); // Загружаем целочисленное значение
        //totalSteps = sharedPref.getInt("steps", 0);
        previousTotalSteps = savedNumber; // Не нужно приводить тип, так как это уже целочисленное значение
        Log.e("LoadSteps", "steps=" + previousTotalSteps + "totalSteps=" + totalSteps);
        //steps.setText(String.valueOf(totalSteps - previousTotalSteps));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepSensor != null) {
            mSensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        handler.postDelayed(this::startSpeedCheck, CHECK_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stepSensor != null) {
            mSensorManager.unregisterListener(this, stepSensor);
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onWeatherDataReceived(String weatherData) {

    }

    @Override
    public void onWeatherUpdated(String weatherInfo) {
        // Обновление UI с данными о погоде
        locationAndTemperature.setText(weatherInfo);
        loadingText.setVisibility(View.GONE);
    }
}
