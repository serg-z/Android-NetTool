# Android NetTool

[![Graphs and stats page](http://i.imgur.com/ND9wER0m.png)](http://i.imgur.com/ND9wER0.png)
[![Streamer page](http://i.imgur.com/ecNdufZm.png)](http://i.imgur.com/ecNdufZ.png)

# Building

## Getting libraries and SDKs

Create "libs" dir

``` bash
mkdir libs
```

* Get JDK from Oracle's website, tar.gz version will work fine
* Get Android SDK (stand-alone Android SDK Tools)
* Get [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html)
* Get AndroidPlot library's JAR file from [it's site](http://androidplot.com/download/), put it into project's "libs" directory

Unpack JDK and Android SDK/NDK
Copy paths to root JDK directory, Android SDK's "tools" directory and android NDK's directory into `android_paths` script, it should look something like that:

``` bash
export JAVA_HOME="/home/user/Dev/jdk1.8.0_31"
export NDK_HOME="/home/user/Dev/android-ndk-r10d"
export ANDROID_HOME="/home/user/Dev/android-sdk_r24.0.2-linux/android-sdk-linux/tools"
```

Update PATH variable before working with Android SDK calling this command:

``` bash
source android_paths
```

# Android SDK Manager

To launch Android SDK Manager run command:

``` bash
android &
```

Click there "Deselect All" to skip unwanted packages for now. Select and install "Extras / Android Support Library".

Copy Android support library into project's directory

``` bash
cp $ANDROID_HOME/../extras/android/support/v4/android-support-v4.jar libs/
```

From Android 5.0.1 (API 21) install

* SDK Platform
* To work with emulator - Intel x86 Atom System Image
* Another Android SDK Platforms if required

Install "Tools / Android SDK Build-tools 21.1.2"

*You'll need to update "PATH" variable in `android_paths` script to use another version of Build-tools*

Close SDK Manager

List available targets

``` bash
android list target
```

Note the ID for target (default: Android 5.0.1)

Prepare project for building

``` bash
android update project -t <id> -p .
```

# Building

Builds can be found in "bin" directory

## Debug version

``` bash
./build_debug
```

## Release version

### Generate keystore with release signing key in it

Make sure you're not overwriting existing keystore file (NetTool-release.keystore in current directory)

Run the command:

``` bash
$JAVA_HOME/bin/keytool -genkey -v -keystore NetTool-release.keystore -alias NetTool-key -keyalg RSA -keysize 2048 -validity 10000
```

1. Enter password for keystore. You'll need it to access keys stored in it.
2. Enter information for key.
3. Enter password for key.

**NOTE: If you plan to publish the app on Google Play, you'd like to keep your release signing key to update the app in the future. It's impossible to update the app with different key.**

More information at [Signing Your App Manually](http://developer.android.com/tools/publishing/app-signing.html#signing-manually)

### Build, sign and zipalign APK

``` bash
./build_release <path to keystore>
```

*During building, at signing stage, you'll be asked for keystore password and key password (if it's different from keystore's password)*

NetTool-release.apk is located in "bin" directory.

# Emulator

To create virtual devices for emulator run next command:

``` bash
android avd
```

After creating device, note it's name and from root project directory run command:

``` bash
./run_emulator <avd name> [scale]
```

# Installing APK onto device or emulator using adb

To list available devices run command

``` bash
adb devices
```

To install APK onto one of the listed devices run

``` bash
adb -s <device name> install -r <apk path>
```
