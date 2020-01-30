package by.drebenchksy.bleapp

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2

private const val MSG_DISCOVER_SERVICES = 11
private const val MSG_SERVICES_DISCOVERED = 12
private const val MSG_DATA_READ = 13
private const val MSG_RECONNECT = 14
private const val MSG_GATT_DISCONNECTED = 15


class MyGattCallback(val context: Context) : BluetoothGattCallback(), Handler.Callback {
    var connectionState = STATE_DISCONNECTED
    var enabled: Boolean = true
    private var characteristicAuth: BluetoothGattCharacteristic? = null
    private var descriptorAuth: BluetoothGattDescriptor? = null
    private val bleHandler: Handler
    private var bluetoothGattInGattCallback: BluetoothGatt? = null

    init {
        val handlerThread = HandlerThread("BLE-Worker")
        handlerThread.start()
        bleHandler = Handler(handlerThread.looper, this)
    }

    fun setBluetoothGattInGattCallback(gatt: BluetoothGatt?) {
        bluetoothGattInGattCallback = gatt
    }

    fun setCharacteristicAuth (characteristic: BluetoothGattCharacteristic) {
        characteristicAuth = characteristic
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_DISCOVER_SERVICES -> {
                val gatt = msg.obj as BluetoothGatt
                gatt.discoverServices()
                return true
            }
            MSG_SERVICES_DISCOVERED -> {
                val gatt = msg.obj as BluetoothGatt
//                subscribeNotifications(gatt)
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                return true
            }
            MSG_DATA_READ -> {
                broadcastUpdate(ACTION_DATA_AVAILABLE, msg.obj as BluetoothGattCharacteristic)
                return true
            }
//            MSG_RECONNECT -> {
//                broadcastUpdate(ACTION_RECONNECT_DEVICE, deviceName)
//                return true
//            }
            MSG_GATT_DISCONNECTED -> {
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                return true
            }
            else -> return false
        }
    }

    fun dispose() {
        Log.i("AAA", "MyGattCallback, dispose method")
        bleHandler.removeCallbacksAndMessages(null);
        bleHandler.getLooper().quit();
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        val intentAction: String
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
//                intentAction = ACTION_GATT_CONNECTED
//                connectionState = STATE_CONNECTED
//                broadcastUpdate(intentAction)
                bleHandler.obtainMessage(MSG_DISCOVER_SERVICES, gatt).sendToTarget();
                Log.i("AAA", "Connected to GATT server.")
//                Log.i(
//                    "AAA", "Attempting to start service discovery: " +
//                            gatt.discoverServices()
//                )
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
//                intentAction = ACTION_GATT_DISCONNECTED
//                connectionState = STATE_DISCONNECTED
                Log.i("AAA", "Disconnected from GATT server.")
//                broadcastUpdate(intentAction)
                bleHandler.obtainMessage(MSG_GATT_DISCONNECTED, gatt).sendToTarget()
            }
            else -> Log.i("AAA", "onConnectionStateChange, status: $status")
        }
    }

    // New services discovered
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> {
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.i("AAA", "onServicesDiscovered(), GATT_SUCCESS")
                bleHandler.obtainMessage(MSG_SERVICES_DISCOVERED, gatt).sendToTarget();
            }

            else -> Log.w("AAA", "onServicesDiscovered received: $status")
        }


    }

    // Result of a characteristic read operation
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.i("AAA", "onCharacteristicRead, status: $status")
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> {
                bleHandler.obtainMessage(MSG_DATA_READ, characteristic).sendToTarget();
//                broadcastUpdate(ACTION_DATA_AVAILABLE, )

                val value = characteristic.value
                if (value[0] == 0x10.toByte() && value[1] == 0x01.toByte() && value[2] == 0x01.toByte()) {
                    Log.i("AAA", "FIRST step in auth was successful")
                    tryTwoStepAuth(characteristicAuth)
                }

                if (value[0] == 0x10.toByte() && value[1] == 0x02.toByte() && value[2] == 0x01.toByte()) {
                    Log.i("AAA", "SECOND step in auth was successful")
                    tryThreeStepAuth(characteristicAuth)
                }

                if (value[0] == 0x10.toByte() && value[1] == 0x03.toByte() && value[2] == 0x01.toByte()) {
                    Log.i("AAA", "THIRD step in auth was successful")
//            setHeartMeasurementNotification()
//        }
                }
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        Log.i("AAA", "onCharacteristicChanged characteristic: ${characteristic.uuid}")

        val value = characteristic.value
        if (value[0] == 0x10.toByte() && value[1] == 0x01.toByte() && value[2] == 0x01.toByte()) {
            Log.i("AAA", "FIRST step in auth was successful")
            tryTwoStepAuth(characteristicAuth)
        }

        if (value[0] == 0x10.toByte() && value[1] == 0x02.toByte() && value[2] == 0x01.toByte()) {
            Log.i("AAA", "SECOND step in auth was successful")
            tryThreeStepAuth(characteristicAuth)
        }

        if (value[0] == 0x10.toByte() && value[1] == 0x03.toByte() && value[2] == 0x01.toByte()) {
            Log.i("AAA", "THIRD step in auth was successful")
            setHeartMeasurementNotification()
        }

        if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        Log.i("AAA", "onCharacteristicWrite(), status $status")
        if (status == GATT_SUCCESS) {
//            setNotify(characteristic, true)
//            bluetoothGattInGattCallback?.readCharacteristic(characteristic)
        }
    }


    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        Log.i("AAA", "onDescriptorWrite(), status $status")
        val characteristic = descriptor?.characteristic

        if (characteristic?.uuid == UUID.fromString("00000009-0000-3512-2118-0009af100700") ) {
            if (descriptor?.uuid == UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) {
                if (status == GATT_SUCCESS) {
                    val value = descriptor?.value
                    if (value != null) {
                        if (value[0] != 0.toByte()) {
                            Log.i("AAA", "onDescriptorWrite(), value[0] != 0.toByte() is true")
                            characteristicAuth?.let { writeCharacteristicData(it) }
                        }
                    }
                }
            }
        }

        if (characteristic?.uuid == UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")){
            if (descriptor?.uuid == UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) {
                if (status == GATT_SUCCESS) {
                    val value = descriptor?.value
                    if (value != null) {
                        if (value[0] != 0.toByte()) {
                            Log.i("AAA", "onDescriptorWrite(), value[0] != 0.toByte() is true")
                            Log.i("AAA", "onDescriptorWrite(), heartRateMeasurementNotification is on")

                        }
                    }
                }
            }
        }
    }


    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        when (characteristic.uuid) {
            UUID_HEART_RATE_MEASUREMENT -> {
                val flag = characteristic.properties
                val format = when (flag and 0x01) {
                    0x01 -> {
                        Log.d("AAA", "Heart rate format UINT16.")
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    }
                    else -> {
                        Log.d("AAA", "Heart rate format UINT8.")
                        BluetoothGattCharacteristic.FORMAT_UINT8
                    }
                }
                val heartRate = characteristic.getIntValue(format, 1)
                Log.d("AAA", String.format("Received heart rate: %d", heartRate))
                intent.putExtra(EXTRA_DATA, (heartRate).toString())

            }
            else -> {
                // For all other profiles, writes the data formatted in HEX.
                val data: ByteArray? = characteristic.value
                if (data != null) {
                    for (x in data) {
                        Log.i("AAA", "broadcastUpdate(), else branch, data: $x")
                    }
                }
                if (data?.isNotEmpty() == true) {
                    val hexString: String = data.joinToString(separator = " ") {
                        String.format("%02X", it)
                    }
                    intent.putExtra(EXTRA_DATA, "$data\n$hexString")
                }
            }

        }
        context.sendBroadcast(intent)
    }


    fun setNotify(characteristic: BluetoothGattCharacteristic?, enabled: Boolean) {
        bleHandler.post {
            bluetoothGattInGattCallback?.setCharacteristicNotification(characteristic, enabled)
            val descriptor: BluetoothGattDescriptor? =
                characteristic?.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))

            var value: ByteArray? = byteArrayOf()
            val properties = characteristic?.properties

            value = if (properties!! and PROPERTY_NOTIFY > 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else if (properties and PROPERTY_INDICATE > 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                null
            }

            val finalValue =
                if (enabled) value!! else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            descriptor?.setValue(finalValue)
            bluetoothGattInGattCallback?.writeDescriptor(descriptor)
        }

    }


    fun writeCharacteristicData(characteristic: BluetoothGattCharacteristic) {
        bleHandler.post {
            characteristic.setValue(
                byteArrayOf(
                    0x01,
                    0x8,
                    0x30,
                    0x31,
                    0x32,
                    0x33,
                    0x34,
                    0x35,
                    0x36,
                    0x37,
                    0x38,
                    0x39,
                    0x40,
                    0x41,
                    0x42,
                    0x43,
                    0x44,
                    0x45
                )
            )
            bluetoothGattInGattCallback?.writeCharacteristic(characteristic)
        }
    }

     fun enableNotificationsForAuth(chrt: BluetoothGattCharacteristic) {
        bleHandler.post {
            bluetoothGattInGattCallback?.setCharacteristicNotification(chrt, true)
            for (descriptor in chrt.descriptors) {
                Log.i("AAA", "enableNotificationsForAuth(), descriptor UUID ${descriptor.uuid}")
                if (descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                    Log.i(
                        "AAA",
                        "Found NOTIFICATION BluetoothGattDescriptor: " + descriptor.uuid.toString()
                    )
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    bluetoothGattInGattCallback?.writeDescriptor(descriptor)
                }
            }
        }
    }

    fun enableCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        Log.i(
            "AAA",
            "enableCharacteristicNotification method, characteristic: ${characteristic.uuid}"
        )

        bluetoothGattInGattCallback?.setCharacteristicNotification(characteristic, enabled)
        Log.i(
            "AAA",
            "enableCharacteristicNotification method, setCharacteristicNotification: ${bluetoothGattInGattCallback?.setCharacteristicNotification(
                characteristic,
                enabled
            )}"
        )
        val uuid: UUID = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)

        for (descriptor: BluetoothGattDescriptor in characteristic.descriptors) {
            if (descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                Log.i("AAA", "Found NOTIFICATION BluetoothGattDescriptor: ${descriptor.uuid}")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                descriptorAuth = descriptor
                characteristicAuth = characteristic

                characteristic.value = byteArrayOf(
                    0x01,
                    0x8,
                    0x30,
                    0x31,
                    0x32,
                    0x33,
                    0x34,
                    0x35,
                    0x36,
                    0x37,
                    0x38,
                    0x39,
                    0x40,
                    0x41,
                    0x42,
                    0x43,
                    0x44,
                    0x45
                )
                bluetoothGattInGattCallback?.writeCharacteristic(characteristic)
            }
        }


        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val descriptor = characteristic.getDescriptor(uuid)?.apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            Log.i(
                "AAA",
                "enableCharacteristicNotification method, descriptor: ${descriptor.toString()}"
            )
            bluetoothGattInGattCallback?.writeDescriptor(descriptor)
        }
    }


    private fun tryTwoStepAuth(charact: BluetoothGattCharacteristic?) {
        Log.i("AAA", "tryTwoStepAuth() method")
//        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bleHandler.post {
            charact?.value = byteArrayOf(0x02, 0x8)
            if (bluetoothGattInGattCallback != null && charact != null) {
                bluetoothGattInGattCallback?.writeCharacteristic(charact)
            } else {
                Log.i("AAA", "tryTwoStepAuth bluetoothGattInGattCallback $bluetoothGattInGattCallback, charact $charact")
            }
        }

    }

    private fun tryThreeStepAuth(charact: BluetoothGattCharacteristic?) {
        Log.i("AAA", "tryThreeStepAuth() method")
//        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bleHandler.post {
            val value: ByteArray
            val tmpValue: ByteArray

            if (charact != null) {
                value = charact.value
                tmpValue = value.copyOfRange(3, 19)

                val cipher = Cipher.getInstance("AES/ECB/NoPadding")
                val key = SecretKeySpec(
                    byteArrayOf(
                        0x30,
                        0x31,
                        0x32,
                        0x33,
                        0x34,
                        0x35,
                        0x36,
                        0x37,
                        0x38,
                        0x39,
                        0x40,
                        0x41,
                        0x42,
                        0x43,
                        0x44,
                        0x45
                    ), "AES"
                )

                cipher.init(Cipher.ENCRYPT_MODE, key)
                val bytes = cipher.doFinal(tmpValue)

                val rq: ByteArray = byteArrayOf(0x03, 0x8) + bytes
                charact.value = rq
                bluetoothGattInGattCallback?.writeCharacteristic(charact)
            }
        }
    }

    private fun setHeartMeasurementNotification() {
        Log.i("AAA", "setHeartMeasurementNotification()")
        bleHandler.post {
            val characteristic: BluetoothGattCharacteristic? =
                bluetoothGattInGattCallback?.getService(HEART_RATE_SERVICE_UUID)
                    ?.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)

            bluetoothGattInGattCallback?.setCharacteristicNotification(characteristic, enabled)
            val uuid: UUID = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
            val descriptor = characteristic?.getDescriptor(uuid)?.apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            bluetoothGattInGattCallback?.writeDescriptor(descriptor)
        }
    }
}