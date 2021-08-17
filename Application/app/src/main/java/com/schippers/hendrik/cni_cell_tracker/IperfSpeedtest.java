package com.schippers.hendrik.cni_cell_tracker;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class IperfSpeedtest {
    private final Context zContext;
    private static final String TAG_IPERF_SPEEDTEST = "IPERF_SPEEDTEST";
    final String appFileDirectory;
    final File zCacheDir;

    public static class IperfStream {
        public long lostPackets;
        double start, end, duration, bits_per_second;
        long bytes, packets_or_retransmits;
        boolean sender, omitted;
    }
    //max congestion window, max round-trip-time, min round-trip-time, mean round-trip-time
    public static class IperfConnection extends IperfStream {
        long tcp_max_snd_cwnd, tcp_max_rtt, tcp_min_rtt, tcp_mean_rtt;
    }

    public enum transferFileSize {
        filesize_100KB(104858), filesize_500KB(524288), filesize_1MB(1048576), filesize_2MB(2097152), filesize_3MB(3145728), filesize_4MB(4194304), filesize_5MB(5242880), filesize_6MB(6291456), filesize_7MB(7340032), filesize_8MB(8388608), filesize_9MB(9437184), filesize_10MB(10485760);
        public final int size;

        transferFileSize(int size) {
            this.size = size;
        }
    }

    public static class IperfCommandResults {
        public String command;
        public String result;
        public String errors;
        long timestamp_start;
        long timestamp_end;
        boolean processTimeout;
        public boolean useUDP, download;
        public int parallelConnections;
        int fileSize;
    }

    public IperfSpeedtest(Context pContext) {
        zContext = pContext;
        appFileDirectory = zContext.getApplicationInfo().nativeLibraryDir;
        zCacheDir = zContext.getCacheDir();
    }

    public IperfCommandResults udpUploadLargeTest(String pServerIp, int pServerPort, int pParallelConnections) {
        return startIperf3(pServerIp, pServerPort, 0, 1, true, transferFileSize.filesize_10MB, false, true);
    }

    public IperfCommandResults measureSpeedWithRandomFilesAndModesAndReturn(String pServerIp, int pServerPort, int pParallelConnections) {
        Random rand = new Random();

        boolean useUDP = rand.nextBoolean();
        int fileSizeIndex;
        //If UDP is used, only use file-sizes larger than 1MByte. That is done because of time measurement problems with smaller file sizes
        if (useUDP) {
            fileSizeIndex = rand.nextInt(transferFileSize.values().length - 3) + 3;
        } else {
            fileSizeIndex = rand.nextInt(transferFileSize.values().length);
        }
        boolean download = rand.nextBoolean();
        return startIperf3(pServerIp, pServerPort, 0, pParallelConnections, true, transferFileSize.values()[fileSizeIndex], download, useUDP);
    }

    public IperfCommandResults startIperf3(String pServerIp, int pServerPort, int pOmit, int pParallelCons, boolean pTransferFile, transferFileSize pFilesize, boolean pReverse, boolean pUseUDP) {
        Log.d(TAG_IPERF_SPEEDTEST, "Building iperf command");

        Runtime.getRuntime().gc();//For reasons of fear...
        if (pParallelCons < 1 || pParallelCons > 127) {
            //Toast.makeText(zContext.getApplicationContext(), "Wrong Iperf Parameters: too many/less parallel connections", Toast.LENGTH_LONG).show();
            Log.d(TAG_IPERF_SPEEDTEST, "Wrong Iperf Parameters: too many/less parallel connections");
            IperfSpeedtest.IperfCommandResults errorReturn = new IperfSpeedtest.IperfCommandResults();
            errorReturn.errors = "impossible amount of parallel connections";
            errorReturn.result = "";
            errorReturn.command = "";
            errorReturn.download = pReverse;
            errorReturn.fileSize = pFilesize.size;
            errorReturn.useUDP = pUseUDP;
            return errorReturn;
        }
        IperfSpeedtest.IperfCommandResults results = new IperfSpeedtest.IperfCommandResults();
        results.errors = "";
        if (pUseUDP) {
            results.parallelConnections = 1;
        } else {
            results.parallelConnections = pParallelCons;
        }


        //l is the minimum size to send. But the used value is 10 times larger as set by this option
        //n is the target count of bytes to sent. The real count of sent bytes is always slightly larger or equal depending on the buffer size

        String command = appFileDirectory + "/libIperf3.9.so -p 5201" +
                " -y " + zCacheDir +// "/data/user/0/" + getPackageName() + "/cache" + //Added this option to Iperf3.9 to specify cache location
                " -c " + pServerIp + //Client mode and set server ip
                " -p " + pServerPort + //set server port
                " -n " + pFilesize.size + //set amount of data to up/download
                " -O " + pOmit +  //set time of omitted packets in seconds
                " -J " +  //set Output to JSON format: returns more exact values and is more easy to parse
                " -i 0.1 " + //interval to generate intermediate results
                " --get-server-output" +
                " --connect-timeout 4000";

        if (!pUseUDP) {
            //Set Buffer length: Should not be too small--> performance impact. Should not be too large-->transmits too much
            //Iperf always sends the whole buffer
            command += " -l " + Math.max(1, Math.round(pFilesize.size / 200.0));
            //command += " -C \"cubic\" ";
            command += " -P " + pParallelCons;  //Number of parallel connections: maybe important for tcp slow-start mechanism
        } else {
            command += " -u -b 99999M"; //Do not restrict udp bandwidth

        }

        if (pReverse) {
            command += " -R "; //Sets the mode to download if set
        }
        if (!pReverse && pTransferFile) {
            //If it is a upload: send specific file
            File sdPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), zContext.getString(R.string.transmit_files_path));
            File transmitFile = new File(sdPath, "file_" + pFilesize.size);
            command += " -F " + transmitFile.getAbsolutePath();
        }

        Log.d(TAG_IPERF_SPEEDTEST, "about to execute iperf speed-test: ");

        //Test ProcessBuilder
        Process process = null;
        results.timestamp_start = Calendar.getInstance().getTime().getTime();
        try {
            //(new StringBuilder("doInBackground to run: ")).append(paramVarArgs[0]);
            process = (new ProcessBuilder()).command(command.split(" ")).redirectErrorStream(false).start();
        } catch (IOException e) {
            e.printStackTrace();
            if(process != null) {
                process.destroy();
                process = null;
            }
            //Toast.makeText(zContext.getApplicationContext(), "Error while Performing Speed-test", Toast.LENGTH_LONG).show();
            Log.d(TAG_IPERF_SPEEDTEST, "Error while staring Speed-test");
            results.processTimeout = false;
            results.result = "";
            results.errors = "IOException after staring Iperf Measurement";
            results.download = pReverse;
            results.command = command;
            results.fileSize = pFilesize.size;
            results.useUDP = pUseUDP;
            return results;
        } catch (Exception f) {
            Log.d(TAG_IPERF_SPEEDTEST, "Exception running Speed-test");
            results.errors += "Exception occurred running IPERF";
            f.printStackTrace();
            if(process!=null) {
                process.destroy();
                process = null;
            }
        }
        //Test if process could be started
        if (process == null) {
            results.processTimeout = false;
            results.result = "";
            results.errors = "Process is null";
            results.download = pReverse;

            results.command = command;

            results.fileSize = pFilesize.size;
            results.useUDP = pUseUDP;
            return results;
        }
        //Immediately start to read output from Process, to prevent buffer overflow. Read until process is dead or until timeout
        final BufferedReader erReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        final StringBuilder iperf3OutputStream = new StringBuilder();
        final StringBuilder iperf3ErrorStream = new StringBuilder();





        MyReaderThread bufferedReaderThread = new MyReaderThread(reader,erReader,iperf3OutputStream,iperf3ErrorStream);
        bufferedReaderThread.start();
        boolean stopWaiting = false;
        StringBuilder errorString = new StringBuilder();
        while(process.isAlive() && !stopWaiting) {
            try {
                process.waitFor(20, TimeUnit.SECONDS);
                stopWaiting = true;
            } catch (InterruptedException inE) {
                errorString.append("InterruptedException while waiting for Iperf Process\n");

            }
            catch(Exception f){
                Log.d(TAG_IPERF_SPEEDTEST, "Exception waiting for Speed-test");
                errorString.append("Exception while waiting for IPERF\n");
                f.printStackTrace();
            }
        }
        results.errors += errorString.toString();
        if(process.isAlive() && stopWaiting) {
            process.destroy();
            results.processTimeout = true;
        }
        //After process finished, wait some time to let the reader read all remaining data

        try {
            bufferedReaderThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(bufferedReaderThread.isAlive()) {
            bufferedReaderThread.interrupt();
        }


        try {
            erReader.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        results.result = iperf3OutputStream.toString();
        results.errors = iperf3ErrorStream.toString();
        if (results.processTimeout) {
            results.errors += "Timeout\n";
        }
        results.timestamp_end = Calendar.getInstance().getTime().getTime();


        //End Test Process Builder



        Runtime.getRuntime().gc(); //Out of fear

        results.download = pReverse;

        results.command = command;

        results.fileSize = pFilesize.size;
        results.useUDP = pUseUDP;

        return results;
    }

}
