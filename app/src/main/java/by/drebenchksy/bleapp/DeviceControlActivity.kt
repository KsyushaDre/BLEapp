package by.drebenchksy.bleapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_device_control.*

private const val LIST_NAME = "NAME"
private const val LIST_UUID = "UUID"

class DeviceControlActivity: AppCompatActivity() {


    private lateinit var mGattCharacteristics: MutableList<MutableList<BluetoothGattCharacteristic>>
    private var isServiceConnected = false
    lateinit var serviceConnection: ServiceConnection
    private var bluetoothDevice: BluetoothDevice? = null
    private lateinit var expandableListView: ExpandableListView
    private lateinit var expandableListAdapter: SimpleExpandableListAdapter

    companion object {
        var serviceBle: ServiceBle? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        expandableListView = elv_services_and_characteristics

        val arguments = intent.extras
        bluetoothDevice = arguments?.getParcelable("device")

        if (bluetoothDevice != null) {
            tv_device_name.text = bluetoothDevice!!.name
            tv_device_address.text = bluetoothDevice!!.address
        } else {
            tv_device_name.text = "Unknown device name"
            tv_device_address.text = "Unknown device address"
        }

        serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) {
                isServiceConnected = false
                serviceBle = null
            }

            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                isServiceConnected = true
                Log.i("AAA", "onServiceConnected")
                Log.i("AAA", "onServiceConnected, binder: $binder")
                serviceBle = (binder as ServiceBle.MyBinder).getService()

                if (serviceBle != null){ serviceBle!!.setBluetoothDevice(bluetoothDevice)}
            }
        }


        expandableListView.setOnChildClickListener { expListView, view, groupPosition, childPosition, id ->
            val characteristics = mGattCharacteristics[groupPosition]
            val charact = characteristics[childPosition]

            serviceBle?.getGattCallback()?.setCharacteristicAuth(charact)
            serviceBle?.getGattCallback()?.enableNotificationsForAuth(charact)

            false
        }
    }


    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, ServiceBle::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        Log.i("AAA", "DeviceControlActivity onStop")
        if (isServiceConnected) {
            unbindService(serviceConnection)
            isServiceConnected = false
        }

    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        intentFilter.addAction(ACTION_GATT_CONNECTED)
        intentFilter.addAction(ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        registerReceiver(gattUpdateReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }


    private val gattUpdateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                ACTION_GATT_CONNECTED -> {
//                    connected = true
//                    updateConnectionState(R.string.connected)
//                    (context as? Activity)?.invalidateOptionsMenu()
                    Log.i("AAA", "onReceive ACTION_GATT_CONNECTED")
                }
                ACTION_GATT_DISCONNECTED -> {
//                    connected = false
//                    updateConnectionState(R.string.disconnected)
//                    (context as? Activity)?.invalidateOptionsMenu()
//                    clearUI()
                    Log.i("AAA", "onReceive ACTION_GATT_DISCONNECTED")
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    Log.i("AAA", "onReceive ACTION_GATT_SERVICES_DISCOVERED")
//                    serviceBle?.readCharacteristic()
//                    serviceBle?.enableCharacteristicNotification()
                    displayGattServices(serviceBle?.getSupportedGattServices())
                }
                ACTION_DATA_AVAILABLE -> {
                    Log.i(
                        "AAA",
                        "onReceive ACTION_DATA_AVAILABLE, data: ${intent.getStringExtra(EXTRA_DATA)}"
                    )
//                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }




    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = "Unknown service"
        val unknownCharaString= "Unknown characteristic"
        //коллекция для групп
        val gattServiceData: MutableList<HashMap<String, String?>> = mutableListOf()
        //Общая коллекция для коллекций элементов
        val gattCharacteristicData: MutableList<ArrayList<HashMap<String, String?>>> =
            mutableListOf()
        mGattCharacteristics = mutableListOf()

        var groupFrom: Array<String>? = null
        var groupTo: IntArray? = null

        var childFrom: Array<String>? = null
        var childTo: IntArray? = null

        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            Log.i("AAA", "displayGattServices, service uuid: $uuid")
            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
            Log.i("AAA", "displayGattServices, service name: ${SampleGattAttributes.lookup(uuid, unknownServiceString)}")
            currentServiceData[LIST_UUID] = uuid
            gattServiceData += currentServiceData

            //Список атрибутов групп для чтения
            groupFrom = arrayOf(LIST_NAME, LIST_UUID)
            //список ID view-элементов, в которые будет помещены атрибуты групп
            groupTo = intArrayOf(R.id.tv_group_name, R.id.tv_group_uuid)

            //коллекция для элементов одной группы
            val gattCharacteristicGroupData: ArrayList<HashMap<String, String?>> = arrayListOf()
            val gattCharacteristics = gattService.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String?> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()
                Log.i("AAA", "displayGattServices, characteristic uuid: $uuid")
                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
                Log.i("AAA", "displayGattServices, characteristic name: ${SampleGattAttributes.lookup(uuid, unknownServiceString)}")
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData += currentCharaData
            }
            mGattCharacteristics.plusAssign(charas)
            gattCharacteristicData += gattCharacteristicGroupData

            //Список элементов групп для чтения
            childFrom = arrayOf(LIST_NAME, LIST_UUID)
            //список ID view-элементов, в которые будет помещены атрибуты элементов
            childTo = intArrayOf(R.id.tv_child_name, R.id.tv_child_uuid)
        }

        expandableListAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            R.layout.group_view,
            groupFrom,
            groupTo,
            gattCharacteristicData,
            R.layout.child_view,
            childFrom,
            childTo
        )

        expandableListView.setAdapter(expandableListAdapter)
    }
}