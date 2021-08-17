# CNI-Cell-Tracker
!!!! Work in Progress !!!!

The **CNI-Cell-Tracker** is an Android application that can be used for active and passive mobile network measurements. It is able to display and log network, mobility informations provided by the Android API. In addition to that, data rate measurements using **iPerf** and latency measurements can be periodically executed. The obatained data can then save to several comma-seperated-value(.csv) files.

An extensive measurement campaign has been conducted with the help of this application. The resulting logs are located in the folder *measurements*
## Usage of the Provided Measurement Data
The measurement data consits of three different log types: the combined log, the cell log, the neighboring cell log, the ping log and the iPerf transfer log. 

-__Cell Log:__ Contains all informations available about the currently connected mobile cell.  
-**Neighboring Cell Log:** Contains all informations available about neighboring mobile network cells.  
-**Ping Log:** Contains the latency informations gained by the `ping` command.  
-**Iperf Transfer Log:** Contains the data rate measurements conducted with iPerf.  
-**CombinedLog:** Contains a merged version of all logs. The temporal resolution is limited by the data rate measurements.  

In addition to that the result of the to-String-method of the SignalStrength class and the CellInfo class are logged seperately. The json log of the conducted iPerf measurements is also included.

## Installation of the Application
To be able to use this application, the provided code needs to be compiled. For this, Android Studio can be used. 


### Embedding iPerf into the Application
If the speed-test functionality should also be used, a compiled version of iPerf is also mandatory. The iPerf binaries are not contained in this repository.

#### Changes Applied to iPerf
The used version of iPerf in this application was slighly changed for the use case of mobile data rate measurements. 
These include the following changes: 
1. Created an option to pass the path iPerf should use for caching. 
2. Added a delay at the end of the test to wait for all packets arriving at the server.
3. Change the data rate calcultion to take the delay at the end of the test into consideration.

Changes were made in the files "iperf.h", "iperf_api.h" and "iperf_client_api.c". 

##### iperf.h
Added the following lines at line 369 and following:

```c++
char *cacheFile_Path;
double test_End_Delay;
int test_End_Delay_Reached;
```
##### iperf_api.h
Added the following lines at line 82 and following:

