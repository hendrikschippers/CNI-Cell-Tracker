package com.schippers.hendrik.cni_cell_tracker;

import java.io.Serializable;
import java.util.Arrays;

/*
This class saves all the cell information and gps data, that could be obtained using the API.
 */
public class Cell_Information implements Serializable {

    public static final String UNAVAILABLE = "2147483647"; //this number is returned if a value is not available

    public long timestampCellInfo = -1; //The timestamp, at which the cell info is obtained
    public String serviceStateString = "";
    public String signalStrengthString = "";
    // GPS variables
    public float m_accuracy;
    public double m_altitude;
    public long nanosecs;
    public double m_latitude;
    public double m_longitude;
    public String m_provider;
    public float m_speed;
    public float m_speedAccuracy;
    public long m_time; //GPS time
    public float m_verticalAccuracy;

    // Cell variables
    public String m_networkType = null;
    //Variables explaining which cell type is available
    public boolean m_gsm_available = false;
    public boolean m_wcdma_available = false;
    public boolean m_lte_available = false;
    public boolean m_nr_available = false;
    //Variables explaining which cell type is registered
    public boolean m_gsm_registered = false;
    public boolean m_wcdma_registered = false;
    public boolean m_lte_registered = false;
    public boolean m_nr_registered = false;
    public boolean m_nr_registerred_override = false;
    public String m_operator = null;

    public int m_EARFCN = 0; //represents the used frequency
    int[] m_Cell_Bandwidths = null;
    // LTE info
    public String m_ci = null;
    public String m_pci = null;
    public String m_tac = null;
    public String m_asu = null;
    public String m_cqi = null;
    public String m_rsrp = null;
    public String m_rsrq = null;
    public String m_rssi = null;
    public String m_rssnr = null;
    public String m_ta = null;
    public String m_bandwith = null;

    // LTE NR info
    public long m_nci = -1;   // NR Cell Identity
    public String m_mcc = null;
    public String m_mnc = null;
    public int m_nr_arfcn = -1;
    public int m_nrlevel = -1;

    public int m_ss_rsrp = -1;
    public int m_ss_rsrq = -100;
    public int m_ss_sinr = -1;

    // CellSingalStrengthLte
    public int m_csi_rsrp = -1;
    public int m_csi_rsrq = -100;
    public int m_csi_sinr = -1;
    public int m_csi_rsrp_dbm = -1;

    public String m_level = null;

    public String m_rsrp_dbm;
    public String cellSignalStrengthString;
    public boolean m_nr_registerred_override_mmWave = false;

    /*
    Resets the all the saved Reference Signals to their initial state.
    */
    public void resetCellValues() {
        m_EARFCN = 0;
        m_Cell_Bandwidths = null;
        timestampCellInfo = -1;
        serviceStateString = "";
        signalStrengthString = "";
        m_networkType = null;
        m_gsm_available = false;
        m_wcdma_available = false;
        m_lte_available = false;
        m_nr_available = false;
        m_gsm_registered = false;
        m_wcdma_registered = false;
        m_lte_registered = false;
        m_nr_registered = false;
        //Do not reset: changes on listener only
        //m_nr_registerred_override = false;
        m_operator = null;

        // LTE info
        m_ci = null;
        m_pci = null;
        m_tac = null;
        m_asu = null;
        m_cqi = null;
        m_rsrp = null;
        m_rsrq = null;
        m_rssi = null;
        m_rssnr = null;
        m_ta = null;
        m_bandwith = null;

        // LTE NR info
        m_nci = -1;   // NR Cell Identity
        m_mcc = null;
        m_mnc = null;
        m_nr_arfcn = -1;
        m_nrlevel = -1;

        // CellSingalStrengthLte
        m_csi_rsrp = -1;
        m_csi_rsrq = -100;
        m_csi_sinr = -1;
        m_csi_rsrp_dbm = -1;
        m_ss_rsrp = -1;
        m_ss_rsrq = -100;
        m_ss_sinr = -1;
        m_level = null;

        m_rsrp_dbm = null;
        cellSignalStrengthString = null;
    }

