# Relevant Motion

### Installation
1. In the local.properties file you must add your notch sandbox app username and password. Example:

```
username=sandboxapplication-###
password=pass1234
license=license1234
```


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
