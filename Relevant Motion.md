# Relevant Motion

### Installation
1. In the local.properties file you must add your notch sandbox app username and password. Example:

```
notchUsername='sandboxapplication-###'
notchPassword='password'
```


2. OSC

* be sure to change the IP address
see line 1239: `private String myIP = "172.16.244.95"; // this needs to be the IP of the computer sending to...`
* OSC is sent constantly but will null values until 'START REAL-TIME' is active


3. Notch Hardware + App

  a. open Tutorial app
  b. turn on 2 Notches
  c. tap 'real-time' checkbox
  d. tap 'INIT 2' in capture menu (wait for success)
  e. tap 'CONFIGURE REAL-TIME' (wait for success)
  f. tap 'START REAL-TIME' (Notch visualization will open + OSC will start sending correct data)
