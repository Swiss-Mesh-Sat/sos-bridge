# SOS Bridge

**ATAK-CIV plugin that triggers the ATAK emergency alert from SOS Flashlight: two taps to alert your team over the TAK network, including Meshtastic.**

> Status: early version (0.2). Signed through the TAK.gov third-party pipeline; field-tested over Meshtastic with version 0.1 (see *Tested configuration*).

## Why SOS Bridge?

Triggering an emergency alert in ATAK takes several actions: about 4 to 5 taps from an unlocked phone in our tests. With SOS Bridge, two taps in SOS Flashlight are enough, and the alert is sent automatically over the TAK network, including Meshtastic.

SOS Flashlight lets you independently enable or disable the screen, the flashlight, vibration and sound, and transmit a custom Morse message. This gives you several modes:

- **Full signal**: light, sound and ATAK alert, to be seen and heard while alerting your team.
- **Silent alert**: all local outputs disabled, only the ATAK alert goes out over the network.
- **Light only**: SOS Bridge unloaded in ATAK, no radio transmission at all.

## How it works

```
SOS Flashlight + ATAK  --SOS_STARTED / SOS_STOPPED-->  SOS Bridge (ATAK plugin)  -->  ATAK emergency alert  -->  TAK network / Meshtastic
```

SOS Bridge requires **SOS Flashlight + ATAK**, a modified version of SOS Flashlight that announces when signaling starts and stops:
**https://github.com/Swiss-Mesh-Sat/SOSFlashlightApp** (branch `sos-bridge`).

The plugin listens for two broadcast actions:

- `ch.swissmeshsat.sosbridge.SOS_STARTED`
- `ch.swissmeshsat.sosbridge.SOS_STOPPED`

## Behavior

### The ATAK alert follows SOS Flashlight

- Starting SOS Flashlight sends the ATAK emergency alert, whatever Morse text is configured.
- Stopping SOS Flashlight cancels the alert.

### Deliberate 2-minute minimum between two sends

SOS Bridge waits **at least 2 minutes between two sends** (activation or cancellation). This delay is **intentional**.

In our tests, cancelling an alert *before* the ATAK delivery acknowledgment (ACK) of the activation had come back caused a serious problem: the alert stayed stuck on the receiving device, reappeared when dismissed, and the devices kept exchanging messages in a loop, sharply increasing radio airtime. On a LoRa mesh this could saturate the channel for the whole group.

In good radio conditions we observed the ACK coming back 15 to 20 seconds after sending. Meshtastic may retry undelivered messages, which can lengthen this time, so 2 minutes leaves a wide margin. Resending more often would not help a message get through anyway; it would only load the channel.

What this means in practice:

- If SOS is stopped less than 2 minutes after the alert was sent, the cancellation is sent automatically when the 2 minutes have elapsed. The alert remains visible until then.
- If SOS is stopped and restarted within the delay, nothing is sent: the alert simply stays active.

### Radio usage over Meshtastic

Observed in our tests with the Meshtastic ATAK plugin (LONG_FAST preset):

- Each alert or cancellation is sent as a compressed generic CoT message, split into 3 LoRa packets.
- The transfer is confirmed by the receiving device after about 20 to 25 seconds, adding about 0.2 to 0.3 % of air utilization per message.
- The Meshtastic ATAK plugin waits 42 seconds for this confirmation and retries the whole transfer if it does not arrive. Under poor radio conditions, retries multiply the airtime used and may show "message not delivered" notifications.
- Short peaks of channel utilization during a transfer are expected.

### Alert type

SOS Bridge uses the alert type currently selected in ATAK's emergency tool (911 Alert, Ring The Bell, Geo-fence Breached or Troops In Contact), and **911 Alert by default**. In our tests ATAK reset its emergency tool to 911 Alert at every restart: to use another type, select it again in ATAK after launching it. Agree in advance with your group on what each alert type means.

### Do not mix SOS Flashlight and ATAK

An alert started from SOS Flashlight should be stopped from SOS Flashlight. An alert started manually in ATAK should be stopped in ATAK. Why:

1. **The plugin does not see your manual actions in ATAK in real time.** It only checks the actual ATAK alert state when SOS Flashlight changes state, or when a pending delay ends. Acting in ATAK during an SOS can lead the plugin to make a later decision that contradicts your action.
2. **The 2-minute safety delay only applies to sends made by the plugin.** Mixing SOS Flashlight and ATAK bypasses it, with the risk of the stuck-alert / radio-loop problem described above.

During an SOS, the ATAK emergency button can be used to stop the radio broadcast while the light keeps signaling.

### Light only, no radio

Unload SOS Bridge in ATAK. SOS Flashlight then behaves like the original app.

## Known limitations

- ATAK must be running with SOS Bridge loaded; otherwise the start/stop messages are lost.
- The broadcasts are not authenticated: any app on the device could send or listen to them.
- Manual actions in ATAK are not counted in the 2-minute delay (see *Do not mix*).
- Signed through the TAK.gov third-party pipeline: ATAK indicates that the plugin uses a third-party signature.

