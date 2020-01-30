package by.drebenchksy.bleapp

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED =
    "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)

class ServiceBle : Service() {

    private var device: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val binder = MyBinder()

    private val gattCallback = MyGattCallback(this)

    fun getGattCallback(): MyGattCallback {
        return gattCallback
    }


    override fun onCreate() {
        super.onCreate()
        Log.i("AAA", "Service onCreate, device: $device")

    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("AAA", "ServiceBle onBind")
        device = intent?.getParcelableExtra("device")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        Log.d("AAA", "ServiceBle onUnBind")
        return super.onUnbind(intent)
    }

    inner class MyBinder : Binder() {
        fun getService(): ServiceBle {
            return this@ServiceBle
        }
    }

    fun setBluetoothDevice(_device: BluetoothDevice?) {
        device = _device
        Log.i("AAA", "Service, setBluetoothDevice, device: $device")
        bluetoothGatt = device?.connectGatt(this, false, gattCallback)
        gattCallback.setBluetoothGattInGattCallback(bluetoothGatt)
    }


    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        Log.i("AAA", "readCharacteristic method, characteristic: ${characteristic.uuid}")
        bluetoothGatt?.readCharacteristic(characteristic)
    }

    fun getSupportedGattServices(): MutableList<BluetoothGattService>? {
        return bluetoothGatt?.services
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("AAA", "onDestroy service")
        close()
    }

    private fun close() {
        gattCallback.dispose()
        Log.i("AAA", "close() method from ServiceBLE")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}