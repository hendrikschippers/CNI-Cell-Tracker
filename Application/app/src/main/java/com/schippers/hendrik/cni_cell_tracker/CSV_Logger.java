package com.schippers.hendrik.cni_cell_tracker;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/*
This class writes the obtained measurement data to files.
 */
public class CSV_Logger {
    final Context zContext;
    private static final String TAG_CSV_LOGGER = "CSV_LOGGER";

    public CSV_Logger(Context pContext) {
        zContext = pContext;
    }

    public static long getNextMeasurementIndex(){
        Date date = android.icu.util.Calendar.getInstance().getTime();
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());
        File filesDir = Environment.getExternalStorageDirectory();
        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "IperfTransferLog" + File.separator);
        dir = new File(dir, dfDay.format(date));
        File[] files = dir.listFiles();
        if(files == null){
            return 0;
        }else{
            if(files.length > 2)
                return files.length-2;
            else
                return 0;
        }
    }
    /*
    Writes the speed-test results to a dedicated file. This is done to preserve data in the case of lost data in the conversion process to .csv.
     */
    public void writeSpeedtestLog(IperfSpeedtest.IperfCommandResults commandResults, long p_count) {
        //Save Speed-test results to own file
        if (commandResults != null && commandResults.errors != null && commandResults.command != null && commandResults.result != null) {
            saveToSDCard(p_count + "\n" + commandResults.errors + "\n" + commandResults.command + "\n\n" + commandResults.result);
        } else {
            if (commandResults != null) {
                StringBuilder saveText = new StringBuilder();
                saveText.append(p_count).append("\n");
                if (commandResults.errors != null) {
                    saveText.append(commandResults.errors);
                } else {
                    saveText.append("null");
                }
                saveText.append("\n");
                if (commandResults.command != null) {
                    saveText.append(commandResults.command);
                } else {
                    saveText.append("null");
                }
                saveText.append("\n\n");
                if (commandResults.result != null) {
                    saveText.append(commandResults.result);
                } else {
                    saveText.append("null");
                }
                saveToSDCard(saveText.toString());
            } else {
                saveToSDCard("null" + "\n" + "null" + "\n\n" + "null");
            }
        }
        //Save speed-test result and network info to csv.
        SpeedtestData spTData = parseSpeedtestData(commandResults);
        saveSpeedtestDataToCSV(spTData, p_count);
    }

    /*
    Saves the parsed speedtest data to a .csv file.
     */
    public void saveSpeedtestDataToCSV(SpeedtestData pData, long pCount) {
        // Current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));


        Date date = android.icu.util.Calendar.getInstance().getTime();
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());

        File filesDir = Environment.getExternalStorageDirectory();
        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "IperfTransferLog" + File.separator);
        dir = new File(dir, dfDay.format(date));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Iperf Result CSV", Toast.LENGTH_LONG).show();
            }
        }
        File fileEnd = new File(dir, "IperfLog_" + dfDay.format(date) + ".csv");
        File fileIntervals = new File(dir, "IperfIntervalLog_" + dfDay.format(date) + ".csv");
        writeToCSV(fileEnd, "counter;" + pData.getEndResultCSVHeader() + "\n", pCount + ";" + pData.getEndResultCSVString() + "\n");
        writeToCSV(fileIntervals, pData.getIntervalsCSVHeader(), pData.intervalsToCSVString(pCount));

    }

    /*
    Writes the data pData to the file pTarget and adds the header pHeader if the file does not
    exist already. If the file exists, pData is appended to the file.
     */
    private void writeToCSV(File pTarget, String pHeader, String pData) {
        if (pTarget.exists() && pTarget.isFile()) {
            try {
                FileOutputStream fosXML = new FileOutputStream(pTarget, true);
                fosXML.write(pData.getBytes(Charset.defaultCharset()));
                fosXML.close();
            } catch (IOException e) {
                Toast.makeText(zContext.getApplicationContext(), "Error adding data to  Speedtest CSV to file", Toast.LENGTH_LONG).show();
                Log.d(TAG_CSV_LOGGER, "Error adding data to speedtest csv file" );

                e.printStackTrace();
            }
        } else {
            try {
                if (pTarget.createNewFile()) {
                    //create file and write header + data
                    try {
                        FileOutputStream fosXML = new FileOutputStream(pTarget);
                        fosXML.write(pHeader.getBytes(Charset.defaultCharset()));
                        fosXML.write(pData.getBytes(Charset.defaultCharset()));
                        fosXML.close();
                    } catch (IOException e) {
                        Toast.makeText(zContext.getApplicationContext(), "Error writing to new Speedtes CSV file", Toast.LENGTH_LONG).show();
                        Log.d(TAG_CSV_LOGGER, "Error writing to new Speed-test CSV file" );
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(zContext.getApplicationContext(), "Failed creating new Speed-test CSV file", Toast.LENGTH_LONG).show();
                    Log.d(TAG_CSV_LOGGER, "Failed creating new Speed-test CSV file" );
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(zContext.getApplicationContext(), "Error creating new Speed-test CSV file", Toast.LENGTH_LONG).show();
                Log.d(TAG_CSV_LOGGER, "Error creating new Speed-test CSV file" );
            }
        }
    }

    /*
    Parses the JSON Output from Iperf and saves it into a SpeedtestData instance.
     */
    public SpeedtestData parseSpeedtestData(IperfSpeedtest.IperfCommandResults pIperfResults) {
        SpeedtestData stData = new SpeedtestData();

        //If there are errors in the measurement: Save as failed
        if (pIperfResults == null || !pIperfResults.errors.equals("") || (pIperfResults.errors.equals("") && pIperfResults.result.equals(""))) {
            stData.receiver_Throughput = -1;
            stData.receiver_packetlossPercentage = 100.0;
            stData.receiver_totalSentBytes = 0;
            stData.receiver_lostPackets = 0;
            stData.receiver_totalPackets = 0;
            stData.receiver_Time = 0;
            stData.sender_Throughput = -1;
            stData.sender_packetLossPercentage = 100.0;
            stData.sender_totalSentBytes = 0;
            stData.sender_lostPackets = 0;
            stData.receiver_Time = 0;
            stData.fileSize = -1;
            stData.protocol = "";
            stData.direction = "";
            stData.sender_retransmits = -1;
            if(pIperfResults != null) {
                stData.parallelConnections = pIperfResults.parallelConnections;
            }
            return stData;
        }
        stData.processTimeout = pIperfResults.processTimeout;
        stData.timestamp_start = pIperfResults.timestamp_start;
        stData.timestamp_end = pIperfResults.timestamp_end;
        stData.parallelConnections = pIperfResults.parallelConnections;
        stData.fileSize = pIperfResults.fileSize;
        stData.protocol = (pIperfResults.useUDP ? "UDP" : "TCP");
        stData.direction = (pIperfResults.download ? "download" : "upload");

        /*
         Parsing _Iperf Intervals:
         These intervals are generated while the speed-test is run.
         */
        JSONArray intervalParse;

        try {
            intervalParse = new JSONObject(pIperfResults.result).getJSONArray("intervals");

            stData.intervals = new IperfSpeedtest.IperfStream[intervalParse.length()];
            //iterate over all intervals
            for (int i = 0; i < intervalParse.length(); i++) {
                //Get sum result of every interval existing of n parallel streams
                JSONObject pInterval = intervalParse.getJSONObject(i).getJSONObject("sum");
                stData.intervals[i] = new IperfSpeedtest.IperfStream();
                stData.intervals[i].start = pInterval.getDouble("start");
                stData.intervals[i].end = pInterval.getDouble("end");
                stData.intervals[i].duration = pInterval.getDouble("seconds");
                stData.intervals[i].bytes = pInterval.getLong("bytes");
                stData.intervals[i].bits_per_second = pInterval.getDouble("bits_per_second");
                if (pInterval.has("packets")) {
                    stData.intervals[i].packets_or_retransmits = pInterval.getLong("packets");
                } else {
                    if (pInterval.has("retransmits")) {
                        stData.intervals[i].packets_or_retransmits = pInterval.getLong("retransmits");
                    } else {
                        stData.intervals[i].packets_or_retransmits = -1;
                    }
                }
                if(pInterval.has("lost_packets")){
                    stData.intervals[i].lostPackets = pInterval.getLong("lost_packets");
                }else{
                    stData.intervals[i].lostPackets = -1;
                }
                stData.intervals[i].omitted = pInterval.getBoolean("omitted");
                stData.intervals[i].sender = pInterval.getBoolean("sender");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG_CSV_LOGGER, "JSON Exception at intervals parse" );
        }
        /*
            Parse the servers output
         */
        JSONArray serverIntervalParse;
        try {
            serverIntervalParse = new JSONObject(pIperfResults.result).getJSONObject("server_output_json").getJSONArray("intervals");

            stData.ServerIntervals = new IperfSpeedtest.IperfStream[serverIntervalParse.length()];
            //iterate over all intervals
            for (int i = 0; i < serverIntervalParse.length(); i++) {
                //Get sum result of every interval existing of n parallel streams
                JSONObject pInterval = serverIntervalParse.getJSONObject(i).getJSONObject("sum");
                stData.ServerIntervals[i] = new IperfSpeedtest.IperfStream();
                stData.ServerIntervals[i].start = pInterval.getDouble("start");
                stData.ServerIntervals[i].end = pInterval.getDouble("end");
                stData.ServerIntervals[i].duration = pInterval.getDouble("seconds");
                stData.ServerIntervals[i].bytes = pInterval.getLong("bytes");
                stData.ServerIntervals[i].bits_per_second = pInterval.getDouble("bits_per_second");

                if (pInterval.has("packets")) {
                    stData.ServerIntervals[i].packets_or_retransmits = pInterval.getLong("packets");
                } else {
                    if (pInterval.has("retransmits")) {
                        stData.ServerIntervals[i].packets_or_retransmits = pInterval.getLong("retransmits");
                    } else {
                        stData.ServerIntervals[i].packets_or_retransmits = -1;
                    }
                }
                if(pInterval.has("lost_packets")){
                    stData.ServerIntervals[i].lostPackets = pInterval.getLong("lost_packets");
                }else{
                    stData.ServerIntervals[i].lostPackets = -1;
                }
                stData.ServerIntervals[i].omitted = pInterval.getBoolean("omitted");
                stData.ServerIntervals[i].sender = pInterval.getBoolean("sender");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG_CSV_LOGGER, "JSON Exception at intervals parse" );
        }

        /*
        Parsing the end-results
         */
        JSONObject jsonParse = null;
        try {
            jsonParse = new JSONObject(pIperfResults.result);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG_CSV_LOGGER, "JSON Exception at results string parsing" );
        }
        if (jsonParse == null) {
            return stData;
        }
        JSONObject endEvaluation = null;
        if (pIperfResults.useUDP) {
            if (pIperfResults.download) {
                /*
                        UDP DOWNLOAD
                 */
                try {
                    endEvaluation = jsonParse.getJSONObject("end").getJSONObject("sum");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (endEvaluation != null) {

                    try {
                        //Sender info
                        stData.receiver_Time = endEvaluation.getDouble("seconds");
                        stData.rawThroughput = endEvaluation.getDouble("bits_per_second"); //Bits per second without losses
                        stData.sender_lostPackets = endEvaluation.getInt("lost_packets"); // lost packets
                        stData.sender_totalSentBytes = endEvaluation.getInt("bytes");
                        stData.sender_totalPackets = endEvaluation.getInt("packets");
                        stData.sender_packetLossPercentage = endEvaluation.getDouble("lost_percent");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                try {
                    //Receiver Info
                    endEvaluation = jsonParse.getJSONObject("end").getJSONArray("streams").getJSONObject(0).getJSONObject("udp");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (endEvaluation != null) {
                    try {
                        stData.sender_Time = endEvaluation.getDouble("seconds");
                        stData.receiver_Throughput = endEvaluation.getDouble("bits_per_second");
                        stData.receiver_jitter = endEvaluation.getDouble("jitter_ms");
                        stData.receiver_lostPackets = endEvaluation.getInt("lost_packets");
                        stData.receiver_totalPackets = endEvaluation.getInt("packets");
                        stData.receiver_outOfOrderPackets = endEvaluation.getInt("out_of_order");
                        stData.receiver_packetlossPercentage = endEvaluation.getInt("lost_percent");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                if (stData.sender_Time != 0) {
                    stData.sender_Throughput = (stData.sender_totalSentBytes * 8 * (100 - stData.sender_packetLossPercentage) * 0.01) / stData.sender_Time; //Bytes per second
                } else {
                    stData.sender_Throughput = -1;
                }
                long sumBytes = 0;
                double sumDuration = 0;
                int sumPackets = 0;
                int sumLost = 0;
                for (int i = 0; i < stData.intervals.length; i++) {
                    //Get the sum result of every interval existing of n parallel streams
                    sumBytes += stData.intervals[i].bytes;
                    sumDuration += stData.intervals[i].duration;
                    sumPackets += stData.intervals[i].packets_or_retransmits;
                    sumLost += stData.intervals[i].lostPackets;
                }
                stData.receiver_Throughput = (((double)sumBytes)/sumDuration)*8.0*(1.0-(double)sumLost/(double)sumPackets);
                stData.receiver_totalPackets = sumPackets;
                stData.receiver_lostPackets = sumLost;
                stData.receiver_totalSentBytes = (int)sumBytes;

            } else {
                /*
                        UDP UPLOAD
                 */
                try {
                    endEvaluation = jsonParse.getJSONObject("end");
                    JSONArray test = endEvaluation.getJSONArray("streams");
                    endEvaluation = test.getJSONObject(0).getJSONObject("udp");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (endEvaluation != null) {
                    try {
                        stData.sender_Time = endEvaluation.getDouble("seconds");
                        stData.sender_lostPackets = endEvaluation.getInt("lost_packets"); // lost packets
                        stData.sender_totalSentBytes = endEvaluation.getInt("bytes");
                        stData.sender_totalPackets = endEvaluation.getInt("packets");
                        stData.sender_packetLossPercentage = endEvaluation.getDouble("lost_percent");
                        //Have to recalculate because of the included sleep-time in the modified iperf version
                        stData.rawThroughput = (double)stData.sender_totalSentBytes/stData.sender_Time*8.0;
                        //stData.rawThroughput = endEvaluation.getDouble("bits_per_second"); //Bits per second without lost packets

                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                    stData.sender_Throughput = (stData.sender_totalSentBytes * (100 - stData.sender_packetLossPercentage) * 0.01) / stData.sender_Time; //Bytes per second

                }
                long sumBytes = 0;
                double sumDuration = 0;
                int sumPackets = 0;
                int sumLost = 0;
                for (int i = 0; i < stData.intervals.length; i++) {
                    //Get the sum result of every interval existing of n parallel streams
                    sumBytes += stData.intervals[i].bytes;
                    sumDuration += stData.intervals[i].duration;
                    sumPackets += stData.intervals[i].packets_or_retransmits;
                    if(stData.intervals[i].lostPackets != -1) {
                        sumLost += stData.intervals[i].lostPackets;
                    }else{
                        sumLost = -1;
                    }
                }
                if(sumLost == -1){
                    sumLost = stData.sender_lostPackets;
                }
                stData.sender_Throughput = (((double)sumBytes)/sumDuration)*8.0*(1.0-(double)sumLost/(double)sumPackets);
                stData.sender_totalPackets = sumPackets;
                stData.sender_lostPackets = sumLost;
                stData.sender_totalSentBytes = (int)sumBytes;

                // Receiver Throughput with server data
                sumBytes = 0;
                sumDuration = 0;
                sumPackets = 0;
                sumLost = 0;
                for (int i = 0; i < stData.ServerIntervals.length; i++) {
                    //Get sum result of every interval existing of n parallel streams
                    sumBytes += stData.ServerIntervals[i].bytes;
                    sumDuration += stData.ServerIntervals[i].duration;
                    sumPackets += stData.ServerIntervals[i].packets_or_retransmits;
                    if(stData.ServerIntervals[i].lostPackets != -1) {
                        sumLost += stData.ServerIntervals[i].lostPackets;
                    }else{
                        sumLost = -1;
                    }
                }
                if(sumLost == -1){
                    sumLost = stData.sender_lostPackets;
                }
                stData.receiver_totalSentBytes = (int)sumBytes;
                stData.receiver_Throughput = (((double)sumBytes)/sumDuration)*8.0*(1.0-(double)sumLost/(double)sumPackets);
                stData.receiver_totalPackets = sumPackets;
                stData.receiver_lostPackets = sumLost;
            }

        } else {
           /*
           TCP UPLOAD and Download
            */
            try {
                endEvaluation = jsonParse.getJSONObject("end").getJSONObject("sum_received");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "JSON Exception at results/end/sum_received parsing" );
            }


            if (endEvaluation != null) {
                try {
                    //receiver
                    stData.receiver_Time = endEvaluation.getDouble("seconds") ;
                    stData.sender_Time = endEvaluation.getDouble("seconds");
                    stData.receiver_totalSentBytes = endEvaluation.getInt("bytes");
                    stData.receiver_Throughput = (double)stData.receiver_totalSentBytes/stData.receiver_Time*8.0;///endEvaluation.getDouble("bits_per_second");
                    //Sender
                    endEvaluation = jsonParse.getJSONObject("end").getJSONObject("sum_sent");

                    stData.sender_Time = endEvaluation.getDouble("seconds");

                    stData.sender_totalSentBytes = endEvaluation.getInt("bytes");
                    stData.sender_Throughput = (double)stData.sender_totalSentBytes/stData.sender_Time*8.0;
                    //stData.sender_Throughput = endEvaluation.getDouble("bits_per_second");
                    stData.sender_retransmits = endEvaluation.getInt("retransmits");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG_CSV_LOGGER, "JSON Exception at endEvaluation parsing individual data" );
                }
            }


            stData.rawThroughput = -1;
            stData.receiver_packetlossPercentage = 0;
            stData.sender_packetLossPercentage = 0;
            stData.sender_totalPackets = -1;
            stData.receiver_totalPackets = -1;

            //If server results are available and reverse Mode is selected
            // use the following information provided by the server:
            // "max_snd_cwnd", "max_rtt", "min_rtt", "mean_rtt"
            // These results are not available at the client if the client is the receiver.
            //If the client is the sender, these results are available at client
            JSONArray senderStreams = null;
            if (pIperfResults.download) {
                try {
                    senderStreams = jsonParse.getJSONObject("server_output_json").getJSONObject("end").getJSONArray("streams");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG_CSV_LOGGER, "JSON Exception at server_output_json parsing" );
                }
            } else {
                try {
                    senderStreams = jsonParse.getJSONObject("end").getJSONArray("streams");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG_CSV_LOGGER, "JSON Exception at server_output_json/end/streams parsing" );
                }
            }
            if (senderStreams != null) {
                stData.connections = new IperfSpeedtest.IperfConnection[senderStreams.length()];
                for (int i = 0; i < senderStreams.length(); i++) {
                    JSONObject pConnection;
                    try {
                        pConnection = senderStreams.getJSONObject(i).getJSONObject("sender");
                        stData.connections[i] = new IperfSpeedtest.IperfConnection();
                        //
                        stData.connections[i].start = pConnection.getDouble("start");
                        stData.connections[i].end = pConnection.getDouble("end");
                        stData.connections[i].duration = pConnection.getDouble("seconds");
                        stData.connections[i].bytes = pConnection.getLong("bytes");
                        stData.connections[i].bits_per_second = pConnection.getDouble("bits_per_second");
                        stData.connections[i].packets_or_retransmits = pConnection.getInt("retransmits");
                        stData.connections[i].tcp_max_snd_cwnd = pConnection.getLong("max_snd_cwnd"); //congestion window
                        stData.connections[i].tcp_max_rtt = pConnection.getLong("max_rtt");
                        stData.connections[i].tcp_min_rtt = pConnection.getLong("min_rtt");
                        stData.connections[i].tcp_mean_rtt = pConnection.getLong("mean_rtt");
                        stData.connections[i].sender = pConnection.getBoolean("sender");

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG_CSV_LOGGER, "JSON Exception at streams json parsing individual data" );
                    }
                }
            }


        }

        //Parse Server Intervals for retransmits/lostPackets if download
        if (pIperfResults.download) {
            long sumRetransmits = 0;
            try {
                intervalParse = jsonParse.getJSONObject("server_output_json").getJSONArray("intervals");

                //iterate over all intervals
                for (int i = 0; i < intervalParse.length(); i++) {
                    //Get sum result of every interval existing of n parallel streams
                    JSONObject pInterval = intervalParse.getJSONObject(i).getJSONObject("sum");

                    if (pInterval.has("retransmits")) {
                        sumRetransmits += pInterval.getLong("retransmits");

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "JSON Exception at server_output parse intervals" );
            }
            stData.intervals[0].packets_or_retransmits = sumRetransmits;
        }


        return stData;
    }

    /*
    Saves a given String of an iperf log to a file containing the current time and date in its name.
     */
    public void saveToSDCard(String pSaveText) {
        File filesDir = Environment.getExternalStorageDirectory();
        //File filesDir = getFilesDir();
        Date date = android.icu.util.Calendar.getInstance().getTime();
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());
        SimpleDateFormat dfTime = new SimpleDateFormat("HH_mm_ss_SSS", Locale.getDefault());

        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "IperfTransferLog" + File.separator);
        dir = new File(dir, dfDay.format(date));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Iperf Result CSV", Toast.LENGTH_LONG).show();
            }
        }
        File file = new File(dir, "IperfLog_" + dfTime.format(date) + ".txt");

        try {
            if (file.createNewFile()) {
                FileOutputStream fOs = null;
                try {
                    fOs = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(zContext.getApplicationContext(), "Can not find Speed-test logfile", Toast.LENGTH_LONG).show();
                    Log.d(TAG_CSV_LOGGER, "Can not find Speed-test logfile" );
                }
                if (fOs != null) {
                    try {
                        fOs.write(pSaveText.getBytes(Charset.defaultCharset()));
                        fOs.flush();
                        fOs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(zContext.getApplicationContext(), "Error cannot write to Speedtes logfile", Toast.LENGTH_LONG).show();
                        Log.d(TAG_CSV_LOGGER, "Can not write Speedtest logfile" );
                    }
                }
            } else {
                Toast.makeText(zContext.getApplicationContext(), "Can not create Speedtest file: already exists", Toast.LENGTH_LONG).show();
                Log.d(TAG_CSV_LOGGER, "Speedtest file already exists, can not write" );
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(zContext.getApplicationContext(), "Error can not create Speedtest file", Toast.LENGTH_LONG).show();
            Log.d(TAG_CSV_LOGGER, "Error can not create Speedtest file" );
        }
    }
    /*
        Saves the results of the ping log to a dedicated file.
     */
    public void writePingLogToCSV(ICMP_Ping.PingResult pPingResult, long pCount) {
        File filesDir = Environment.getExternalStorageDirectory();
        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "PingLog" + File.separator);
        Date date = android.icu.util.Calendar.getInstance().getTime();
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());
        dir = new File(dir, dfDay.format(date));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Iperf Result CSV", Toast.LENGTH_LONG).show();
            }
        }
        File fileEnd = new File(dir, "PingLog" + dfDay.format(date) + ".csv");
        if (pPingResult != null) {
            writeToCSV(fileEnd, "Counter;" + ICMP_Ping.PingResult.getCSVHeader() + "\n", pCount + ";" + pPingResult.getCSVData() + "\n");
        } else {
            writeToCSV(fileEnd, "\"Counter;" + ICMP_Ping.PingResult.getCSVHeader() + "\n", pCount + ";" + -1 + ";" + ICMP_Ping.PingResult.getCSVErrData() + "\n");
        }

    }
    /*
    Saves the network log to a csv file.
    pStartTime is used to decide if the data needs to be appended to an existing file or if a new file needs to be created
    m_count is used to give every measurement a unique ID referencing to the number of performed data rate measurements in this session
    m_csv_filename Sets the filename of the csv file
    m_cell_log  Tells if the cell log should also be saved
    m_gps_log  Tells if the gps log should also be saved
     */
    public synchronized void writeNetworkCSVLog(Date pStartTime, Cell_Information zCellInfo, long m_count, String m_csv_filename, boolean m_cell_log, boolean m_gps_log) {

        // Prepare csv string
        String header_string = "";
        String body_string = "";
        // Current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss:SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date cur_time = Calendar.getInstance().getTime();
        String cur_date = dateFormat.format(cur_time);
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());


        // CSV file
        File file_path = Environment.getExternalStorageDirectory();
        File dir = new File(file_path.getAbsolutePath(), "CNI_APP" + File.separator + "CellLogging" + File.separator);
        dir = new File(dir, dfDay.format(cur_time));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Iperf Result CSV", Toast.LENGTH_LONG).show();
            }
        }
        SimpleDateFormat timeFilename = new SimpleDateFormat("HH_mm_ss_SSS", Locale.getDefault());
        File file = new File(dir, m_csv_filename + "_" + timeFilename.format(pStartTime) + ".csv");

        //Raw String file
        File dirRawStringFile = new File(dir, "rawStrings");
        File rawStringFile = new File(dirRawStringFile, "rawStrings_" + timeFilename.format(cur_time) + ".txt");
        if (!dirRawStringFile.exists()) {
            if (!dirRawStringFile.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for raw String Result", Toast.LENGTH_LONG).show();
            }
        }
        FileOutputStream rawString_stream;
        try {
            rawString_stream = new FileOutputStream(rawStringFile, true);
            rawString_stream.write((cur_time.getTime() + "\n\n" + zCellInfo.serviceStateString + "\n\n" + zCellInfo.signalStrengthString).getBytes());
            rawString_stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG_CSV_LOGGER, "Error can not write rawString file" );
        }

        FileOutputStream file_stream;

        header_string += "Count;Date;Timestamp;";
        body_string += m_count + ";" + cur_date + ";" + cur_time.getTime() + ";";

        // Cell logging
        if (m_cell_log) {
            header_string += Cell_Information.getCellCSVHeader();
            body_string += zCellInfo.getCellCSVData();

            header_string += ";";
            body_string += ";";

        }
        // GPS log
        if (m_gps_log) {
            body_string += zCellInfo.getGPSCSVData();
            header_string += zCellInfo.getGPSCSVHeader();
        }

        header_string += "\n";
        body_string += "\n";

        // Log file already exists
        if (file.exists() && file.isFile() && file.canWrite()) {
            try {
                file_stream = new FileOutputStream(file, true);
                file_stream.write(body_string.getBytes());
                file_stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "can not append data to logfile" );
            }
        }

        // Create new log file
        else {
            try {
                file_stream = new FileOutputStream(file, false);
                file_stream.write(header_string.getBytes());
                file_stream.write(body_string.getBytes());
                file_stream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "Can not create new logfile" );
            }
        }
    }

    /*
    Writes a log consiting of the network, gps and data rate log. It is merged based on the time of the data rate measurement:
    For every data rate measurement, cell reference signals and gps informations are added if they are selected with m_cell_log,
    m_gps_log, m_transmit_test

     */
    public void writeCombindedCSVLog(boolean m_cell_log, boolean m_gps_log, boolean m_transmit_test, boolean m_ping_log, Cell_Information zCellInfo,
                                     long m_count, SessionService sessionService, IperfSpeedtest.IperfCommandResults commandResult, ICMP_Ping.PingResult pingResult, Date m_startTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault());

        File filesDir = Environment.getExternalStorageDirectory();
        File dir = new File(filesDir.getAbsolutePath(), "CNI_APP" + File.separator + "CombinedLog" + File.separator);
        dir = new File(dir, dfDay.format(m_startTime));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Combined Log", Toast.LENGTH_LONG).show();
            }
        }
        File file = new File(dir, "CombinedLog" + dfDay.format(m_startTime) + ".csv");
        String header = "count;";
        String result = "";
        result += m_count + ";";
        if (m_cell_log) {
            header += Cell_Information.getCellCSVHeader();
            result += zCellInfo.getCellCSVData();
        }
        if (m_gps_log) {
            if (m_cell_log) {
                header += ";";
                result += ";";
            }
            header += zCellInfo.getGPSCSVHeader();
            result += zCellInfo.getGPSCSVData();
        }
        if (m_transmit_test) {
            //Save speed-test result to csv. Add Network info
            SpeedtestData spTData = parseSpeedtestData(commandResult);
            if (m_gps_log || m_cell_log) {
                header += ";";
                result += ";";
            }
            header += spTData.getEndResultCSVHeader();
            result += spTData.getEndResultCSVString();
        }
        if (m_ping_log) {
            if (m_cell_log || m_gps_log || m_transmit_test) {
                header += ";";
                result += ";";
            }
            header += ICMP_Ping.PingResult.getCSVHeader();
            if (pingResult != null) {
                result += pingResult.getCSVData();
            } else {
                result += ICMP_Ping.PingResult.getCSVErrData();
            }

        }
        header += "\n";
        result += "\n";


        writeToCSV(file, header, result);


    }

    public void writeNeighboringCellCSVLog(ArrayList<Cell_Information> zNeighborCellInfos, long m_count, String neighboringCellLog,Date pStartTime) {
        //Prepare csv string
        String header_string;
        if(zNeighborCellInfos.isEmpty()){
            return;
        }
        header_string = "Count;Date;Timestamp;" +Cell_Information.getCellCSVHeader();
        header_string += "\n";
        StringBuilder body_string= new StringBuilder();
        // Current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss:SSS", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date cur_time = Calendar.getInstance().getTime();
        String cur_date = dateFormat.format(cur_time);
        SimpleDateFormat dfDay = new SimpleDateFormat("yyy_MM_dd", Locale.getDefault());


        // CSV file
        File file_path = Environment.getExternalStorageDirectory();
        File dir = new File(file_path.getAbsolutePath(), "CNI_APP" + File.separator + "CellLogging" + File.separator);
        dir = new File(dir, dfDay.format(cur_time));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Toast.makeText(zContext.getApplicationContext(), "Can not create new Folder for Iperf Result CSV", Toast.LENGTH_LONG).show();
            }
        }
        SimpleDateFormat timeFilename = new SimpleDateFormat("HH_mm_ss_SSS", Locale.getDefault());
        File file = new File(dir, neighboringCellLog + "_" + timeFilename.format(pStartTime) + ".csv");


        FileOutputStream file_stream;
        for (int i = 0; i < zNeighborCellInfos.size(); i++) {

            body_string.append(m_count).append(";").append(cur_date).append(";").append(zNeighborCellInfos.get(i).timestampCellInfo).append(";");
            body_string.append(zNeighborCellInfos.get(i).getCellCSVData());
            body_string.append("\n");

        }

        // Log file already exists
        if (file.exists() && file.isFile() && file.canWrite()) {
            try {
                file_stream = new FileOutputStream(file, true);
                file_stream.write(body_string.toString().getBytes());
                file_stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "can not append to logfile 2" );
            }
        }

        // Create new log file
        else {
            try {
                file_stream = new FileOutputStream(file, false);
                file_stream.write(header_string.getBytes());
                file_stream.write(body_string.toString().getBytes());
                file_stream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG_CSV_LOGGER, "can not create logfile 2" );
            }
        }
    }
}
