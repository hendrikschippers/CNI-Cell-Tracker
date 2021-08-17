# CNI-Cell-Tracker
!!!! Work in Progress !!!!

The **CNI-Cell-Tracker** is an Android application that can be used for active and passive mobile network measurements. It is able to display and log network, mobility informations provided by the Android API. In addition to that, data rate measurements using **iPerf** and latency measurements can be periodically executed. The obatained data is than save to several comma-seperated-value(.csv) files.

## Installation of the application
To be able to use this application, the provided code needs to be compiled. For this, Android Studio can be used. 


### Embedding iPerf into the application
If the speed-test functionality should also be used, a compiled version of iPerf is also mandatory. The iPerf binaries are not contained in this repository.
#### Changes applied to iPerf
The used version of iPerf in this application was slighly changed for the use case of mobile data rate measurements. 
These include the following changes: 
1. Created an option to pass the path iPerf should use for caching. 
2. Added a delay at the end of the test to wait for all packets arriving at the server.
3. Change the data rate calcultion to take the delay at the end of the test into consideration.

Changes were made in the files "iperf.h", "iperf_api.h" and "iperf_client_api.c". 

#### Compile iPerf for Android
To compile iPerf, the following steps were done:

1. Download Docker
2. Start docker desktop
3. Clone the repository xxx with the help of the command "Git clone """
4. Use "cd" to get to the 
5. Execute the command "Docker build-t android-ndk:latest." to build the latest android-ndk needed to compile iPerf for Android
6. Run the docker container with the command "docker run -d --name android-ndk-container android-ndk"
7. Create a directory for the resulting binaries with the command "mkdir -p binaries"
8. Execute "docker cp -a android-ndk-container:/temp/libs binaries"
9. Stop the docker container with "docker stop android-ndk-container"



