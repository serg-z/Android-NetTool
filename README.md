# Building

## Getting libraries and SDKs

Create "libs" dir

    mkdir libs

* Get JDK from Oracle's website, tar.gz version will work fine
* Get Android SDK (stand-alone Android SDK Tools)
* Get AndroidPlot library's JAR file from [it's site](http://androidplot.com/download/), put it into project's "libs" directory

Unpack JDK and Android SDK
Copy path to root JDK directory and Android SDK's "tools" directory into android_paths script, it should look something like that:
~~~
export JAVA_HOME="/home/zhur/Dev/jdk1.8.0_25"
export ANDROID_HOME="/home/zhur/Dev/android-sdk_r23.0.2-linux/android-sdk-linux/tools"
~~~

Update PATH variable before working with Android SDK calling this command:

    source android_paths

Copy Android support library

    cp $ANDROID_HOME/../extras/android/support/v4/android-support-v4.jar libs/

# Android SDK Manager

To launch Android SDK Manager run command:

    android

From Android 4.4.2 (API 19) install

* SDK Platform
* To work with emulator - Intel x86 Atom System Image
* Another Android SDK Platforms if required

Close SDK Manager

List available targets

    android list target

Note the id for target (default: Android 4.4.2)

Prepare project for building

    android update project -t <id> -p .

Builds can be found in "bin" directory

## Building debug version

    ant debug

## Building release version

*(coming soon)*

# Emulator

To create virtual devices for emulator run next command:

    android avd

After creating device, note it's name and from root project directory run command:

    ./run_emulator <avd name>

# Installing APK onto device or emulator using adb

To list available devices run command

    adb devices

To install APK onto one of the listed devices run

    adb -s <device name> install -r <apk path>
