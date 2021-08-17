package com.schippers.hendrik.cni_cell_tracker;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class UiUpdateService extends MeasureService {

    private static final String TAG_MEASURE_SERVICE = "UI_Update_Service";
    public static final String START_MEASURE_SERVICE = "START_MEASURE_SERVICE";
    public static final String STOP_MEASURE_SERVICE = "STOP_MEASURE_SERVICE";
    private static final String CHANNEL_ID = "2";

    // Binder given to clients
    protected final IBinder binder = new LocalBinder();

    // Extended Binder for app-only access
    public class LocalBinder extends Binder {
        UiUpdateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return UiUpdateService.this;
        }
    }
    final Handler m_handler = new Handler();
    final Runnable cellUpdate = new Runnable() {
        @Override
        public void run() {

                Thread cellLogThread;
                    cellLogThread = new Thread() {
                        @Override
                        public void run() {
                            checkNetworkState();
                        }
                    };
                    cellLogThread.start();
                    try {
                        cellLogThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

            m_handler.postDelayed(cellUpdate,2000);

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
                case START_MEASURE_SERVICE:
                    startForegroundService();
                    //Toast.makeText(getApplicationContext(), "UI Update service has started.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
        return binder;
    }


    public Cell_Information getCellInfo() {

        return zCellInfo;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG_MEASURE_SERVICE, "MeasureService: onCreate().");
        SharedPreferences m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Create location request
        m_locationRequest = new LocationRequest();
        int m_logging_interval = Integer.parseInt(m_sharedPreferences.getString("interval", "10"));
        m_locationRequest.setInterval(m_logging_interval);
        m_locationRequest.setFastestInterval(m_logging_interval);
        m_locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                String location_provider = location.getProvider();
                Log.d(TAG_MEASURE_SERVICE, "Location provider: " + location_provider);
            }
        });

        m_locationCallback = new LocationCallback() {
            // @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    //Log.d(TAG_MEASURE_SERVICE, location.toString());
                    parseGPSData(location);
                }
            }
        };

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener listener = new PhoneStateListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo) {
                Log.i(TAG_MEASURE_SERVICE, "CellInfo changed");
                //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();
                checkNetworkState();
            }

            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onCellLocationChanged(CellLocation location) {
                Log.i(TAG_MEASURE_SERVICE, "CellLocation changed");
                //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();
                checkNetworkState();
            }

            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                Log.i(TAG_MEASURE_SERVICE, "SignalStrength changed");
                //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();
                checkNetworkState();
            }

            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {


                if (ActivityCompat.checkSelfPermission(UiUpdateService.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                super.onDisplayInfoChanged(telephonyDisplayInfo);

                int overrideNetworkType = telephonyDisplayInfo.getOverrideNetworkType();
                //int nwtype = telephonyDisplayInfo.getNetworkType();

                zCellInfo.m_nr_registerred_override = overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA;
                zCellInfo.m_nr_registerred_override_mmWave = overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE;
            }
        };

        int events;
        events = PhoneStateListener.LISTEN_CELL_INFO
                | PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
        | PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED;

        // Subscribe to events
        tm.listen(listener, events);

        //final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // startForegroundService();
            // Toast.makeText(getApplicationContext(), "Logging service is started.", Toast.LENGTH_SHORT).show();
            String action = intent.getAction();
            switch (action) {
                case START_MEASURE_SERVICE:
                    startForegroundService();
                    //Toast.makeText(getApplicationContext(), "Logging service is started.", Toast.LENGTH_LONG).show();
                    break;
                case STOP_MEASURE_SERVICE:
                    stopForegroundService();
                    //Toast.makeText(getApplicationContext(), "Logging service is stopped.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /* Used to build and start foreground service. */
    protected void startForegroundService() {
        Log.d(TAG_MEASURE_SERVICE, "Start measuring service.");

        // Create notification channel (for SDK >= 26)
        createNotificationChannel(CHANNEL_ID);

        // Create notification default intent.
        Intent intent = new Intent(this, ScrollingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Measurement service running")
                //.setContentText("Tap to stop")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startLocationUpdates();

        // Start foreground service.
        startForeground(2, notification);
        m_handler.postDelayed(cellUpdate,2000);

    }

    protected void stopForegroundService() {
        Log.d(TAG_MEASURE_SERVICE, "Stop foreground service.");

        // Remove service form foreground and remove the notification.
        stopForeground(true);

        // Stop service
        stopSelf();
    }



    //public Object[] getGPSInfo() {
   //     return new Object[]{zCellInfo.m_latitude, zCellInfo.m_longitude, zCellInfo.m_provider, zCellInfo.m_time, zCellInfo.m_accuracy, zCellInfo.m_altitude,
   //             zCellInfo.m_speed, zCellInfo.m_speedAccuracy, zCellInfo.m_verticalAccuracy};
   // }



}
