# HarmonyOS app

This Stage-model application embeds the same Lynx bundles used by the Android and iOS shells.

Open this directory in DevEco Studio, configure signing, and run the `entry` module. From the
repository root, `pnpm run:harmony` builds the Lynx bundle, copies it into `rawfile`, installs
OHPM dependencies, and assembles a debug HAP.

The shell registers `spkPipe` and reads its enabled methods from
`entry/src/main/ets/bridge/SparklingAutolink.ets`. Run `pnpm autolink` after adding or removing a
Sparkling method package; the same command updates Android, iOS, and HarmonyOS registries.
It also resolves bundled `asset:///` images, exposes device and container global props, applies page
chrome query parameters, and supports image resizing and quality options in `chooseMedia`.

Application-specific router interceptors and network common parameters belong in
`entry/src/main/ets/bridge/SparklingHostHooks.ets`. The generated implementation is intentionally
empty: override it with the authentication, risk-control, or business parameters owned by the host
application. The default media implementation ignores `isNeedCut`, `cropRatioWidth`, and
`cropRatioHeight`, matching the current default behavior on Android and iOS.

Requirements:

- DevEco Studio or HarmonyOS Command Line Tools with SDK 6.0.0 (API 20)
- Put the Command Line Tools (`ohpm`, `hvigorw`, and `hdc`) on `PATH`, configure an SDK with
  `DEVECO_SDK_HOME`, `HOS_SDK_HOME`, `OHOS_HOME`, or `OHOS_SDK_HOME`, or install DevEco Studio
  in its default macOS location
- A signing configuration in `build-profile.json5` for physical-device installation; emulators can
  usually install the generated unsigned debug HAP