## Tested configuration

- ATAK-CIV 5.6.0.12 (Play Store), Samsung Galaxy Tab Active5 (SM-X300), Android 16, SOS Bridge 0.1 signed by the TAK.gov third-party pipeline. Alerts and cancellations received over Meshtastic on a second ATAK device (Samsung Galaxy S23).
- ATAK-CIV 5.6.0.24 developer build (SDK), Samsung SM-T500, Android 12.

## Building

Requirements:

- Eclipse Temurin JDK 17
- Android SDK (installed with Android Studio)
- ATAK-CIV SDK 5.6.0, available from https://tak.gov (free account). **The SDK is not included in this repository and must not be redistributed.**

Create a `local.properties` file at the project root (never commit it). Offline mode, without TAK.gov credentials:

```
sdk.dir=/path/to/Android/Sdk
takdev.plugin=/path/to/ATAK-CIV-5.6.0.x-SDK/atak-gradle-takdev.jar
sdk.path=/path/to/ATAK-CIV-5.6.0.x-SDK
```

Then build the debug variant:

```
JAVA_HOME=/path/to/temurin-17 ./gradlew assembleCivDebug
```

## Testing without SOS Flashlight

With ATAK running and the plugin loaded, simulate SOS Flashlight with adb:

```
adb shell am broadcast -a ch.swissmeshsat.sosbridge.SOS_STARTED
adb shell am broadcast -a ch.swissmeshsat.sosbridge.SOS_STOPPED
adb logcat -s SosBridge
```

## About the icon

![SOS Bridge icon](app/src/main/res/drawable/ic_launcher.png) ![SOS Bridge logo](docs/user_manual/plugin_icon.png)

The logo reads both **SOS** and **SMS** (Swiss Mesh Sat): a red **M** sits at the heart of the **O**. Each element echoes SOS Flashlight: the white frame is the flashlight, the red is the screen, and the M stands for Morse.

## Changelog

- **0.2**: new plugin icon (SOS / SMS monogram).
- **0.1**: first release, signed through the TAK.gov third-party pipeline.

## License and credits

SOS Bridge is free software, released under the **GNU General Public License v3.0** (see `LICENSE`).

- Built from the ATAK-CIV plugin template (TAK Product Center).
- Works with [SOS Flashlight](https://github.com/WeilJimmer/SOSFlashlightApp) by WeilJimmer (GPLv3).
- The approach to triggering the ATAK emergency alert was informed by [TAKWatch](https://github.com/TDF-PL/TAKWatch) (GPL-3.0).

SOS Bridge is not an official TAK product. It comes with no warranty and is no substitute for contacting emergency services.

Developed by Swiss Mesh Sat - https://swissmeshsat.ch

## TAK.gov plugin information

### PURPOSE AND CAPABILITIES

Triggers and cancels the ATAK emergency alert when SOS Flashlight + ATAK starts or stops signaling, so an alert can be raised in two taps. Uses the alert type selected in the ATAK emergency tool (911 Alert by default) and enforces a minimum of 2 minutes between two sends.

### STATUS

In development (0.2). Signed through the TAK.gov third-party pipeline and tested on ATAK-CIV 5.6.0.12 (Play Store) over Meshtastic.

### ATAK VERSIONS

ATAK-CIV 5.6.0

### POINT OF CONTACTS

Nenad Vasilijevic (Swiss Mesh Sat) - swissmeshsat@protonmail.com

### USER GROUPS

Civilian ATAK-CIV users: hunting groups, outdoor and backcountry professionals, survivalists and outdoor teams coordinating over Meshtastic.

### EQUIPMENT REQUIRED

Android device running ATAK-CIV 5.6.0, with SOS Flashlight + ATAK installed (https://github.com/Swiss-Mesh-Sat/SOSFlashlightApp).

### EQUIPMENT SUPPORTED

Any network used by ATAK for emergency alerts. Meshtastic radios through the Meshtastic ATAK plugin: alerts and cancellations triggered by SOS Bridge were tested over Meshtastic.

### PORTS REQUIRED

None. SOS Bridge opens no network port: it receives local Android broadcasts and uses the ATAK emergency API. Network transmission is handled by ATAK.

### COMPILATION

See *Building* above. Release build: `./gradlew assembleCivRelease`. The version code is fixed by `takStaticVersion` in `gradle.properties`, because the source archive has no git history.

### DEVELOPER NOTES

- Listens for `ch.swissmeshsat.sosbridge.SOS_STARTED` and `ch.swissmeshsat.sosbridge.SOS_STOPPED` through `AtakBroadcast.registerSystemReceiver`, unregistered in `onStop()`.
- Uses `EmergencyManager`: `getEmergencyType`, `setEmergencyType`, `initiateRepeat`, `setEmergencyOn`, `cancelRepeat`, `isEmergencyOn`.
- No lambdas, as they break release builds after ProGuard according to the SDK README.
