package com.schippers.hendrik.cni_cell_tracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static androidx.legacy.content.WakefulBroadcastReceiver.startWakefulService;

public class ScrollingActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    PowerManager.WakeLock wakeLock;
    PowerManager.WakeLock mWakeLock;
    // Unique permission codes
    private Cell_Information zCellInfo;
    private static final String TAG = "ScrollingActivity";

    private SwitchCompat m_cell_switch;
    private SwitchCompat m_gps_switch;
    private SwitchCompat m_transmit_switch;
    private SwitchCompat m_ping_switch;
    private boolean m_cell_log;
    private boolean m_gps_log;
    private boolean m_transmit_test;
    private boolean m_ping_test;


    private boolean m_loggingActive = false;
    private ToggleButton m_toggleButton;


    UiUpdateService uiUpdateService;
    SessionService sessionService;

    final Handler m_ui_Handler = new Handler();
    boolean m_ui_Service_Bound = false;
    boolean m_session_Service_Bound = false;
    final Runnable timedUIUpdate = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void run() {
            if (uiUpdateService != null) {
                zCellInfo = uiUpdateService.getCellInfo();
                updateUI();
            }

            m_ui_Handler.postDelayed(timedUIUpdate, 2000);
        }
    };
    final Handler m_StopLogHandler = new Handler();
    final Runnable stopLog = new Runnable() {
        @Override
        public void run() {
            m_toggleButton = findViewById(R.id.toggle_logging);
            m_toggleButton.setChecked(false);
            stopLoggingService();
            Log.d(TAG, "Stop cell logging");
            Toast.makeText(ScrollingActivity.this, "Stop cell logging...", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra("stopMeasurementSession")) {
            stopLoggingService();
        }
        //Keep CPU on
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        mWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "myApp:tag2");
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        //Keep Screen on
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.activity_scrolling);
        Toolbar m_toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(m_toolbar);
        m_cell_switch = findViewById(R.id.cell_switch);
        m_cell_log = m_cell_switch.isChecked();
        m_gps_switch = findViewById(R.id.gps_switch);
        m_gps_log = m_gps_switch.isChecked();
        m_transmit_switch = findViewById(R.id.transmit_switch);
        m_transmit_test = m_transmit_switch.isChecked();
        m_toggleButton = findViewById(R.id.toggle_logging);
        m_ping_switch = findViewById(R.id.pingSwitch);
        m_ping_test = m_ping_switch.isChecked();
        // Check for permissions
        ArrayList<String> permissionsListDenied = checkPermissions();
        if (!permissionsListDenied.isEmpty()) {
            askForPermissions(permissionsListDenied);
        }

        m_cell_switch.setOnCheckedChangeListener((buttonView, isChecked) -> m_cell_log = m_cell_switch.isChecked());

        m_gps_switch.setOnCheckedChangeListener((buttonView, isChecked) -> m_gps_log = m_gps_switch.isChecked());
        m_transmit_switch.setOnCheckedChangeListener((compoundButton, b) -> m_transmit_test = m_transmit_switch.isChecked());
        m_ping_switch.setOnCheckedChangeListener((buttonView, isChecked) -> m_ping_test = m_ping_switch.isChecked());


       Intent uIUpdateMeasureService = new Intent(this, UiUpdateService.class);
        uIUpdateMeasureService.setAction(UiUpdateService.START_MEASURE_SERVICE);
        bindService(uIUpdateMeasureService, uiUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        new Intent(this, UiUpdateService.class);
        //UI Updates
        m_ui_Handler.postDelayed(timedUIUpdate, 0);



    }


    private final ServiceConnection uiUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            UiUpdateService.LocalBinder binder = (UiUpdateService.LocalBinder) service;
            uiUpdateService = binder.getService();
            m_ui_Service_Bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            m_ui_Service_Bound = false;
        }

    };
    private final ServiceConnection sessionServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SessionService.LocalBinder binder = (SessionService.LocalBinder) service;
            sessionService = binder.getService();
            m_session_Service_Bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            m_session_Service_Bound = false;
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Settings
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            // Log files
            //case R.id.action_logs:
               // intent = new Intent(this, FileListActivity.class);
               // startActivity(intent);
              //  return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {

        super.onResume();

        Intent uIUpdateMeasureService = new Intent(this, UiUpdateService.class);
        uIUpdateMeasureService.setAction(UiUpdateService.START_MEASURE_SERVICE);
        bindService(uIUpdateMeasureService, uiUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        m_ui_Handler.postDelayed(timedUIUpdate, 100);


        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {

        super.onPause();
        m_ui_Handler.removeCallbacksAndMessages(null);
        if (m_ui_Service_Bound) {
            unbindService(uiUpdateServiceConnection);
            m_ui_Service_Bound = false;
        }
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (m_ui_Service_Bound) {
            unbindService(uiUpdateServiceConnection);
            m_ui_Service_Bound = false;
        }
        if (m_session_Service_Bound) {
            unbindService(sessionServiceConnection);
            m_session_Service_Bound = false;
        }


        //wakeLock.release();

    }

    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;

    @RequiresApi(api = Build.VERSION_CODES.M)
    private ArrayList<String> checkPermissions() {
        ArrayList<String> requestPermissionsList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        // ACCESS BACKGROUND LOCATION (API Q ONLY)
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (!requestPermissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

        return requestPermissionsList;

    }


    public void askForPermissions(ArrayList<String> requestPermissionsList) {
        if (!requestPermissionsList.isEmpty()) {
            String[] test = requestPermissionsList.toArray(new String[0]);
            requestPermissions(test, PERMISSIONS_MULTIPLE_REQUEST);
        }
    }

    // Start logging service
    public void startLoggingService(boolean pCSVLog) {
        Intent serviceIntent = new Intent(getApplicationContext(), SessionService.class);
        serviceIntent.putExtra("celllog", m_cell_log);
        serviceIntent.putExtra("gpslog", m_gps_log);
        serviceIntent.putExtra("csvEnabled", pCSVLog);
        serviceIntent.putExtra("transmit", m_transmit_test);
        serviceIntent.putExtra("pingEnabled", m_ping_test);

        // serviceIntent.putExtra("inputExtra", extras);
        serviceIntent.setAction(SessionService.Start_Session_Service);

        startWakefulService(getApplicationContext(), serviceIntent);
        //startService(serviceIntent);
        //  bindService(serviceIntent, sessionServiceConnection, Context.BIND_AUTO_CREATE);

    }

    // Stop logging service
    public void stopLoggingService() {
        // Remove pending runnables
        m_cell_switch.setEnabled(true);
        m_gps_switch.setEnabled(true);
        m_transmit_switch.setEnabled(true);
        m_ping_switch.setEnabled(true);
        m_loggingActive = false;
        m_toggleButton.setChecked(false);
        m_loggingActive = false;
        Log.d(TAG, "Stop cell logging");
        Toast.makeText(ScrollingActivity.this, "Stop cell logging...", Toast.LENGTH_SHORT).show();

        Intent serviceIntent = new Intent(this, SessionService.class);
        serviceIntent.setAction(SessionService.Stop_Session_Service);
        // Additional data
        //serviceIntent.putExtra("inputExtra", STOP_LOGGING_SERVICE);
        startService(serviceIntent);
        //unbindService(sessionServiceConnection);
        //stopService(serviceIntent);
        //Intent speedtestService = new Intent(this, SpeedtestService.class);
        //speedtestService.setAction(SpeedtestService.STOP_LOGGING_SERVICE);
        //startService(speedtestService);
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }

    }

    // De-/activate logging
    /*
    Logging Switch has been activated/deactivated
     */
    public void switchLogging(View view) {
        // Stop logging
        if (m_loggingActive) {
            Log.d(TAG, "Stop logging");
            Toast.makeText(this, "Stop logging", Toast.LENGTH_SHORT).show();

            stopLoggingService();

        }
        // Read preference parameters and start logging
        else {
            if (m_cell_log || m_gps_log || m_transmit_test) {
                m_cell_switch.setEnabled(false);
                m_gps_switch.setEnabled(false);
                m_transmit_switch.setEnabled(false);
                m_ping_switch.setEnabled(false);
                m_loggingActive = true;
                // Get preferences
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                int m_logging_duration = Integer.parseInt(sharedPreferences.getString("duration", "60"));
                //Start Speed test as well in a parralel service
                // if (m_transmit_test) {
                //    Intent speedtestIntent = new Intent(this, SpeedtestService.class);
                //   speedtestIntent.setAction(SpeedtestService.START_LOGGING_SERVICE);
                //  startService(speedtestIntent);
                // }
                if (m_cell_log || m_gps_log || m_transmit_test) {
                    // CSV logging?
                    // CSV
                    boolean m_csv_write = sharedPreferences.getBoolean("csv", false);

                    startLoggingService(m_csv_write);
                }
                // Set stop handler for logging
                if (m_logging_duration > 0) {
                    m_StopLogHandler.postDelayed(stopLog, m_logging_duration * 1000L);
                }
            }
            // Neither cell nor GPS logging
            else {
                Log.d(TAG, "Neither cell nor gps logging");
                Toast.makeText(ScrollingActivity.this, "Neither cell nor GPS logging...", Toast.LENGTH_LONG).show();
                m_toggleButton = findViewById(R.id.toggle_logging);
                m_toggleButton.setChecked(false);

                // Stop service
            }
        }
    }


    // Get current network info
    @SuppressLint("SetTextI18n")
    private void updateUI() {

        TextView available_value = findViewById(R.id.available_value);
        TextView type_value = findViewById(R.id.type_value);
        TextView reg_value = findViewById(R.id.reg_value);
        TextView ci_value = findViewById(R.id.cellI_value);
        TextView nci_value = findViewById(R.id.cellNCIValue);
        TextView mcc_value = findViewById(R.id.mcc_value);
        TextView mnc_value = findViewById(R.id.mnc_value);
        TextView operator_value = findViewById(R.id.operator_value);
        TextView pci_value = findViewById(R.id.pci_value);
        TextView tac_value = findViewById(R.id.tac_value);
        TextView cqi_value = findViewById(R.id.cqi_value);
        TextView rsrp_value = findViewById(R.id.rsrp_value);
        TextView rsrq_value = findViewById(R.id.rsrq_value);
        TextView csi_rsrp_value = findViewById(R.id.csiRSRPvalue);
        TextView csi_rsrq_value = findViewById(R.id.csiRSRQvalue);
        TextView ss_value = findViewById(R.id.ss_value);
        TextView rssnr_value = findViewById(R.id.rssnr_value);
        TextView csi_sinr_value = findViewById(R.id.csi_rssnr_value);
        TextView ta_value = findViewById(R.id.ta_value);
        TextView speedtestStatusText = findViewById(R.id.speedtestStatus);
        TextView speedtestStatus = findViewById(R.id.textViewSpeedtestResults);
        TextView cellSignalStrengthString = findViewById(R.id.cellSignalStrenthString);


        if (isMyServiceRunning(SessionService.class)) {
            speedtestStatus.setText("Speedtest is running");
        } else {
            speedtestStatus.setText("Speedtest      off!");
        }

        Date date = android.icu.util.Calendar.getInstance().getTime();
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());
        File filesDir = Environment.getExternalStorageDirectory();
        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "IperfTransferLog" + File.separator);
        dir = new File(dir, dfDay.format(date));
        File[] files = dir.listFiles();

        if (files != null) {
            int anzahl = files.length;

            if (anzahl > 2) {
                anzahl -= 2;
            }
            speedtestStatusText.setText(String.valueOf(anzahl));
        } else {
            speedtestStatusText.setText("0");
        }

        //Get last modified file
        long lastModifiedTime = Long.MIN_VALUE;
        File latestFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime && file.getName().endsWith(".txt")) {
                    latestFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }
        if(latestFile != null) {
            speedtestStatusText.setText("\t"+speedtestStatusText.getText() + "\t\t\t\t\t\t"+ latestFile.length()/1024+ " KB");
        }


        if (zCellInfo.cellSignalStrengthString != null) {
            cellSignalStrengthString.setText(zCellInfo.cellSignalStrengthString);
        } else {
            cellSignalStrengthString.setText("-");
        }
        if (zCellInfo.m_mcc != null) {
            mcc_value.setText(zCellInfo.m_mcc);
        } else {
            mcc_value.setText("-");
        }
        if (zCellInfo.m_mnc != null) {
            mnc_value.setText(zCellInfo.m_mnc);
        } else {
            mnc_value.setText("-");
        }
        if (zCellInfo.m_networkType != null && uiUpdateService.isRegistered()) {
            type_value.setText(zCellInfo.m_networkType);
        } else {
            type_value.setText("-");
        }
        if (zCellInfo.m_ci != null) {
            ci_value.setText(zCellInfo.m_ci);
        } else {
            ci_value.setText("-");
        }
        if (zCellInfo.m_pci != null) {
            pci_value.setText(zCellInfo.m_pci);
        } else {
            pci_value.setText("-");
        }
        if (zCellInfo.m_tac != null) {
            tac_value.setText(zCellInfo.m_tac);
        } else {
            tac_value.setText("-");
        }
        if (zCellInfo.m_nci != -1) {
            nci_value.setText(String.valueOf(zCellInfo.m_nci));
        } else {
            nci_value.setText("-");
        }

        if (zCellInfo.m_nr_registered) {
            ss_value.setText("-");
            if (zCellInfo.m_ss_rsrp != -1) {
                rsrp_value.setText(String.valueOf(zCellInfo.m_ss_rsrp));
            } else {
                if (zCellInfo.m_rsrp != null) {
                    rsrp_value.setText(zCellInfo.m_rsrp);
                } else {
                    rsrp_value.setText("-");
                }
            }
            if (zCellInfo.m_ss_rsrq != -100) {
                rsrq_value.setText(String.valueOf(zCellInfo.m_ss_rsrq));
            } else {
                if (zCellInfo.m_rsrq != null) {
                    rsrq_value.setText(zCellInfo.m_rsrq);
                } else {
                    rsrq_value.setText("-");
                }
            }
            if (zCellInfo.m_csi_rsrp != -1) {
                csi_rsrp_value.setText(String.valueOf(zCellInfo.m_csi_rsrp));
            } else {
                csi_rsrp_value.setText("-");
            }
            if (zCellInfo.m_csi_rsrq != -100) {
                csi_rsrq_value.setText(String.valueOf(zCellInfo.m_csi_rsrq));
            } else {
                csi_rsrq_value.setText("-");
            }


            if (zCellInfo.m_ss_sinr != -1) {
                rssnr_value.setText(String.valueOf(zCellInfo.m_ss_sinr));
            } else {
                if (zCellInfo.m_rssnr != null) {
                    rssnr_value.setText(zCellInfo.m_rssnr);
                } else {
                    rssnr_value.setText("-");
                }
            }
            if (zCellInfo.m_csi_sinr != -1) {
                csi_sinr_value.setText(String.valueOf(zCellInfo.m_csi_sinr));
            } else {
                csi_sinr_value.setText("-");
            }

            ta_value.setText("");
            cqi_value.setText("");
        } else {
            csi_rsrp_value.setText("-");
            csi_rsrq_value.setText("-");
            if (zCellInfo.m_rssi != null) {
                ss_value.setText(zCellInfo.m_rssi);
            } else {
                ss_value.setText("-");
            }
            if (zCellInfo.m_rsrp != null) {
                rsrp_value.setText(zCellInfo.m_rsrp);
            } else {
                rsrp_value.setText("-");
            }
            if (zCellInfo.m_rsrq != null) {
                rsrq_value.setText(zCellInfo.m_rsrq);
            } else {
                rsrq_value.setText("-");
            }
            if (zCellInfo.m_rssnr != null) {
                rssnr_value.setText(zCellInfo.m_rssnr);
            } else {
                rssnr_value.setText("-");
            }
        }
        if (zCellInfo.m_cqi != null) {
            cqi_value.setText(zCellInfo.m_cqi);
        } else {
            cqi_value.setText("-");
        }
        if (zCellInfo.m_ta != null) {
            ta_value.setText(zCellInfo.m_ta);
        } else {
            ta_value.setText("-");
        }

        String available_cell_types = uiUpdateService.getAvailableNetworks();


        if (available_cell_types.equals("")) {
            available_value.setText("-");
        } else {
            available_value.setText(available_cell_types);
        }


        //String registered_types = uiUpdateService.getRegisteredNetworks();

        if (uiUpdateService.isRegistered()) {
            reg_value.setText("Yes");
        } else {
            reg_value.setText("No");
        }

        if (zCellInfo.m_operator != null) {
            operator_value.setText(zCellInfo.m_operator);
        } else {
            operator_value.setText("-");
        }

        ///GPS Data Displaying
        // Widget refs
        TextView lat_value = findViewById(R.id.lat_value);
        TextView long_value = findViewById(R.id.long_value);
        TextView altitude_value = findViewById(R.id.altitude_value);
        TextView speed_value = findViewById(R.id.speed_value);

        if (zCellInfo.m_latitude == -1) {
            lat_value.setText("-");
        } else {
            lat_value.setText(String.valueOf(zCellInfo.m_latitude));
        }
        if (zCellInfo.m_longitude == -1) {
            long_value.setText("-");
        } else {
            long_value.setText(String.valueOf(zCellInfo.m_longitude));
        }
        if (zCellInfo.m_altitude == -1) {
            altitude_value.setText("-");
        } else {
            altitude_value.setText(String.valueOf(zCellInfo.m_altitude));
        }
        if (zCellInfo.m_speed == -1) {
            speed_value.setText("-");
        } else {
            speed_value.setText(String.valueOf(zCellInfo.m_speed));
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}//Class end

