package com.parrotgeek.parrotmodfloapp;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.app.PendingIntent;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ethan on 5/16/16.
 */
public class MyService extends Service {
    public static boolean running = false;
    private static final String TAG = "ParrotMod";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = getApplicationContext().getApplicationInfo().dataDir + "/" + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private String execCmd(String[] cmd) {
        try {
            java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
            return s.hasNext() ? s.next().trim() : "";
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "EXEC ERROR: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            running = false;
            return "";
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        copyFile("ParrotMod.sh");
        copyFile("emi_config.bin");
        String script = getApplicationContext().getApplicationInfo().dataDir + "/ParrotMod.sh";
        String sucheck = execCmd(new String[]{"su", "-c", "echo hello"});
        if (!sucheck.equals("hello")) {
            Toast.makeText(this, "ParrotMod ERROR:\n\nYOU DON'T HAVE ROOT\nOr, you denied it.", Toast.LENGTH_LONG).show();
            running = false;
            return START_STICKY;
        }
        final String[] cmd = new String[]{"su", "-c", " sh '" + script + "' </dev/null >/dev/null 2>&1"};
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    execCmd(cmd);
                    Log.e(TAG, "run: for some reason script stopped, restarting");
                }
            }
        }).start();

        new Thread(new Runnable() {
            private SensorManager mSensorManager;
            private SensorEventListener mSensorEventListener;
            @Override
            public void run() {
                 mSensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {

                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
                int microsec = 10000000; // 10 sec
                mSensorManager.registerListener(mSensorEventListener,accel,microsec);
                mSensorManager.registerListener(mSensorEventListener,gyro,microsec);
            }
        }).start();

        running = true;
        Intent notificationIntent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        notificationIntent.setData(Uri.parse("package:" + getPackageName()));
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("ParrotMod Running")
                .setContentText("Tap here and go to Notifications to hide this.")
                .setSmallIcon(R.drawable.notificon)
                .setContentIntent(resultPendingIntent)
                .setWhen(0)
                .build();
        startForeground(0x600dc0de, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("com.parrotgeek.parrotmodfloapp.action.START_SERVICE"));
    }
}
