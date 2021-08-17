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

## Installation
Please follow the [Install](/INSTALL.md) guide to install the application.

## Usage
The application needs several permissions. They need to be granted by the user.  
When started, an overview of several measured variables like reference signals are shown. At the bottom of the screen, it can be selected which data should be logged. It can be chosen between "Cell Tracking", "GPS Tracking", "Speed Test" and "Ping Test". Multiple options can be chosen.  By the click of the "Tracking on" button, the logging can be started.  
In the setting that are accessible at the top, the logging duration, the used iPerf server and several more options can be set. 


