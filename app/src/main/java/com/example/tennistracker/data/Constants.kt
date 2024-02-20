package com.example.tennistracker.data

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
object Constants {
    const val APP_TOAST_BLUETOOTH_NOT_AVAILABLE = "Bluetooth isn't available."
    const val APP_TOAST_BLUETOOTH_DATA_SENDING_NOT_AVAILABLE = "No data can be sent to physical device."
    const val APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SERVICES_FOUND = "Connected to the services!"
    const val APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SUCCESSFUL = "Successful connection."
    const val APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_FAILED = "No connection to the device."
    const val APP_TEXT_LAST_HIT_SPEED = "Last hit speed:"

    const val APP_DEVICE_BLUETOOTH_ADDRESS = "80:65:99:6C:42:FE"
    const val APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE = "19B10000-E8F2-537E-4F6C-D104768A1214"
    const val APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION = "19B10001-E8F2-537E-4F6C-D104768A1214"
    const val APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_INFORMATION = "19B10002-E8F2-537E-4F6C-D104768A1214"

    const val APP_TENNIS_MAX_STRENGTH = 120
    const val APP_TENNIS_MAX_SPEED = 250
    const val APP_TENNIS_MAX_RADIAN = 270

    val APP_STATISTICS_DEFAULT_LIST = listOf(
        TennisHit(speed = 60F, strength = 30F, radian = 135F),
        TennisHit(speed = 50F, strength = 37F, radian = 129F),
        TennisHit(speed = 140F, strength = 90F, radian = 179F),
        TennisHit(speed = 160F, strength = 110F, radian = 165F),
        TennisHit(speed = 155F, strength = 105F, radian = 181F)
    )

    val APP_BLUETOOTH_PERMISSIONS_LIST = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}