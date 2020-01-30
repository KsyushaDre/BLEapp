package by.drebenchksy.bleapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_device_scan.*

private const val SCAN_PERIOD: Long = 10000

class DeviceScanActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val handler = Handler()
    private var mScanning: Boolean = false
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var leScanCallback: ScanCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)
        Log.i("AAA", "DeviceScanActivity onCreate")


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    1
                );
            }
        }

        scan_button.setOnClickListener {
            scanLeDevice(true)
        }

        recycle_view.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(this)
        recycle_view.layoutManager = linearLayoutManager

        val adapter = RecycleViewAdapter(BLEDevices.devices, this)
        recycle_view.adapter = adapter


        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            leScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    Log.i("AAA", "onScanResult called, device: ${result?.device}")
                    val device = result?.device
                    BLEDevices.devices.add(BLEDevice(device, device!!.name, device.address))
                    adapter.notifyDataSetChanged()
//                if (device?.address == SPECIFIC_SENSOR_ADDRESS) {
//                    Log.i(TAG, "Device found: ${device.address}")
//                    bluetoothDevice = device
//                    bluetoothGatt = device.connectGatt(context, false, gattCallback())
//                } else {
//                    Log.i(TAG, "leScanCallback: device not found")
//                }
                }

                override fun onBatchScanResults(results: List<ScanResult?>?) {
                    super.onBatchScanResults(results)
                    Log.i("AAA", "onBatchScanResults")
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    Log.i("AAA", "onScanFailed called $errorCode")
                }
            }
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    mScanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                    Log.i("AAA", "Scanning = $mScanning")
                }, SCAN_PERIOD)
                mScanning = true
                Log.i("AAA", "Scanning = $mScanning")
                bluetoothLeScanner.startScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
                Log.i("AAA", "Scanning = $mScanning")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanLeDevice(false)
    }
}