```c++
#define OPT_TEST_END_DELAY 23
```
##### iperf_api.c
Added the `test_End_Delay`to the JSON output at line 461:
```c++
...%d test_end_delay: %f...,test->test_End_Delay
```
Added a new line after line 920:
```c++
{"cache-file", required_argument,NULL, 'y'},
{"test-end-delay", required_argument, NULL, OPT_TEST_END_DELAY},
```
Added the flag `y:` to line 956.
Added the case y and OPT_TEST_END_DELAY to the switch after line 1345:
```c++
case 'y':
  test->cacheFile_Path = strdup(optarg);
  break;
case OPT_TEST_END_DELAY:
  test->test_End_Delay = atof(optarg);
  test->test_End_Delay_Reached = 0;
break;	
```
Added the following code after line 1353:
```c++
//Test_end_Delay only in sender role and if is in bytes mode n or in blocks mode
if(test->mode != SENDER && (test->settings->bytes || test->settings->blocks)){
  test->test_End_Delay = 0.0;
  test->test_End_Delay_Reached = 0;
}
```
Replaced line 3114 with
```c++
double duration = irp->interval_duration;
if(test->test_End_Delay_Reached){
  duration = duration - (test->test_End_Delay/1000.0);
}
if(duration > 0){
  bandwidth = (double) bytes / duration;
}else{
  bandwidth = 0;
}
```
Added after line 3120
```c++
if(test->test_End_Delay_Reached){
  end_time = end_time - (test->test_End_Delay/1000.0);
}
```
Added adter line 3339
```c++
if(test->test_End_Delay_Reached){
  sender_time = sender_time - (test->test_End_Delay/1000.0);
}
```
Added after line 3443
```c++
if(test->test_End_Delay_Reached){
  receiver_time = receiver_time - (test->test_End_Delay/1000.0);
  end_time = end_time - (test->test_End_Delay/1000.0);
}
```
Replaced line 3749 with
```c++
if(test->test_End_Delay_Reached){
  if((double)irp->interval_duration - (test->test_End_Delay/1000.0) > 0){
      bandwidth = (double) irp->bytes_transferred / ((double) irp->interval_duration- (test->test_End_Delay/1000.0) );
  }else{
    bandwidth = 0.0;
  }
}else{
  bandwidth = (double) irp->bytes_transferred / (double) irp->interval_duration;
}
```
Added after line 3759
```c++
double tempEndTime = (double)et;
double tempDuration = (double) irp->interval_duration;
if(test->test_End_Delay_Reached){
	tempEndTime =tempEndTime- test->test_End_Delay/1000.0;
	tempDuration = tempDuration- test->test_End_Delay/1000.0;
}
```
Changed line 3764-3794 to
```c++
  if (test->json_output){		
  cJSON_AddItemToArray(json_interval_streams, iperf_json_printf("socket: %d  start: %f  end: %f  seconds: %f  bytes: %d  bits_per_second: %f  retransmits: %d  snd_cwnd:  %d  rtt:  %d  rttvar: %d  pmtu: %d  omitted: %b sender: %b", (int64_t) sp->socket, (double) st, tempEndTime, tempDuration, (int64_t) irp->bytes_transferred, bandwidth * 8, (int64_t) irp->interval_retrans, (int64_t) irp->snd_cwnd, (int64_t) irp->rtt, (int64_t) irp->rttvar, (int64_t) irp->pmtu, irp->omitted, sp->sender));
  }else {
    unit_snprintf(cbuf, UNIT_LEN, irp->snd_cwnd, 'A');
    iperf_printf(test, report_bw_retrans_cwnd_format, sp->socket, mbuf, st, tempEndTime, ubuf, nbuf, irp->interval_retrans, cbuf, irp->omitted?report_omitted:"");
  }
} else {
  /* Interval, TCP without retransmits. */
  if (test->json_output)
    cJSON_AddItemToArray(json_interval_streams, iperf_json_printf("socket: %d  start: %f  end: %f  seconds: %f  bytes: %d  bits_per_second: %f  omitted: %b sender: %b", (int64_t) sp->socket, (double) st, tempEndTime, tempDuration, (int64_t) irp->bytes_transferred, bandwidth * 8, irp->omitted, sp->sender));
  else
    iperf_printf(test, report_bw_format, sp->socket, mbuf, st, tempEndTime, ubuf, nbuf, irp->omitted?report_omitted:"");
}
} else {
/* Interval, UDP. */
if (sp->sender) {
  if (test->json_output)
    cJSON_AddItemToArray(json_interval_streams, iperf_json_printf("socket: %d  start: %f  end: %f  seconds: %f  bytes: %d  bits_per_second: %f  packets: %d  omitted: %b sender: %b", (int64_t) sp->socket, (double) st, tempEndTime, tempDuration, (int64_t) irp->bytes_transferred, bandwidth * 8, (int64_t) irp->interval_packet_count, irp->omitted, sp->sender));  
  else
		iperf_printf(test, report_bw_udp_sender_format, sp->socket, mbuf, st, tempEndTime, ubuf, nbuf, zbuf, irp->interval_packet_count, irp->omitted?report_omitted:"");
	} else {
	    if (irp->interval_packet_count > 0) {
	    	lost_percent = 100.0 * irp->interval_cnt_error / irp->interval_packet_count;
	    }
	    else {
		    lost_percent = 0.0;
	    }
	    if (test->json_output)
		cJSON_AddItemToArray(json_interval_streams, iperf_json_printf("socket: %d  start: %f  end: %f  seconds: %f  bytes: %d  bits_per_second: %f  jitter_ms: %f  lost_packets: %d  packets: %d  lost_percent: %f  omitted: %b sender: %b", (int64_t) sp->socket, (double) st, tempEndTime, tempDuration, (int64_t) irp->bytes_transferred, bandwidth * 8, (double) irp->jitter * 1000.0, (int64_t) irp->interval_cnt_error, (int64_t) irp->interval_packet_count, (double) lost_percent, irp->omitted, sp->sender));
	    else
		iperf_printf(test, report_bw_udp_format, sp->socket, mbuf, st, tempEndTime, ubuf, nbuf, irp->jitter * 1000.0, irp->interval_cnt_error, irp->interval_packet_count, lost_percent, irp->omitted?report_omitted:"");
```
Changed line 3832 to
```c++
snprintf(template, sizeof(template) / sizeof(char), "%s/iperf3.XXXXXX", tempdir);
		iperf_err(test, "parameter error - %s", template);
```
Added after line 3844
```c++
tempdir = test->cacheFile_Path;//"/data/user/0/com.example.commandexecutiontest/cache";
```
##### iperf_client_api.c
Added include: `#include <time.h>`

Changed, that if reversed mode is used, the number of received bytes is tested to end the test. For this, add the following conditions to the code afte line 546
```c++
...||
(test->settings->bytes != 0 && test->bytes_received >= test->settings->bytes) ||
(test->settings->blocks != 0 && test->blocks_received >= test->settings->blocks
```

#### Compile iPerf for Android
To compile iPerf, the following steps were done:

1. Download Docker
2. Start docker desktop
3. Clone the repository [android-iperf](https://github.com/KnightWhoSayNi/android-iperf) with the help of the command "Git clone "...""
4. Edit the "Dockerfile": Only Iperf 3.9 is needed to be compiled. You can comment-out all other versions with "#". Add the following lines after line 252: 
  ```
  COPY /iperf-3.9-changed/iperf_api.h /tmp/iperf-3.9/src
  COPY /iperf-3.9-changed/iperf_api.c /tmp/iperf-3.9/src
  COPY /iperf-3.9-changed/iperf_client_api.c /tmp/iperf-3.9/src
  ```
  The changed iperf source files of the previous chapter should be contained in the folder `iperf-3.9-changed`. The copy instructions in the Dockerfile replace the existing source files with the modified files.
6. Execute the command "Docker build-t android-ndk:latest." to build the latest android-ndk needed to compile iPerf for Android
7. Run the docker container with the command "docker run -d --name android-ndk-container android-ndk"
8. Create a directory for the resulting binaries with the command "mkdir -p binaries"
9. Execute "docker cp -a android-ndk-container:/temp/libs binaries"
10. Stop the docker container with "docker stop android-ndk-container"

After these steps, the compiled binary files of iperf are located in the binary folder. They are generated for the architectures arm64, armeabi-v7a, x86 and x86_64.

#### Add the iPerf Binaries to the Android Application
The compiled iPerf binaries should now be copied to the jniLibs folder in the Android Studio project. The iPerf binaries for all architectures have to be renamed into "libIperf3.9.so". 

#### Run the Android Application
Open the Android Studio Project. Wait until everything has been loaded. Then go to "Build->Make Project". After that you can run the application, given you have connected a Android smartphone to the computer that has enabled the developer mode .