    /*
        Converts the GPS date into a .csv representation String.
     */
    public String getGPSCSVData() {
        return m_longitude + ";" + m_latitude + ";" + m_altitude + ";" + m_time +
                ";" + m_accuracy + ";"+ m_speed +";" + m_speedAccuracy;
    }

    /*
        Returns a empty .csv representation of the GPS data. Useful if no GPS tracking is done.
     */
    public static String getEmptyGPSCSVData() {
        return ";;;;";
    }

    /*
    Converts the Cell Reference Signals into a .csv representation String.
     */
    public String getCellCSVData() {
        String body_string = "";
        body_string += m_networkType + ";";

        body_string += getAvailableNetworks();
        body_string += ";";
        body_string += getRegisteredNetworks() + ";";

        body_string += m_ci + ";" + m_pci + ";" + m_tac +";"+m_EARFCN+ ";" + m_asu + ";" + m_cqi + ";" +
                m_rsrp + ";" + m_rsrq + ";" + m_rssi + ";" + m_rssnr + ";" + m_ta + ";" + m_bandwith +
                ";" + m_nci + ";" + m_mcc + ";" + m_mnc + ";" + m_nr_arfcn + ";" + m_csi_rsrp + ";" + m_csi_rsrq +
                ";" + m_csi_sinr + ";" + m_csi_rsrp_dbm +
                ";" + m_ss_rsrp + ";" + m_ss_rsrq + ";" + m_ss_sinr + ";" + m_nrlevel+";" +Arrays.toString(m_Cell_Bandwidths);

        return body_string;
    }
    /*
        Returns the csv Header for the GPS data.
     */
    public String getGPSCSVHeader() {
        return "Longitude;Latitude;Altitude;Time;Accuracy;velocity;velocityAccuracy";
    }
    /*
    Return the csv header for the cell data.
     */
    public static String getCellCSVHeader() {
        String header_string = "";
        header_string += "Type;Available;Registered;";
        //4G Data
        header_string += "CI;PCI;TAC;EARFCN/f;ASU;CQI;RSRP;RSRQ;RSSI;RSSNR;TA;Bandwith;";
        header_string += "NCI;MCC;MNC;ARFCN;CSI_RSRP;CSI_RSRQ;" +
                "CSI_SINR;CSI_RSRP_dbm;SS_RSRP;SS_RSRQ;SS_SINR;LEVEL;Bandwidths";
        return header_string;
    }
    /*
    Returns the available networks as a String.
     */
    public String getAvailableNetworks() {
        String available_cell_types = "";
        boolean type_set = false;

        if (m_gsm_available) {
            available_cell_types += "GSM";
            type_set = true;
        }
        if (m_wcdma_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "WCDMA";
            type_set = true;
        }
        if (m_lte_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "LTE";
            type_set = true;
        }
        if (m_nr_available) {
            if (type_set)
                available_cell_types += ", ";
            available_cell_types += "NR";
        }
        return available_cell_types;
    }
    /*

     */
    public boolean isRegistered() {
        return (m_gsm_registered || m_wcdma_registered || m_lte_registered || m_nr_registered);
    }
    /*
    Returns a list of registered networks as a string.
     */
    public String getRegisteredNetworks() {
        // Registered
        String registered_types = "";
        boolean type_registered = false;

        if (m_gsm_registered) {
            registered_types += "GSM";
            type_registered = true;
        }
        if (m_wcdma_registered) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "WCDMA";
            type_registered = true;
        }
        if (m_lte_registered) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "LTE";
            type_registered = true;
        }
        if (m_nr_registered || m_nr_registerred_override || m_nr_registerred_override_mmWave) {
            if (type_registered)
                registered_types += ", ";
            registered_types += "NR";
            if (m_nr_registerred_override_mmWave) {
                registered_types += "mmWave";
            }
            type_registered = true;
        }


        return registered_types;
    }
    /*
    Resets the GPS variables to their initial values.
     */
    public synchronized void resetGPSValues() {
        m_latitude = -1;
        m_longitude = -1;
        m_provider = "";
        m_time = -1;
        m_accuracy = -1;
        m_altitude = -1;
        m_speed = -1;
        m_speedAccuracy = -1;
        m_verticalAccuracy = -1;
        nanosecs = -1;
    }

}
