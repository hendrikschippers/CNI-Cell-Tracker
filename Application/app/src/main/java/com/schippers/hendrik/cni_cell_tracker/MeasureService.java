package com.schippers.hendrik.cni_cell_tracker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MeasureService extends Service {
    private static final String TAG_MEASURE_SERVICE = "MEASURE_SERVICE";
    protected final Cell_Information zCellInfo = new Cell_Information();
    protected ArrayList<Cell_Information> zNeighborCellInfos = new ArrayList<>();

    // LocationClient
    protected FusedLocationProviderClient fusedLocationClient;
    protected LocationRequest m_locationRequest;
    protected LocationCallback m_locationCallback;
    protected TelephonyManager tm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected void createNotificationChannel(String pChannelID) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(pChannelID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Log.d(TAG_MEASURE_SERVICE, "Created notification channel with id " + pChannelID);

    }


    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(m_locationRequest,
                m_locationCallback, Looper.getMainLooper());

    }

    protected void stopLocationUpdates() {
        if (m_locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(m_locationCallback);
        }
    }


    protected void parseGPSData(Location location) {
        zCellInfo.resetGPSValues();


        //String gps_string = "";

        zCellInfo.nanosecs = location.getElapsedRealtimeNanos();

        zCellInfo.m_latitude = location.getLatitude();
        //String lat_string = String.valueOf(zCellInfo.m_latitude);
        //gps_string += lat_string;

        zCellInfo.m_longitude = location.getLongitude();
        //String long_str = String.valueOf(zCellInfo.m_longitude);
        //gps_string += long_str;

        zCellInfo.m_provider = location.getProvider();
        zCellInfo.m_time = location.getTime();

        // Accuracy
        if (location.hasAccuracy()) {
            zCellInfo.m_accuracy = location.getAccuracy();
        }

        // Altitude
        if (location.hasAltitude()) {
            zCellInfo.m_altitude = location.getAltitude();
            //String alt_string = String.valueOf(zCellInfo.m_altitude);
            //gps_string += alt_string;
        }

        // Speed
        if (location.hasSpeed()) {
            zCellInfo.m_speed = location.getSpeed();
            //String speed_string = String.valueOf(zCellInfo.m_speed);
        }

        // Speed Accuracy
        if (location.hasSpeedAccuracy()) {
            zCellInfo.m_speedAccuracy = location.getSpeedAccuracyMetersPerSecond();
        }

        // Vertical Accuracy
        if (location.hasVerticalAccuracy()) {
            zCellInfo.m_verticalAccuracy = location.getVerticalAccuracyMeters();
        }
    }

    public String getAvailableNetworks() {
        String available_cell_types = "";
        boolean type_set = false;

        if (zCellInfo.m_gsm_available) {
            available_cell_types += "GSM";
            type_set = true;
        }
        if (zCellInfo.m_wcdma_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "WCDMA";
            type_set = true;
        }
        if (zCellInfo.m_lte_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "LTE";
            type_set = true;
        }
        if (zCellInfo.m_nr_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "NR";
        }
        return available_cell_types;
    }

    public boolean isRegistered() {
        return (zCellInfo.m_gsm_registered || zCellInfo.m_wcdma_registered || zCellInfo.m_lte_registered || zCellInfo.m_nr_registered);
    }

    public String getRegisteredNetworks() {
        // Registered
        String registered_types = "";
        boolean type_registered = false;

        if (zCellInfo.m_gsm_registered) {
            registered_types += "GSM";
            type_registered = true;
        }
        if (zCellInfo.m_wcdma_registered) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "WCDMA";
            type_registered = true;
        }
        if (zCellInfo.m_lte_registered) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "LTE";
            type_registered = true;
        }
        if (zCellInfo.m_nr_registered || zCellInfo.m_nr_registerred_override || zCellInfo.m_nr_registerred_override_mmWave) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "NR";
            if (zCellInfo.m_nr_registerred_override_mmWave) {
                registered_types += "mmWave";
            }
            //type_registered = true;
        }


        return registered_types;
    }

    // Get current network type
    //public String getNetworkType() {
    //    return zCellInfo.m_networkType;
    //}

    // Get operator info
    // public String[] getOperatorInfo() {
    //      return new String[]{zCellInfo.m_mcc, zCellInfo.m_mnc, zCellInfo.m_operator};
    //}

    // Get GSM info
    //public Boolean[] getGSMInfo() {
    //    return new Boolean[]{zCellInfo.m_gsm_available, zCellInfo.m_gsm_registered};
    //}

    // Get WCDMA info
    //public Boolean[] getWCDMAInfo() {
    //    return new Boolean[]{zCellInfo.m_wcdma_available, zCellInfo.m_wcdma_registered};
    //}

    // Get LTE info
    //public Object[] getLTEInfo() {
    //     return new Object[]{zCellInfo.m_lte_available, zCellInfo.m_lte_registered, zCellInfo.m_ci, zCellInfo.m_pci, zCellInfo.m_tac,
    //             zCellInfo.m_asu, zCellInfo.m_cqi, zCellInfo.m_rsrp, zCellInfo.m_rsrq, zCellInfo.m_rssi, zCellInfo.m_rssnr, zCellInfo.m_level, zCellInfo.m_ta, zCellInfo.cellSignalStrengthString, zCellInfo.m_bandwith};
    // }

    // Get 5G info
    //public Object[] get5GInfo() {
    //    return new Object[]{zCellInfo.m_nr_available, zCellInfo.m_nr_registered, zCellInfo.m_nci, zCellInfo.m_mcc, zCellInfo.m_mnc,
    //            zCellInfo.m_nr_arfcn, zCellInfo.m_pci, zCellInfo.m_tac, zCellInfo.m_asu, zCellInfo.m_csi_rsrp, zCellInfo.m_csi_rsrq, zCellInfo.m_csi_sinr,
    //            zCellInfo.m_csi_rsrp_dbm, zCellInfo.m_ss_rsrp, zCellInfo.m_ss_rsrq, zCellInfo.m_ss_sinr, zCellInfo.m_nrlevel};
    //}

    // Get current network info
    public synchronized void checkNetworkState() {

        //Damit bei Netzwerkwechsel nicht alte Werte drin bleiben, die es bei neuem NEtzwerk nicht gibt
        zCellInfo.resetCellValues();
        // Log.d(TAG_MEASURE_SERVICE, "checkNetworkState()");


        zCellInfo.m_gsm_available = false;
        zCellInfo.m_wcdma_available = false;
        zCellInfo.m_lte_available = false;
        zCellInfo.m_nr_available = false;

        zCellInfo.m_gsm_registered = false;
        zCellInfo.m_wcdma_registered = false;
        zCellInfo.m_lte_registered = false;
        zCellInfo.m_nr_registered = false;

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        zCellInfo.serviceStateString = getServiceStateString(tm);
        zCellInfo.signalStrengthString = getSignalStrengthString(tm);
        // Network Operator
        String networkOperator = tm.getNetworkOperator();
        if (networkOperator != null && networkOperator.length() > 3) {
            zCellInfo.m_mcc = networkOperator.substring(0, 3);
            zCellInfo.m_mnc = tm.getNetworkOperator().substring(3);
        } else {
            zCellInfo.m_mcc = networkOperator;
            zCellInfo.m_mnc = null;
        }
        zCellInfo.m_operator = tm.getNetworkOperatorName();

        // Check for granted permissions
        if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            //EARFCN
            ServiceState tempServiceState = tm.getServiceState();
            if (tempServiceState != null){
            zCellInfo.m_EARFCN = tempServiceState.getChannelNumber();
                zCellInfo.m_Cell_Bandwidths = tempServiceState.getCellBandwidths();
            }else{
                zCellInfo.m_EARFCN = 0;
                zCellInfo.m_Cell_Bandwidths = null;
            }

            // Get Data Network Type
            switch (tm.getDataNetworkType()) {
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    zCellInfo.m_networkType = "Unknown";
                    break;
                // GSM
                case TelephonyManager.NETWORK_TYPE_GSM:
                    zCellInfo.m_networkType = "GSM";
                    break;
                // GPRS
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    zCellInfo.m_networkType = "GPRS";
                    break;
                // EDGE
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    zCellInfo.m_networkType = "EDGE";
                    break;
                // UMTS
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    zCellInfo.m_networkType = "UMTS";
                    break;
                // HSPA
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    zCellInfo.m_networkType = "HSPA";
                    break;
                // LTE
                case TelephonyManager.NETWORK_TYPE_LTE:
                    zCellInfo.m_networkType = "LTE";
                    break;
                // NR
                case TelephonyManager.NETWORK_TYPE_NR:
                    zCellInfo.m_networkType = "NR";
                    break;
                default:
                    zCellInfo.m_networkType = null;
                    break;

            }
            if (zCellInfo.m_nr_registerred_override) {

                zCellInfo.m_nr_available = true;
            }
            Log.d(TAG_MEASURE_SERVICE, "About to get AllCellInfo");
            List<CellInfo> cellInfoList = tm.getAllCellInfo();
            if(cellInfoList == null){
                Log.d(TAG_MEASURE_SERVICE, "cellInfoList is null");
            }else {
                Log.d(TAG_MEASURE_SERVICE, "got it");
                long timestampCellInfo = Calendar.getInstance().getTime().getTime();
                //Empty list of neighboring cells and then refill to update
                zNeighborCellInfos = new ArrayList<>(Math.max(1,cellInfoList.size() - 1));
                zCellInfo.timestampCellInfo = timestampCellInfo;
                //TODO: Filter for registered and non registered networks
                Log.d(TAG_MEASURE_SERVICE, "filter cellinfos and sort");
                for (CellInfo info : cellInfoList) {


                    // GSM Cell Info
                    if (info instanceof CellInfoGsm) {

                        zCellInfo.m_gsm_available = true;

                        CellInfoGsm cellInfo = (CellInfoGsm) info;
                        CellSignalStrengthGsm cellSignalStrength = cellInfo.getCellSignalStrength();
                        //CellIdentityGsm cellIdentity = cellInfo.getCellIdentity();
                        if (cellInfo.isRegistered()) {
                            zCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());
                            zCellInfo.cellSignalStrengthString = cellSignalStrength.toString();
                       /* toast = Toast.makeText(this, "GSM", Toast.LENGTH_SHORT);
                        toast.show();*/

                            // Registered
                            zCellInfo.m_gsm_registered = true;
                        } else {
                            Cell_Information pCellInfo = new Cell_Information();
                            pCellInfo.m_rsrp = String.valueOf(cellSignalStrength.getDbm());
                            pCellInfo.cellSignalStrengthString = cellSignalStrength.toString();
                            pCellInfo.m_gsm_available = true;
                            pCellInfo.m_networkType = "GSM";
                            pCellInfo.timestampCellInfo = timestampCellInfo;

                            zNeighborCellInfos.add(pCellInfo);
                        }
                    }
                    // WCDMA Cell Info
                    if (info instanceof CellInfoWcdma) {

                        zCellInfo.m_wcdma_available = true;

                        CellInfoWcdma cellInfo = (CellInfoWcdma) info;
                        CellSignalStrengthWcdma cellSignalStrength = cellInfo.getCellSignalStrength();
                        // CellIdentityWcdma cellIdentity = cellInfo.getCellIdentity();
                        if (cellInfo.isRegistered()) {
                            zCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());
                            zCellInfo.cellSignalStrengthString = cellSignalStrength.toString();
                    /*    toast = Toast.makeText(this, "WCDMA", Toast.LENGTH_SHORT);
                        toast.show();*/


                            zCellInfo.m_wcdma_registered = true;
                        } else {
                            Cell_Information pCellInfo = new Cell_Information();
                            pCellInfo.m_rsrp = String.valueOf(cellSignalStrength.getDbm());
                            pCellInfo.cellSignalStrengthString = cellSignalStrength.toString();
                            pCellInfo.m_wcdma_available = true;
                            pCellInfo.timestampCellInfo = timestampCellInfo;
                            pCellInfo.m_networkType = "WCDMA";
                            zNeighborCellInfos.add(pCellInfo);
                        }

                    }
                    // LTE Cell Info
                    if (info instanceof CellInfoLte) {
                        zCellInfo.m_lte_available = true;

                        CellInfoLte cellInfo = (CellInfoLte) info;
                        CellSignalStrengthLte cellSignalStrength = cellInfo.getCellSignalStrength();
                        CellIdentityLte cellIdentity = cellInfo.getCellIdentity();
                        if (cellInfo.isRegistered()) {

                            zCellInfo.cellSignalStrengthString = cellSignalStrength.toString();

                        /* toast = Toast.makeText(this, "LTE", Toast.LENGTH_SHORT);
                        toast.show();*/

                            String pBandwith = String.valueOf(cellIdentity.getBandwidth());
                            if (pBandwith.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_bandwith = "not available";
                            } else {
                                zCellInfo.m_bandwith = String.valueOf(cellIdentity.getBandwidth());
                            }


                            // Registered

                            zCellInfo.m_lte_registered = true;

                            // reg_value.setText(String.valueOf(cellInfo.isRegistered()));


                            // CI
                            zCellInfo.m_ci = String.valueOf(cellIdentity.getCi());
                            if (zCellInfo.m_ci.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_ci = "not available";
                            }
                            //Log.d(TAG, "ci: " + ci);

                            //int pci_int = cellIdentity.getPci();
                            zCellInfo.m_pci = String.valueOf(cellIdentity.getPci());
                            if (zCellInfo.m_pci.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_pci = "not available";
                            }
                            //Log.d(TAG, "pci: " + pci);

                            zCellInfo.m_tac = String.valueOf(cellIdentity.getTac());
                            if (zCellInfo.m_tac.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_tac = "not available";
                            }
                            //Log.d(TAG, "tac: " + tac);

                            // CellSingalStrengthLte
                            zCellInfo.m_asu = String.valueOf(cellSignalStrength.getAsuLevel());
                            if (zCellInfo.m_asu.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_asu = "not available";
                            }
                            zCellInfo.m_cqi = String.valueOf(cellSignalStrength.getCqi());
                            if (zCellInfo.m_cqi.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_cqi = "not available";
                            }

                            //Log.d(TAG, "cqi: " + cqi);

                            zCellInfo.m_rsrp = String.valueOf(cellSignalStrength.getRsrp());
                            if (zCellInfo.m_rsrp.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_rsrp = "not available";
                            }

                            //Log.d(TAG, "rsrp: " + rsrp);

                            // String m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());

                            zCellInfo.m_rsrq = String.valueOf(cellSignalStrength.getRsrq());
                            if (zCellInfo.m_rsrq.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_rsrq = "not available";
                            }

                            //Log.d(TAG, "rsrq: " + rsrq);


                            zCellInfo.m_rssi = "";

                            // Android API 29
                            zCellInfo.m_rssi = String.valueOf(cellSignalStrength.getRssi());
                            if (zCellInfo.m_rssi.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_rssi = "not available";
                            }
                            //Log.d(TAG, "rssi: " + rssi);

                            zCellInfo.m_rssnr = String.valueOf(cellSignalStrength.getRssnr());
                            if (zCellInfo.m_rssnr.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_rssnr = "not available";
                            }

                            //Log.d(TAG, "rssnr: " + rssnr);

                            zCellInfo.m_level = String.valueOf(cellSignalStrength.getLevel());
                            if (zCellInfo.m_level.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_level = "not available";
                            }
                            zCellInfo.m_ta = String.valueOf(cellSignalStrength.getTimingAdvance());
                            if (zCellInfo.m_ta.equals(Cell_Information.UNAVAILABLE)) {
                                zCellInfo.m_ta = "not available";
                            }
                            //Log.d(TAG, "ta: " + ta);

                            zCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());

                       /* TextView raw_value = findViewById(R.id.raw_value);
                        raw_value.setText(raw);
                        TextView cell_value = findViewById(R.id.ci_value);
                        cell_value.setText(raw_cell);*/
                        } else {
                            Cell_Information pCellInfo = new Cell_Information();
                            pCellInfo.timestampCellInfo = timestampCellInfo;

                            pCellInfo.cellSignalStrengthString = cellSignalStrength.toString();

                            String pBandwith = String.valueOf(cellIdentity.getBandwidth());
                            if (pBandwith.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_bandwith = "not available";
                            } else {
                                pCellInfo.m_bandwith = String.valueOf(cellIdentity.getBandwidth());
                            }
                            pCellInfo.m_ci = String.valueOf(cellIdentity.getCi());
                            if (pCellInfo.m_ci.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_ci = "not available";
                            }
                            pCellInfo.m_pci = String.valueOf(cellIdentity.getPci());
                            if (pCellInfo.m_pci.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_pci = "not available";
                            }
                            pCellInfo.m_tac = String.valueOf(cellIdentity.getTac());
                            if (pCellInfo.m_tac.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_tac = "not available";
                            }
                            pCellInfo.m_asu = String.valueOf(cellSignalStrength.getAsuLevel());
                            if (pCellInfo.m_asu.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_asu = "not available";
                            }
                            pCellInfo.m_cqi = String.valueOf(cellSignalStrength.getCqi());
                            if (pCellInfo.m_cqi.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_cqi = "not available";
                            }
                            pCellInfo.m_rsrp = String.valueOf(cellSignalStrength.getRsrp());
                            if (pCellInfo.m_rsrp.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_rsrp = "not available";
                            }

                            pCellInfo.m_rsrq = String.valueOf(cellSignalStrength.getRsrq());
                            if (pCellInfo.m_rsrq.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_rsrq = "not available";
                            }
                            pCellInfo.m_rssi = "";
                            // Android API 29
                            pCellInfo.m_rssi = String.valueOf(cellSignalStrength.getRssi());
                            if (pCellInfo.m_rssi.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_rssi = "not available";
                            }
                            pCellInfo.m_rssnr = String.valueOf(cellSignalStrength.getRssnr());
                            if (pCellInfo.m_rssnr.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_rssnr = "not available";
                            }

                            pCellInfo.m_level = String.valueOf(cellSignalStrength.getLevel());
                            if (pCellInfo.m_level.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_level = "not available";
                            }
                            pCellInfo.m_ta = String.valueOf(cellSignalStrength.getTimingAdvance());
                            if (pCellInfo.m_ta.equals(Cell_Information.UNAVAILABLE)) {
                                pCellInfo.m_ta = "not available";
                            }
                            pCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());
                            pCellInfo.m_networkType = "LTE";
                            pCellInfo.m_lte_available = true;
                            zNeighborCellInfos.add(pCellInfo);
                        }
                    }
                    // 5G NR Cell Info
                    if (info instanceof CellInfoNr) {

                        zCellInfo.m_nr_available = true;

                        //Toast toast = Toast.makeText(this, "NR", Toast.LENGTH_SHORT);
                        //toast.show();

                        CellInfoNr cellInfo = (CellInfoNr) info;
                        CellSignalStrengthNr cellSignalStrength = (CellSignalStrengthNr) cellInfo.getCellSignalStrength();
                        CellIdentityNr cellIdentity = (CellIdentityNr) cellInfo.getCellIdentity();
                        // Registered
                        if (cellInfo.isRegistered()) {
                            zCellInfo.cellSignalStrengthString = cellSignalStrength.toString();

                            zCellInfo.m_nr_registered = true;

                            // CellInfoNR
                            zCellInfo.m_nci = cellIdentity.getNci();           // NR Cell Identity
                            zCellInfo.m_mcc = cellIdentity.getMccString();
                            zCellInfo.m_mnc = cellIdentity.getMncString();
                            zCellInfo.m_nr_arfcn = cellIdentity.getNrarfcn();
                            zCellInfo.m_pci = String.valueOf(cellIdentity.getPci());
                            zCellInfo.m_tac = String.valueOf(cellIdentity.getTac());
                            zCellInfo.m_asu = String.valueOf(cellSignalStrength.getAsuLevel());
                            zCellInfo.m_csi_rsrp = cellSignalStrength.getCsiRsrp();
                            zCellInfo.m_csi_rsrq = cellSignalStrength.getCsiRsrq();
                            zCellInfo.m_csi_sinr = cellSignalStrength.getCsiSinr();
                            zCellInfo.m_csi_rsrp_dbm = cellSignalStrength.getDbm();
                            zCellInfo.m_ss_rsrp = cellSignalStrength.getSsRsrp();
                            zCellInfo.m_ss_rsrq = cellSignalStrength.getSsRsrq();
                            zCellInfo.m_ss_sinr = cellSignalStrength.getSsSinr();
                            zCellInfo.m_nrlevel = cellSignalStrength.getLevel();
                            zCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());
                        } else {
                            Cell_Information pCellInfo = new Cell_Information();

                            pCellInfo.timestampCellInfo = timestampCellInfo;
                            pCellInfo.cellSignalStrengthString = cellSignalStrength.toString();

                            pCellInfo.m_nr_registered = true;

                            // CellInfoNR
                            pCellInfo.m_nci = cellIdentity.getNci();           // NR Cell Identity
                            pCellInfo.m_mcc = cellIdentity.getMccString();
                            pCellInfo.m_mnc = cellIdentity.getMncString();
                            pCellInfo.m_nr_arfcn = cellIdentity.getNrarfcn();
                            pCellInfo.m_pci = String.valueOf(cellIdentity.getPci());
                            pCellInfo.m_tac = String.valueOf(cellIdentity.getTac());
                            pCellInfo.m_asu = String.valueOf(cellSignalStrength.getAsuLevel());
                            pCellInfo.m_csi_rsrp = cellSignalStrength.getCsiRsrp();
                            pCellInfo.m_csi_rsrq = cellSignalStrength.getCsiRsrq();
                            pCellInfo.m_csi_sinr = cellSignalStrength.getCsiSinr();
                            pCellInfo.m_csi_rsrp_dbm = cellSignalStrength.getDbm();
                            pCellInfo.m_ss_rsrp = cellSignalStrength.getSsRsrp();
                            pCellInfo.m_ss_rsrq = cellSignalStrength.getSsRsrq();
                            pCellInfo.m_ss_sinr = cellSignalStrength.getSsSinr();
                            pCellInfo.m_nrlevel = cellSignalStrength.getLevel();
                            pCellInfo.m_rsrp_dbm = String.valueOf(cellSignalStrength.getDbm());
                            pCellInfo.m_networkType = "NR";
                            pCellInfo.m_nr_available = true;
                            zNeighborCellInfos.add(pCellInfo);

                        }
                    }

                }
            }
            Log.d(TAG_MEASURE_SERVICE, "filter cellinfos and sort ready");
            zCellInfo.m_nr_registered = isNRConnected(tm);
            zCellInfo.m_nr_available = isNRAvailable(tm);
            if (zCellInfo.m_nr_available && zCellInfo.m_nr_registered) {
                zCellInfo.m_networkType = "NR";
            }
            Log.d(TAG_MEASURE_SERVICE, "check ntwrkstate ready");
        }

        //Wenn nicht im Netz registriert, dann gibt es auch keinen Netzwerktyp:
        //z.B. bei 5G abvailable und notRegistered wird sonst LTE zureck gegeben
        if (!isRegistered()) {
            zCellInfo.m_networkType = "";
        }
    }

    public boolean isNRConnected(TelephonyManager telephonyManager) {
        try {
            Object obj = Class.forName(telephonyManager.getClass().getName())
                    .getDeclaredMethod("getServiceState", new Class[0]).invoke(telephonyManager);
            if (obj == null) {
                return false;
            }
            // try extracting from string
            String serviceState = obj.toString();
            boolean is5gActive = serviceState.contains("nrState=CONNECTED") ||
                    serviceState.contains("nsaState=5") ||
                    (serviceState.contains("EnDc=true") &&
                            serviceState.contains("5G Allocated=true"));
            //LTE connected: datareg = 0 und nr false und endc false
            //5g coonected: datareg= 0 und nr true und endc true
            boolean is5gAvail = serviceState.contains("isNrAvailable = true") || serviceState.contains("isEnDcAvailable = true");
            boolean isConnected = serviceState.contains("mDataRegState=0") && (serviceState.contains("nrState=CONNECTED") || serviceState.contains("nrState=NOT_RESTRICTED"));


            if (is5gActive || (is5gAvail && isConnected)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG_MEASURE_SERVICE, "can not detect if nr is connected: getServiceState");
        }
        return false;
    }

    public String getServiceStateString(TelephonyManager telephonyManager) {
        try {
            Object obj = Class.forName(telephonyManager.getClass().getName())
                    .getDeclaredMethod("getServiceState", new Class[0]).invoke(telephonyManager);

            if (obj == null) {
                return "";
            }
            return obj.toString();

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG_MEASURE_SERVICE, "can not get serviceState String" );
        }
        return "";
    }

    public String getSignalStrengthString(TelephonyManager telephonyManager) {
        // try extracting from string
        Object obj2;
        try {
            obj2 = Class.forName(telephonyManager.getClass().getName())
                    .getDeclaredMethod("getSignalStrength", new Class[0]).invoke(telephonyManager);
            if (obj2 != null) {
                return obj2.toString();
            } else {
                return "";
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            Log.d(TAG_MEASURE_SERVICE, "Can not get SignalStrengthString: illegalAccess,InvoctionTarget...");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG_MEASURE_SERVICE, "Can not get SignalStrengthString: ClassNotFoundException");
        }
        return "";
    }

    public boolean isNRAvailable(TelephonyManager telephonyManager) {
        try {
            Object obj = Class.forName(telephonyManager.getClass().getName())
                    .getDeclaredMethod("getServiceState", new Class[0]).invoke(telephonyManager);
            if (obj == null) {
                return false;
            }

            String serviceState = getServiceStateString(telephonyManager);
            boolean is5gActive = serviceState.contains("nrState=CONNECTED") ||
                    serviceState.contains("nsaState=5") ||
                    (serviceState.contains("EnDc=true") ||
                            serviceState.contains("5G Allocated=true"));
            //LTE connected: datareg = 0 und nr false und endc false
            //5g coonected: datareg= 0 und nr true und endc true
            boolean is5gAvail = serviceState.contains("isNrAvailable = true") || serviceState.contains("isEnDcAvailable = true");

            //Wenn connected ist auch available
            if (is5gActive || is5gAvail) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG_MEASURE_SERVICE, "Can not detect if 5G is available getServiceState");
        }
        return false;
    }
}
