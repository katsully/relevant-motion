# Relevant Motion


### OSC

* be sure to change the IP address
see line 1239: `private String myIP = "172.16.244.95"; // this needs to be the IP of the computer sending to...`
* OSC data is sent constantly but will null values until 'START REAL-TIME' is active


### Notch Hardware + App

  1. open Notch Tutorial Android app
  2. turn on 2 Notches (# from real_time config file)
  3. tap 'CONNECT TO NETWORK'
  4. tap 'REAL-TIME' checkbox
  5. tap 'INIT 2' (from capture menu)
  6. tap 'CONFIGURE REAL-TIME'
  7. tap 'START REAL-TIME' (Notch visualization will open + OSC will start sending correct data)


### Updating OSC
1. put imports into the top of mainFragment.java file
```
import java.net.*;
import java.util.*;
import com.illposed.osc.*;
```


2. include illposed Java OSC lib in dependencies block of the app/build.gradle file
```
compile 'com.illposed.osc:javaosc-core:0.4'
```


3. put the OSC thread (the good stuff) add the bottom of mainFragment.java
```
private Thread oscThread = new Thread() {
        …
}
```


4. start the OSC thread when mainFragment is created (inside the onCreate function of mainFragment.java)
```
oscThread.start();
```