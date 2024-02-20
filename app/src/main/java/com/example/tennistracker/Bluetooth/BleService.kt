package com.example.tennistracker.Bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.tennistracker.data.Constants.APP_DEVICE_BLUETOOTH_ADDRESS
import java.util.*


@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class BleService : Service() {

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.tennistracker.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.tennistracker.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.tennistracker.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_CHANGED = "com.example.tennistracker.ACTION_DATA_CHANGED"
        const val ACTION_DATA_READ = "com.example.tennistracker.ACTION_DATA_READ"

        const val EXTRA_KEY_UUID = "INTENT_KEY_EXTRA_UUID"
        const val EXTRA_KEY_VALUE = "INTENT_KEY_EXTRA_VALUE"

        private const val NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                broadcastUpdate(ACTION_GATT_CONNECTED)
                connectionState = STATE_CONNECTED
                // Attempts to discover services after successful connection.
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                connectionState = STATE_DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w("APP_DEBUGGER", "Unsuccessful attempt to discover services: status = $status.")
            }
        }

        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)")
        )
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) =
            onCharacteristicRead(gatt, characteristic, characteristic.value, status)
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_READ, characteristic)
            } else {
                Log.d("APP_DEBUGGER", "Unsuccessful attempt to read a characteristic ${characteristic.uuid}.")
            }
        }

        @Deprecated(
            "Used natively in Android 12 and lower",
            ReplaceWith("onCharacteristicChanged(gatt, characteristic, characteristic.value)")
        )
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) =
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            broadcastUpdate(ACTION_DATA_CHANGED, characteristic)
        }
    }

    fun connect(bluetoothAdapter: BluetoothAdapter, address: String = APP_DEVICE_BLUETOOTH_ADDRESS): Boolean {
        bluetoothAdapter.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w("APP_DEBUGGER", "Device not found with provided address. Unable to connect.")
                return false
            }
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w("APP_DEBUGGER", "Attempt to read a characteristic with an uninitialized BluetoothGatt.")
        }
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        bluetoothGatt?.let { gatt ->
            val bluetoothGattDescriptor = characteristic.getDescriptor(UUID.fromString(NOTIFICATION_DESCRIPTOR_UUID))
            bluetoothGattDescriptor.value = if (enabled) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            val result = gatt.writeDescriptor(bluetoothGattDescriptor)
            Log.d("APP_DEBUGGER", "Descriptor writing success = ${result}.")
            gatt.setCharacteristicNotification(characteristic, enabled)
        } ?: run {
            Log.w("APP_DEBUGGER", "Attempt to set notification for a characteristic with an uninitialized BluetoothGatt.")
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic? = null) {
        val intent = Intent(action)
        Log.d("APP_DEBUGGER", "Received characteristic = ${characteristic != null}")

        // parsing is carried out as per profile specifications.
        if (characteristic != null) {
            val value = characteristic.value
            if (value?.isNotEmpty() == true) {
                for (i in value.indices) {
                    Log.d("DATA_RECEIVER", "byte №${(i+1)} = ${value[i].toInt()}")
                }
                intent.putExtra(EXTRA_KEY_UUID, characteristic.uuid.toString())
                intent.putExtra(EXTRA_KEY_VALUE, value)
            }
        }
        sendBroadcast(intent)
    }



    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BleService {
            return this@BleService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }
}