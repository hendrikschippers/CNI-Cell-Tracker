package com.schippers.hendrik.cni_cell_tracker;

public class SpeedtestData {
    public boolean processTimeout;
    long timestamp_start;
    long timestamp_end;

    String protocol;
    String direction;
    int fileSize;
    int parallelConnections;
    //
    double receiver_Time;
    int receiver_lostPackets;
    int receiver_totalSentBytes;
    double receiver_Throughput;
    double sender_Time;
    int sender_lostPackets;
    int sender_totalSentBytes;
    double sender_Throughput;
    //UDP variables
    double rawThroughput;
    double sender_packetLossPercentage;
    double receiver_packetlossPercentage;
    double receiver_jitter;
    int sender_totalPackets;
    int receiver_totalPackets;
    int receiver_outOfOrderPackets;

    //TCP Variables
    int sender_retransmits;
    IperfSpeedtest.IperfStream[] intervals;
    IperfSpeedtest.IperfStream[] ServerIntervals;
    IperfSpeedtest.IperfConnection[] connections;

    public SpeedtestData() {
        processTimeout = false;
        timestamp_start= 0;
        timestamp_end= 0;
        protocol = "";
        direction = "";
        fileSize = -1;
        //
        sender_Time = -1;
        sender_lostPackets = -1;
        sender_totalSentBytes = -1;
        sender_Throughput = -1;
        receiver_Time = -1;
        receiver_lostPackets = -1;
        receiver_totalSentBytes = -1;
        receiver_Throughput = -1;
        //UDP variables
        rawThroughput = -1;
        sender_packetLossPercentage = -1;
        receiver_packetlossPercentage = -1;
        receiver_jitter = -1;
        sender_totalPackets = -1;
        receiver_totalPackets = -1;
        receiver_outOfOrderPackets = -1;
        //TCP Variables
        sender_retransmits = -1;
    }

    /*
    Returns the Speedtest data as comma-seperated values
     */
    public String getEndResultCSVString() {
        StringBuilder returnValue = new StringBuilder(timestamp_start + ";" +
                timestamp_end + ";" +
                protocol + ";" +
                direction + ";" +
                fileSize + ";" +
                sender_Time + ";" +
                sender_totalPackets + ";" +
                sender_lostPackets + ";" +
                sender_totalSentBytes + ";" +
                sender_Throughput + ";" +
                receiver_Time + ";" +
                receiver_totalPackets + ";" +
                receiver_lostPackets + ";" +
                receiver_totalSentBytes + ";" +
                receiver_Throughput + ";" +
                rawThroughput + ";" +
                sender_packetLossPercentage + ";" +
                receiver_packetlossPercentage + ";" +
                receiver_jitter + ";" +
                receiver_outOfOrderPackets + ";" +
                sender_retransmits + ";" +
                processTimeout + ";" +
                parallelConnections);
        if (connections != null) {
            // Now every connection
            for (int i = 0; i < parallelConnections; i++) {
                returnValue.append(";");
                returnValue.append(connections[i].tcp_max_snd_cwnd).append(";");
                returnValue.append(connections[i].tcp_max_rtt).append(";");
                returnValue.append(connections[i].tcp_min_rtt).append(";");
                returnValue.append(connections[i].tcp_mean_rtt).append(";");
                returnValue.append(connections[i].bits_per_second);
            }
        }else{
            for (int i = 0; i < parallelConnections; i++) {
                returnValue.append(";");
                returnValue.append(";");
                returnValue.append(";");
                returnValue.append(";");
                returnValue.append(";");
            }
        }
        return returnValue.toString();
    }

    /*
    Returns the Header of a csv File containing Speedtest data
     */
    public String getEndResultCSVHeader() {
        StringBuilder returnValue = new StringBuilder("timestampStart;" + "timestampEnd;" + "protocol;" + "direction;" + "filesize;" +
                "senderTime;" + "senderPackets;" + "senderLostPackets;" + "senderBytes;" + "senderThroughput;" +
                "receiverTime;" + "receiverPackets;" + "receiverLostPackets;" + "receiverBytes;" + "receiverThroughput;" +
                "rawThroughput;" + "senderPacketloss;" + "revceiverPacketloss;" + "receiverJitter;" + "receiverOutOfOrder;" +
                "senderRetransmits;" + "speedtestTimeout;" + "parallelStreams");

            // Now every connection
            for (int i = 0; i < parallelConnections; i++) {
                returnValue.append(";");
                returnValue.append("tcp_max_cwnd_").append(i).append(";");
                returnValue.append("tcp_max_rtt_").append(i).append(";");
                returnValue.append("tcp_min_rtt_").append(i).append(";");
                returnValue.append("tcp_mean_rtt_").append(i).append(";");
                returnValue.append("bits_per_second_").append(i);
            }

        return returnValue.toString();
    }

    public String intervalsToCSVString(long pCount) {
        if (intervals == null) {
            return "\n";
        }
        StringBuilder resultString = new StringBuilder();
        for (IperfSpeedtest.IperfStream interval : intervals) {
            resultString.append(pCount);
            resultString.append(";");
            resultString.append(timestamp_start);
            resultString.append(";");
            resultString.append(timestamp_end);
            resultString.append(";");
            resultString.append(protocol);
            resultString.append(";");
            resultString.append(direction);
            resultString.append(";");
            resultString.append(fileSize);
            resultString.append(";");
            resultString.append(interval.start);
            resultString.append(";");
            resultString.append(interval.end);
            resultString.append(";");
            resultString.append(interval.duration);
            resultString.append(";");
            resultString.append(interval.bytes);
            resultString.append(";");
            resultString.append(interval.bits_per_second);
            resultString.append(";");
            if (interval.packets_or_retransmits != -1) {
                resultString.append(interval.packets_or_retransmits);
            }

            resultString.append(";");
            resultString.append(interval.omitted);
            resultString.append(";");
            resultString.append(interval.sender);
            resultString.append(";");
            resultString.append("\n");
        }
        return resultString.toString();
    }


    public String getIntervalsCSVHeader() {
        return "count;timestampStart;timestampEnd;Protocol;direction;filesize_Byte;start;end;duration;bytes;bits_per_second;packets_retransmits;omitted;sender\n";
    }
}