package com.example.tennistracker.ViewModel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tennistracker.Bluetooth.BleService
import com.example.tennistracker.data.TennisHit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
class TennisViewModel : ViewModel() {
    private var isBluetoothAvailable: Boolean? = null

    private var bluetoothAdapter: BluetoothAdapter? = null

    val hitData: MutableLiveData<ArrayList<TennisHit>> = MutableLiveData(arrayListOf())

    fun getBluetoothAdapter(): BluetoothAdapter? {
        return bluetoothAdapter
    }

    fun getCurrentSpeed(): Float {
        var sum = 0F
        hitData.value!!.forEach {
            sum += (it.getSpeed() / hitData.value!!.size)
        }
        return sum
    }

    fun getCurrentStrength(): Float {
        var sum = 0F
        hitData.value!!.forEach {
            sum += (it.getStrength() / hitData.value!!.size)
        }
        return sum
    }

    fun getCurrentRadian(): Float {
        var sum = 0F
        hitData.value!!.forEach {
            sum += (it.getRadian() / hitData.value!!.size)
        }
        return sum
    }

    fun getHitData(): List<TennisHit> {
        return hitData.value!!
    }

    fun addHit(hit: TennisHit) {
        hitData.value!!.add(hit)
        hitData.postValue(hitData.value!!)
    }

    fun cleanHitData() {
        hitData.value!!.clear()
    }

    fun setBluetoothAvailable(available: Boolean) {
        isBluetoothAvailable = available
    }

    fun isBluetoothAvailable(): Boolean? {
        return isBluetoothAvailable
    }

    fun updateBluetoothAdapter(context: Context) {
        val bluetoothManager: BluetoothManager? = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        setBluetoothAvailable(bluetoothAdapter != null && bluetoothAdapter!!.isEnabled)
    }

    fun getIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(BleService.ACTION_GATT_CONNECTED)
        filter.addAction(BleService.ACTION_GATT_DISCONNECTED)
        filter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED)
        filter.addAction(BleService.ACTION_DATA_CHANGED)
        filter.addAction(BleService.ACTION_DATA_READ)

        return filter
    }

    private fun getCheckedByte(command: Int): Byte {
        val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(command).array()
        if (bytes[0].toInt() != 0 || bytes[1].toInt() != 0 || bytes[2].toInt() != 0) {
            Log.d("APP_CHECKER", "Byte1 = ${bytes[0].toInt()}, Byte2 = ${bytes[1].toInt()},  Byte3 = ${bytes[2].toInt()}, Byte4 = ${bytes[3].toInt()}")
            throw(RuntimeException("This number is too big to send."))
        }
        return bytes[3]
    }

    fun performTimerEvent(timerFun: () -> Unit, time: Long) {
        val eventTimer = Timer()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                MainScope().launch {
                    timerFun()
                }
            }
        }
        eventTimer.schedule(timerTask, time)
    }
}