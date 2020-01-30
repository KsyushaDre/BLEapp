package by.drebenchksy.bleapp

import android.app.IntentService
import android.bluetooth.*
import android.content.Intent
import android.util.Log
import java.util.*


//private const val STATE_DISCONNECTED = 0
//private const val STATE_CONNECTING = 1
//private const val STATE_CONNECTED = 2
//const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
//const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
//const val ACTION_GATT_SERVICES_DISCOVERED =
//    "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
//const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
//const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
//val UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
//
//class BluetoothLeService: IntentService("myname") {
//    var connectionState = STATE_DISCONNECTED
//
//    private var device: BluetoothDevice? = null
//
//    private var  bluetoothGatt: BluetoothGatt? = null
//    private lateinit var gattCallback: BluetoothGattCallback
//
//    var enabled: Boolean = true
//    override fun onCreate() {
//        super.onCreate()
//Log.i("AAA", "onCreate service")
//    }
//
//
//    override fun onHandleIntent(intent: Intent?) {
//
//        device = intent?.getParcelableExtra("device")
//
//
//        // Various callback methods defined by the BLE API.
//        gattCallback = object : BluetoothGattCallback() {
//            override fun onConnectionStateChange(
//                gatt: BluetoothGatt,
//                status: Int,
//                newState: Int
//            ) {
//                val intentAction: String
//                when (newState) {
//                    BluetoothProfile.STATE_CONNECTED -> {
//                        intentAction = ACTION_GATT_CONNECTED
//                        connectionState = STATE_CONNECTED
//                        broadcastUpdate(intentAction)
////                        gatt.discoverServices()
//                        Log.i("AAA", "Connected to GATT server.")
//                        Log.i("AAA", "Attempting to start service discovery: " +
//                                bluetoothGatt?.discoverServices())
//                    }
//                    BluetoothProfile.STATE_DISCONNECTED -> {
//                        intentAction = ACTION_GATT_DISCONNECTED
//                        connectionState = STATE_DISCONNECTED
//                        Log.i("AAA", "Disconnected from GATT server.")
//                        broadcastUpdate(intentAction)
//                    }
//                }
//            }
//
//            // New services discovered
//            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//                when (status) {
//                    BluetoothGatt.GATT_SUCCESS -> {
//                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
//
//                        val characteristic =
//                            gatt.getService(HEART_RATE_SERVICE_UUID)
//                                .getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
//
//                        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
//                        val uuid: UUID = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
//                        val descriptor = characteristic.getDescriptor(uuid).apply {
//                            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                        }
//                        bluetoothGatt?.writeDescriptor(descriptor)
//
//
//                    }
//                    else -> Log.w("AAA", "onServicesDiscovered received: $status")
//                }
//
//
//            }
//
//            // Result of a characteristic read operation
//            override fun onCharacteristicRead(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int
//            ) {
//                when (status) {
//                    BluetoothGatt.GATT_SUCCESS -> {
//                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//                    }
//                }
//            }
//
//             override fun onCharacteristicChanged(
//                 gatt: BluetoothGatt,
//                 characteristic: BluetoothGattCharacteristic
//             ) {
//                 broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
//             }
//        }
//
//        bluetoothGatt = device?.connectGatt(this, false, gattCallback)
//    }
//
//    private fun broadcastUpdate(action: String) {
//        val intent = Intent(action)
//        sendBroadcast(intent)
//    }
//
//    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
//        val intent = Intent(action)
//
//        // This is special handling for the Heart Rate Measurement profile. Data
//        // parsing is carried out as per profile specifications.
//        when (characteristic.uuid) {
//            UUID_HEART_RATE_MEASUREMENT -> {
//                val flag = characteristic.properties
//                val format = when (flag and 0x01) {
//                    0x01 -> {
//                        Log.d("AAA", "Heart rate format UINT16.")
//                        BluetoothGattCharacteristic.FORMAT_UINT16
//                    }
//                    else -> {
//                        Log.d("AAA", "Heart rate format UINT8.")
//                        BluetoothGattCharacteristic.FORMAT_UINT8
//                    }
//                }
//                val heartRate = characteristic.getIntValue(format, 1)
//                Log.d("AAA", String.format("Received heart rate: %d", heartRate))
//                intent.putExtra(EXTRA_DATA, (heartRate).toString())
//            }
//            else -> {
//                // For all other profiles, writes the data formatted in HEX.
//                val data: ByteArray? = characteristic.value
//                if (data?.isNotEmpty() == true) {
//                    val hexString: String = data.joinToString(separator = " ") {
//                        String.format("%02X", it)
//                    }
//                    intent.putExtra(EXTRA_DATA, "$data\n$hexString")
//                }
//            }
//
//        }
//        sendBroadcast(intent)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.i("AAA", "onDestroy service")
////        close()
//    }
//
//    fun close() {
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//    }
//}