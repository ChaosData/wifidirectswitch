# Wifi Direct Switch

Google removed the setting app feature to turn Wifi Direct (aka WiFi P2P), this
app can be used to enable/disable it. It will also display the WPA2 "legacy"
network name and the password.

WiFi Direct opens a non-routable `192.168.49.0/24` subnet.
[This repo](https://gitlab.na.nccgroup.com/jdileo/termux-sshd) may be used in
conjunction to tether devices using WiFi Direct. The advantage is that only
traffic specifically tunneling through the device will route out, preventing
other network traffic from being seen by carriers.

***Note:*** Google changed how WiFi Direct works on Oreo+. The network name and
password will change each time it is enabled.

```bash
ANDROID_HOME=/path/to/android/sdk ./gradlew installDebug
```
