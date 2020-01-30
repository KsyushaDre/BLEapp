package by.drebenchksy.bleapp

import android.bluetooth.BluetoothDevice

data class BLEDevice (
    val device: BluetoothDevice?,
    val deviceName: String?,
    val deviceAddress: String?)