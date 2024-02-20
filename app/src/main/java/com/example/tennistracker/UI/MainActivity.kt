package com.example.tennistracker.UI

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tennistracker.Bluetooth.BleService
import com.example.tennistracker.R
import com.example.tennistracker.ViewModel.TennisViewModel
import com.example.tennistracker.data.Constants
import com.example.tennistracker.data.Constants.APP_BLUETOOTH_PERMISSIONS_LIST
import com.example.tennistracker.data.Constants.APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION
import com.example.tennistracker.data.Constants.APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_INFORMATION
import com.example.tennistracker.data.Constants.APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE
import com.example.tennistracker.data.Constants.APP_TOAST_BLUETOOTH_DATA_SENDING_NOT_AVAILABLE
import com.example.tennistracker.data.Constants.APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_FAILED
import com.example.tennistracker.data.Constants.APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SERVICES_FOUND
import com.example.tennistracker.data.Constants.APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SUCCESSFUL
import com.example.tennistracker.data.Constants.APP_TOAST_BLUETOOTH_NOT_AVAILABLE
import com.example.tennistracker.data.TennisHit
import com.example.tennistracker.databinding.ActivityMainBinding


@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tennisViewModel: TennisViewModel by viewModels()
    private var bluetoothService: BleService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BleService.LocalBinder).getService()
            Log.e("APP_DEBUGGER", "BLE service was initialized.")
            bluetoothService?.let { bluetooth ->
                for (permission: String in APP_BLUETOOTH_PERMISSIONS_LIST) {
                    if (ActivityCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                        this@MainActivity.requestPermissions(APP_BLUETOOTH_PERMISSIONS_LIST.toTypedArray(), REQUEST_CODE_LOC)
                        return
                    }
                }
                performBluetoothConnectionAttempt()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bluetooth_menu, menu)

        tennisViewModel.performTimerEvent({
            (findViewById<View>(R.id.bluetooth)).apply {
                this.setBackgroundColor(getColor(R.color.red))
            }
        }, 50L)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.bluetooth -> {
                tennisViewModel.updateBluetoothAdapter(this)

                if (bluetoothService != null) {
                    performBluetoothConnectionAttempt()
                } else {
                    Log.d("APP_DEBUGGER", "Warning: attempt to connect to a device, even though service wasn't initialized!")
                    val serviceIntent = Intent(this, BleService::class.java)
                    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
            R.id.cleaner -> {
                tennisViewModel.cleanHitData()
                Constants.APP_STATISTICS_DEFAULT_LIST.forEach {
                    tennisViewModel.addHit(it)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOC -> if (grantResults.isNotEmpty()) {
                for (gr in grantResults) {
                    // Check if request is granted or not
                    if (gr != PackageManager.PERMISSION_GRANTED) {
                        tennisViewModel.setBluetoothAvailable(false)
                        Toast.makeText(this, APP_TOAST_BLUETOOTH_DATA_SENDING_NOT_AVAILABLE, Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                performBluetoothConnectionAttempt()
            }
            else -> return
        }
    }

    private fun performBluetoothConnectionAttempt() {
        tennisViewModel.updateBluetoothAdapter(this)

        if (tennisViewModel.isBluetoothAvailable()!!) {
            bluetoothService!!.connect(tennisViewModel.getBluetoothAdapter()!!)
        } else {
            Toast.makeText(this, "${APP_TOAST_BLUETOOTH_NOT_AVAILABLE}\n$APP_TOAST_BLUETOOTH_DATA_SENDING_NOT_AVAILABLE", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, BleService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, tennisViewModel.getIntentFilter())
        if (bluetoothService != null) {
            performBluetoothConnectionAttempt()
        } else {
            Log.d("APP_DEBUGGER", "Warning: attempt to connect to a device, even though service wasn't initialized!")
            val serviceIntent = Intent(this, BleService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleService.ACTION_GATT_DISCONNECTED -> {
                    (findViewById<View>(R.id.bluetooth)).apply {
                        this.setBackgroundColor(getColor(R.color.red))
                    }
                    Toast.makeText(context, APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_FAILED, Toast.LENGTH_SHORT).show()
                }
                BleService.ACTION_GATT_CONNECTED -> {
                    (findViewById<View>(R.id.bluetooth)).apply {
                        this.setBackgroundColor(getColor(R.color.yellow))
                    }
                    Toast.makeText(context, APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SUCCESSFUL, Toast.LENGTH_SHORT).show()
                }
                BleService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    (findViewById<View>(R.id.bluetooth)).apply {
                        this.setBackgroundColor(getColor(R.color.green))
                    }
                    Toast.makeText(context, APP_TOAST_BLUETOOTH_DEVICE_CONNECTION_SERVICES_FOUND, Toast.LENGTH_SHORT).show()

                    tennisViewModel.performTimerEvent({
                        Log.d("EVENT_RECEIVER", "Services found: amount = ${bluetoothService!!.getSupportedGattServices()!!.size}")
                        bluetoothService!!.getSupportedGattServices()!!.forEach { service ->
                            Log.d("EVENT_RECEIVER", "${service!!.uuid}")
                            if (service.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE.lowercase()) {
                                Log.d("EVENT_RECEIVER", "The correct service was found (${service.uuid}). Its characteristics are (amount = ${service.characteristics.size}):")
                                service.characteristics.forEach { characteristic ->
                                    Log.d("EVENT_RECEIVER", characteristic.uuid.toString())
                                    if (characteristic.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION.lowercase()) {
                                        Log.d("EVENT_RECEIVER", "The notification characteristic was found (${characteristic.uuid}).")
                                        bluetoothService!!.setCharacteristicNotification(characteristic, true)
                                        //readCharacteristicsByStatesArray()
                                    }
                                }
                            }
                        }
                    }, 100L)
                }
                BleService.ACTION_DATA_CHANGED -> {
                    val uuid = intent.getStringExtra(BleService.EXTRA_KEY_UUID)!!
                    val value = intent.getByteArrayExtra(BleService.EXTRA_KEY_VALUE)!!
                    Log.d("EVENT_RECEIVER", "Available data was changed. UUID = ${uuid}.")

                    if (uuid == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION.lowercase()) {
                        if (value.size < DATA_RECEPTION_AMOUNT_NOTIFICATION) {
                            throw(RuntimeException("Received wrong data amount (${value.size}) from notification characteristic."))
                        }

                        val firstVal = value[0].toUByte().toInt()
                        Log.d("EVENT_RECEIVER", "Notification value = ${firstVal}.")
                        if (firstVal > 0) {
//                            val service = bluetoothService!!.getSupportedGattServices()!!.find {it!!.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE.lowercase()}
//                            val characteristic = service!!.characteristics.find {it.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION.lowercase()}
//                            bluetoothService!!.setCharacteristicNotification(characteristic!!, false)

                            readCharacteristics()
                        }
                    }
                }
                BleService.ACTION_DATA_READ -> {
                    val uuid = intent.getStringExtra(BleService.EXTRA_KEY_UUID)!!
                    val value = intent.getByteArrayExtra(BleService.EXTRA_KEY_VALUE)!!
                    Log.d("EVENT_RECEIVER", "Available data was read. UUID = ${uuid}.")

                    if (uuid == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_INFORMATION.lowercase()) {
                        if (value.size < DATA_RECEPTION_AMOUNT_INFORMATION) {
                            throw(RuntimeException("Received wrong data amount (${value.size}) from information characteristic."))
                        }

                        val firstVal = value[0].toUByte().toInt()
                        val secondVal = value[1].toUByte().toInt()
                        val thirdVal = value[2].toUByte().toInt()
                        val fourthVal = value[3].toUByte().toInt()
                        Log.d("EVENT_RECEIVER", "Information data: first value = $firstVal, second value = ${secondVal}, third value = ${thirdVal}, fourth value = ${fourthVal}.")
                        onTennisHitCharacteristicRead(firstVal, secondVal, thirdVal + fourthVal)
                    }
                }
            }
        }
    }

    private fun readCharacteristics() {
        val service = bluetoothService!!.getSupportedGattServices()!!.find {it!!.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE.lowercase()}
        val characteristic = service!!.characteristics.find {it.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_INFORMATION.lowercase()}
        Log.d("APP_DEBUGGER", "Calling the next read iteration...")
        bluetoothService!!.readCharacteristic(characteristic!!)
    }

    private fun onTennisHitCharacteristicRead(speed: Int, strength: Int, radian: Int) {
        val newTennisHit = TennisHit(
            speed = speed.toFloat(),
            strength = strength.toFloat(),
            radian = radian.toFloat(),
        )
        tennisViewModel.addHit(newTennisHit)

//        val service = bluetoothService!!.getSupportedGattServices()!!.find {it!!.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_MAIN_SERVICE.lowercase()}
//        val characteristic = service!!.characteristics.find {it.uuid.toString() == APP_DEVICE_BLUETOOTH_UUID_CHARACTERISTIC_NOTIFICATION.lowercase()}
//        bluetoothService!!.setCharacteristicNotification(characteristic!!, true)
    }

    companion object {
        const val REQUEST_CODE_LOC = 1
        const val DATA_RECEPTION_AMOUNT_NOTIFICATION = 1
        const val DATA_RECEPTION_AMOUNT_INFORMATION = 4
    }
}