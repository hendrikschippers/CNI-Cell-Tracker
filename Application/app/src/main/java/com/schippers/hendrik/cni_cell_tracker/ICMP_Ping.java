package com.schippers.hendrik.cni_cell_tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import android.util.Log;

public class ICMP_Ping {
    private static final String TAG_ICMP_PING = "Network.java";
    public static String pingError = null;


    /**
     * Ping a host and return error or Ping Value
     * <p>
     * Does not work in Android emulator and also delay by '1' second if host not pingable
     * In the Android emulator only ping to 127.0.0.1 works
     */
    public static PingResult ping(String host, PingResult result) {
        StringBuilder echo = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        Log.v(TAG_ICMP_PING, "About to ping using runtime.exec");
        Process proc = null;
        //android.icu.util.Calendar.getInstance().getTime();
        result.timestampStart = Calendar.getInstance().getTime().getTime();
        try {
            proc = runtime.exec("ping -c 4 -W 1 " + host);//+" -c 4 -W 1");// -d 1 -W 1
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(TAG_ICMP_PING, "can not run ping command");
        }
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.v(TAG_ICMP_PING, "can not wait for ping command");
        }
        result.timestampEnd = Calendar.getInstance().getTime().getTime();
        int exit = proc.exitValue();
        if (exit == 0) {
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            BufferedReader buffer = new BufferedReader(reader);
            String line = "";
            while (true) {
                try {
                    if ((line = buffer.readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(TAG_ICMP_PING, "can not read ping command result");
                }
                echo.append(line).append("\n");
            }
            return getPingStats(echo.toString(), result);
        } else {
            InputStreamReader reader = new InputStreamReader(proc.getErrorStream());
            BufferedReader buffer = new BufferedReader(reader);
            String line = "";
            while (true) {
                try {
                    if ((line = buffer.readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(TAG_ICMP_PING, "can not read ping command 2");
                }
                echo.append(line).append("\n");
            }
            pingError = "failed, exit = 1";
            return null;
        }
    }

    public static class PingResult {
        double minPing;
        double avgPing;
        double maxPing;
        double mdevPing;
        double packetLoss;
        boolean failed;
        long timestampStart;
        long timestampEnd;

        public PingResult() {
            minPing = -1;
            avgPing = -1;
            maxPing = -1;
            mdevPing = -1;
            packetLoss = -1;
            failed = true;
        }

        public static String getCSVHeader() {
            return "PingtimestampStart;PingtimestampEnd;minPing;avgPing;maxPing;meanDevPing;packetLoss;successfull";
        }

        public static String getCSVErrData() {
            return "-1;-1;-1;-1;-1;-1;100;false";
        }

        public String getCSVData() {
            return timestampStart + ";" + timestampEnd + ";" + minPing + ";" + avgPing + ";" + maxPing + ";" + mdevPing + ";" + packetLoss + ";" + !failed;
        }
    }

    /**
     * getPingStats interprets the text result of a Linux ping command
     * <p>
     * Set pingError on error and return null
     * <p>
     * http://en.wikipedia.org/wiki/Ping
     * <p>
     * PING 127.0.0.1 (127.0.0.1) 56(84) bytes of data.
     * 64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.251 ms
     * 64 bytes from 127.0.0.1: icmp_seq=2 ttl=64 time=0.294 ms
     * 64 bytes from 127.0.0.1: icmp_seq=3 ttl=64 time=0.295 ms
     * 64 bytes from 127.0.0.1: icmp_seq=4 ttl=64 time=0.300 ms
     * <p>
     * --- 127.0.0.1 ping statistics ---
     * 4 packets transmitted, 4 received, 0% packet loss, time 0ms
     * rtt min/avg/max/mdev = 0.251/0.285/0.300/0.019 ms
     * <p>
     * PING 192.168.0.2 (192.168.0.2) 56(84) bytes of data.
     * <p>
     * --- 192.168.0.2 ping statistics ---
     * 1 packets transmitted, 0 received, 100% packet loss, time 0ms
     * <p>
     * # ping 321321.
     * ping: unknown host 321321.
     * <p>
     * 1. Check if output contains 0% packet loss : Branch to success -> Get stats
     * 2. Check if output contains 100% packet loss : Branch to fail -> No stats
     * 3. Check if output contains 25% packet loss : Branch to partial success -> Get stats
     * 4. Check if output contains "unknown host"
     *
     * @param s      Inout String returned by ping method
     * @param result PingResult where the result should be saved in
     */
    public static PingResult getPingStats(String s, PingResult result) {
        if (s == null) {
            return result;
        }
        if (s.contains("unknown host")) {
            return result;
        }
        if (!s.contains("% packet loss")) {
            return result;
        }

        // Parse Packetloss from output String
        int lossStart = s.indexOf("received, ") + "received, ".length();
        if (lossStart != -1) {
            int lossEnd = s.indexOf("% ", lossStart);
            if (lossEnd > lossStart) {
                try {
                    result.packetLoss = Double.parseDouble(s.substring(lossStart, lossEnd));
                } catch (NullPointerException  | NumberFormatException e2) {

                    Log.v(TAG_ICMP_PING, "error parsing ping result: packetloss");
                    result.packetLoss = -1;
                    return result;
                }
            }
        }
        //Parse min/av/max/mdev from output String
        int valuesStart = s.indexOf("min/avg/max/mdev = ") + "min/avg/max/mdev = ".length();
        if (valuesStart != -1) {
            int valuesEnd = s.indexOf(" ms", valuesStart);
            if (valuesEnd > valuesStart) {
                String temp = s.substring(valuesStart, valuesEnd);
                String[] pingStats = temp.split("/");
                if (pingStats.length == 4) {
                    try {
                        result.minPing = Double.parseDouble(pingStats[0]);
                        result.avgPing = Double.parseDouble(pingStats[1]);
                        result.maxPing = Double.parseDouble(pingStats[2]);
                        result.mdevPing = Double.parseDouble(pingStats[3]);
                    } catch (NullPointerException e) {
                        Log.v(TAG_ICMP_PING, "error parsing ping result: min, avg, max, dev ping");
                        return result;
                    }
                } else {
                    return result;
                }
            } else {
                return result;
            }
        } else {
            return result;
        }

        result.failed = false;
        return result;
    }
}