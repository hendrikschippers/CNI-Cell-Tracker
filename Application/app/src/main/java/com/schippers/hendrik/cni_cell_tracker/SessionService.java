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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SessionService extends MeasureService {
    public static final String Start_Session_Service = "Start_Session_Service";
    public static final String Stop_Session_Service = "Stop_Session_Service";
    public static final String TAG_Session_Service = "TAG_Session_Service";
    private static final String CHANNEL_ID = "0";

    // LocationClient
    private boolean m_cell_log;
    private boolean m_gps_log;
    private boolean m_ping_log;
    private boolean m_transmit_test;

    private final long m_loggingStart = System.currentTimeMillis() / 1000;
    private int m_logging_duration;
    private int m_logging_interval;
    private int m_Iperf_Interval;
    private long m_loggingStop;
    private boolean m_csv_log;

    private boolean m_only_UDP_Large; //If this is true, only UDP test will be performed by IPerf. This is for Case Study Teleoperated Driveing
    private String m_ServerIP;
    private int m_ServerPort;
    private int m_parallelConnections;
    ICMP_Ping.PingResult pingResult;

    Date m_startTime;

    final Handler m_handler = new Handler();
    final Handler m_fastCellLogHandler = new Handler();
    final IperfSpeedtest.IperfCommandResults[] commandResults = new IperfSpeedtest.IperfCommandResults[1];

    volatile long m_count = 0;


    // Binder given to clients
    protected final IBinder binder = new SessionService.LocalBinder();

    // Extended Binder for app-only access
    public class LocalBinder extends Binder {
        SessionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SessionService.this;
        }
    }

    /*
    CellLog:
    Insgesamt gibt es 3 verschiedene Typen von Logs:
    -Log on Change Listener
    -Log bei Messung z.B. alle 10s
    -Log jede sekunde
    Dadurch soll möglichst alles mit geloggt werden
     */
    final Runnable cellLoggingFast = new Runnable() {
        @Override
        public void run() {
            if (m_cell_log) {
                Thread cellLogThread = null;
                if (m_cell_log) {
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
                        Log.d(TAG_Session_Service, "CellLogThread: Interrupted");
                    }
                }
                if (m_logging_duration == 0 || (m_logging_duration > 0 && System.currentTimeMillis() / 1000 < m_loggingStop)) {

                    if (m_csv_log)
                        writeNetworkLog();
                    // Reschedule
                    // if (m_csv_write) {
                    m_fastCellLogHandler.postDelayed(cellLoggingFast, m_logging_interval);
                    // }
                }
            }
        }
    };
    private Thread transmitThread;
    final Runnable timedLogging = new Runnable() {

        @Override
        public void run() {

            final Thread cellLogThread = new Thread() {
                @Override
                public void run() {
                    checkNetworkState();
                }

            };
            if (m_cell_log) {

                cellLogThread.start();
            }
            //final Thread pingThread = new Thread() {
            //   @Override
            //  public void run() {
            if (m_ping_log){
                pingResult = ICMP_Ping.ping(m_ServerIP, new ICMP_Ping.PingResult());
        }
               // }
            //};
            //if (m_ping_log) {
            //    pingThread.start();
            //}
            commandResults[0] = new IperfSpeedtest.IperfCommandResults();
            commandResults[0].errors = "not started";
            commandResults[0].result ="";
            commandResults[0].fileSize = -1;


            // Test if transmit test has finished. Could be not finished if delayed or this is exceuted to frequently
            if (transmitThread != null && transmitThread.isAlive()) {
                try {
                    transmitThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG_Session_Service, "Transmit thread not ready. Waiting for it to finish");
                }
            }
            transmitThread = new Thread() {
                @Override
                public void run() {
                    IperfSpeedtest pSpeedtest = new IperfSpeedtest(getApplicationContext());
                    if(m_only_UDP_Large){
                        commandResults[0] = pSpeedtest.udpUploadLargeTest(m_ServerIP, m_ServerPort, m_parallelConnections);
                    }else {
                        commandResults[0] = pSpeedtest.measureSpeedWithRandomFilesAndModesAndReturn(m_ServerIP, m_ServerPort, m_parallelConnections);
                    }
                }
            };
            if (m_transmit_test) {
                transmitThread.start();
            }

            //Sollte nie passieren, aber sicherheitshalber
            if (loggingThread != null && loggingThread.isAlive()) {
                try {
                    loggingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG_Session_Service, "LoggingThread interrupted");
                }
            }

            loggingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        if (transmitThread != null && transmitThread.isAlive()) {
                            transmitThread.join();
                        }
                        //if (pingThread != null && pingThread.isAlive()) {
                         //   pingThread.join();
                        //}
                        if (cellLogThread != null && cellLogThread.isAlive()) {
                            cellLogThread.join();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.d(TAG_Session_Service, "TransmitThread, PingThread or CellLogThread Interrupted");
                    }
                    if (m_cell_log || m_gps_log || m_transmit_test) {
                        //Read Settings from Storage
                        if (m_logging_duration == 0 || (m_logging_duration > 0 && System.currentTimeMillis() / 1000 < m_loggingStop)) {

                            if (m_csv_log) {
                                writeCSVLog();
                            }
                            // Reschedule
                            // if (m_csv_write) {
                            m_handler.postDelayed(timedLogging, m_Iperf_Interval * 1000L);

                            // }
                        }
                    }
                    synchronized(this) {
                        m_count++;
                    }

                }
            };
            loggingThread.start();

            Log.d(TAG_Session_Service, "after join");

        }
    };
    private CSV_Logger csvLogger;
    private String m_csv_name;
    private PhoneStateListener phoneStateListener;
    private Thread loggingThread;

    private synchronized void writeCSVLog() {
        //Write Network data to csv-logfile
        if (m_cell_log || m_gps_log)
            writeNetworkLog();
        //Write ping results to csv-logfile
        if (m_ping_log)
            writePingLog();
        //write speedtest log to logfile and csv logfile
        if (m_transmit_test)
            writeSpeedtestLog();

        writeCombinedCSVLog();
    }

    private synchronized void writeCombinedCSVLog() {
        csvLogger.writeCombindedCSVLog(m_cell_log, m_gps_log, m_transmit_test, m_ping_log, zCellInfo, m_count, this, commandResults[0], pingResult, m_startTime);
    }

    private synchronized void writePingLog() {
        csvLogger.writePingLogToCSV(pingResult, m_count);
    }

    private synchronized void writeNetworkLog() {
        csvLogger.writeNetworkCSVLog(m_startTime, zCellInfo, m_count, m_csv_name, m_cell_log, m_gps_log);
        if (m_cell_log)
            csvLogger.writeNeighboringCellCSVLog(zNeighborCellInfos, m_count, "neighboringCellLog", m_startTime);
    }


    private synchronized void writeSpeedtestLog() {

        csvLogger.writeSpeedtestLog(commandResults[0], m_count);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG_Session_Service, "SessionService: onCreate().");
        csvLogger = new CSV_Logger(getApplicationContext());
        SharedPreferences m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        m_ServerIP = m_sharedPreferences.getString("iperf_server_ip", "192.168.178.195");
        m_ServerPort = Integer.parseInt(m_sharedPreferences.getString("iperf_server_port", "5201"));
        m_parallelConnections = Integer.parseInt(m_sharedPreferences.getString("parallelConnections", "1"));
        m_logging_duration = Integer.parseInt(m_sharedPreferences.getString("duration", "60"));
        m_logging_interval = Integer.parseInt(m_sharedPreferences.getString("interval", "1"));
        m_Iperf_Interval = Integer.parseInt(m_sharedPreferences.getString("intervalIperf","10"));
        m_loggingStop = m_loggingStart + m_logging_duration;
        m_csv_name = m_sharedPreferences.getString("csv_name", "cell_log");
        m_only_UDP_Large = m_sharedPreferences.getBoolean("only_udp_large",false);

        //Get measurement count
        m_count = CSV_Logger.getNextMeasurementIndex();


        // Create location request
        m_locationRequest = new LocationRequest();
        m_locationRequest.setInterval(1000);
        m_locationRequest.setFastestInterval(1);
        m_locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Could request permissions here
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                String location_provider = location.getProvider();
                Log.d(TAG_Session_Service, "Location provider: " + location_provider);
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
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            switch (action) {
                case Start_Session_Service:
                    m_startTime = Calendar.getInstance().getTime();

                    m_csv_log = extras.getBoolean("csvEnabled");
                    m_cell_log = extras.getBoolean("celllog");
                    m_gps_log = extras.getBoolean("gpslog");
                    m_ping_log = extras.getBoolean("pingEnabled");
                    m_transmit_test = extras.getBoolean("transmit");
                    startSessionService();
                    break;
                case Stop_Session_Service:
                    stopSessionService();
                    break;
            }
        }
        flags = START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            switch (action) {
                case Start_Session_Service:
                    startSessionService();
                    m_csv_log = extras.getBoolean("csvEnabled");
                    m_cell_log = extras.getBoolean("celllog");
                    m_gps_log = extras.getBoolean("gpslog");
                    m_ping_log = extras.getBoolean("pingEnabled");
                    m_transmit_test = extras.getBoolean("transmit");
                    break;
                case Stop_Session_Service:
                    stopSessionService();
                    break;
            }
        }
        return null;
    }

    private void stopSessionService() {
        Log.d(TAG_Session_Service, "Stop foreground service.");
        m_cell_log = false;
        m_transmit_test = false;
        m_ping_log = false;
        m_gps_log = false;
        //Stop listening of rlocation and Network changes
        if (m_gps_log)
            stopLocationUpdates();
        if (tm != null && phoneStateListener != null) {
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        m_fastCellLogHandler.removeCallbacks(cellLoggingFast);


        stopForeground(true);

        stopSelf();
    }

    private void startSessionService() {
        Log.d(TAG_Session_Service, "Start Session service.");
        //Notification to Stop Measurement Session and show that Session is running
        createNotificationChannel(CHANNEL_ID);
        // Create notification default intent.
        Intent intent = new Intent(this, ScrollingActivity.class);
        intent.putExtra("stopMeasurementSession", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Measurement Session is running")
                .setContentText("Tap to stop")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        //
        if (m_gps_log)
            startLocationUpdates();

        if (m_cell_log && m_csv_log) {

            phoneStateListener = new PhoneStateListener() {
                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void onCellInfoChanged(List<CellInfo> cellInfo) {
                    Log.i(TAG_Session_Service, "CellInfo changed");
                    //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();checkNetworkState();
                    checkNetworkState();
                    writeNetworkLog();
                }

                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void onCellLocationChanged(CellLocation location) {
                    Log.i(TAG_Session_Service, "CellLocation changed");
                    //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();
                    checkNetworkState();
                    writeNetworkLog();
                }

                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    Log.i(TAG_Session_Service, "SignalStrength changed");
                    //Toast.makeText(this, "CellInfo changed!", Toast.LENGTH_SHORT).show();
                    checkNetworkState();
                    writeNetworkLog();
                }

                @RequiresApi(api = Build.VERSION_CODES.R)
                @Override
                public void onDisplayInfoChanged(@NonNull TelephonyDisplayInfo telephonyDisplayInfo) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
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
                    int nwtype = telephonyDisplayInfo.getNetworkType();

                    zCellInfo.m_nr_registerred_override = overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA;
                    zCellInfo.m_nr_registerred_override_mmWave = overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE;
                }
            };
            int events = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                events = PhoneStateListener.LISTEN_CELL_INFO
                        | PhoneStateListener.LISTEN_CELL_LOCATION
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED;
            }
            tm.listen(phoneStateListener, events);
        }

        //start foreground service
        startForeground(0, notification);
        m_handler.postDelayed(timedLogging, m_Iperf_Interval);
        m_fastCellLogHandler.postDelayed(cellLoggingFast, m_logging_interval);
    }
    @Override
    public void onDestroy(){
       if(loggingThread != null) {
           try {
               loggingThread.join();
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
        stopSessionService();
        Toast.makeText(this, "Session Service Stopped", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }
}