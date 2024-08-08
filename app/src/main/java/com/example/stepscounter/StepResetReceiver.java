package com.example.stepscounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class StepResetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        int totalSteps = sharedPref.getInt("key1", 0);
        int previousTotalSteps = sharedPref.getInt("steps", 0);
        int currentSteps = previousTotalSteps - totalSteps ;
        Log.e("StepsNotification", "steps=" + previousTotalSteps + "totalSteps=" + totalSteps);
        // Отправляем уведомление
        NotificationHelper.showNotification(context, "Step Counter", "Ты прошёл " + currentSteps + " шагов сегодня!");

        SharedPreferences sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("key1", previousTotalSteps);

        editor.putInt("steps", totalSteps);
        editor.apply();

        //Toast.makeText(context, "Шаги сброшены!", Toast.LENGTH_SHORT).show();


    }
}
