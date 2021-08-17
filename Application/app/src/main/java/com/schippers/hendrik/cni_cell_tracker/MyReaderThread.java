package com.schippers.hendrik.cni_cell_tracker;

import java.io.BufferedReader;
import java.io.IOException;

public class MyReaderThread extends Thread {

    boolean stopOperation = false;
    private final BufferedReader erReader;
    private final BufferedReader reader;
    final StringBuilder iperf3OutputStream;
    final StringBuilder iperf3ErrorStream;

    public MyReaderThread(final BufferedReader outReader, final BufferedReader errorReader, final StringBuilder outString, final StringBuilder errorString) {
        erReader = errorReader;
        reader = outReader;
        iperf3OutputStream = outString;
        iperf3ErrorStream = errorString;
    }

    @Override
    public void run() {
        String line1 = "";
        String line2 = "";
        while (line1 != null || line2 != null) {
            try {
                if ((line1 = reader.readLine()) != null) {
                    iperf3OutputStream.append(line1);
                    iperf3OutputStream.append("\r\n");
                }
                if ((line2 = erReader.readLine()) != null) {
                    iperf3ErrorStream.append(line2);
                    iperf3ErrorStream.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                line1 = null;
                line2 = null;
            }
        }
    }

}
