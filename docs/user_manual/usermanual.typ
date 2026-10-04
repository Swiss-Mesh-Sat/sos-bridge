#import "@preview/polylux:0.4.0": *
#import "formatting.typ": *

#show: userguide.with(
   plugin-name: "SOS Bridge",
   plugin-version: "0.1",
   platform: "ATAK",
   platform-version: "5.6.0",
)


#tak-slide[
= Overview
#toolbox.side-by-side(columns: (.75fr, 9fr))[
#image("plugin_icon.png", width: 70%)
][
SOS Bridge triggers the ATAK emergency alert from *SOS Flashlight + ATAK*: two taps to alert your team over the TAK network, including Meshtastic.
]

== Requirements
- ATAK-CIV 5.6.0 with SOS Bridge loaded.
- *SOS Flashlight + ATAK* installed on the same device, from `github.com/Swiss-Mesh-Sat/SOSFlashlightApp`.
]


#tak-slide[
= Using SOS Bridge
== Raising an alert
+ Optionally, select the alert type in the ATAK emergency tool. ATAK resets it to 911 Alert at every restart.
+ Start SOS in SOS Flashlight + ATAK. The ATAK emergency alert is sent automatically.

== Cancelling the alert
Stop SOS in SOS Flashlight + ATAK. The alert is cancelled, at the earliest 2 minutes after it was sent.
]


#tak-slide[
= The 2-minute delay
SOS Bridge waits at least 2 minutes between two sends (activation or cancellation). This delay is intentional.

In our tests, cancelling an alert before the ATAK delivery acknowledgment of the activation had come back left the alert stuck on the receiving device, and made the devices exchange messages in a loop, sharply increasing radio airtime.

- SOS stopped less than 2 minutes after the alert: the cancellation is sent when the 2 minutes have elapsed.
- SOS stopped and restarted within the delay: nothing is sent, the alert stays active.
]


#tak-slide[
= Rules of use
== Do not mix SOS Flashlight and ATAK
An alert started from SOS Flashlight + ATAK is stopped from SOS Flashlight + ATAK. An alert started manually in ATAK is stopped in ATAK. The plugin does not see manual ATAK actions in real time, and its 2-minute safety delay only covers its own sends.

During an SOS, the ATAK emergency button can be used to stop the radio broadcast while the light keeps signaling.

== Light only, no radio
Unload SOS Bridge in ATAK.

== Contact
Swiss Mesh Sat: swissmeshsat\@protonmail.com
]
