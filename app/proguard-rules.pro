# usb-serial-for-android finds its drivers by reflection (getSupportedDevices(), constructor(UsbDevice)).
-keep class com.hoho.android.usbserial.driver.** { *; }

# Keep line numbers for readable crash reports in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
