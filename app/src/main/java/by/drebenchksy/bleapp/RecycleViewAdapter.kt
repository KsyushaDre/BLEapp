package by.drebenchksy.bleapp

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView

import androidx.cardview.widget.CardView

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


class RecycleViewAdapter(val devices: ArrayList<BLEDevice>, val context: Context) :
    RecyclerView.Adapter<RecycleViewAdapter.DeviceViewHolder?>() {


    inner class DeviceViewHolder internal constructor(itemView: View) : ViewHolder(itemView) {
        var cv: CardView
        var deviceNameTV: TextView
        var deviceAddressTV: TextView
        var device: BluetoothDevice? = null

        init {
            cv = itemView.findViewById(R.id.cv)
            deviceNameTV = itemView.findViewById(R.id.device_name)
            deviceAddressTV = itemView.findViewById(R.id.device_address)

            itemView.setOnClickListener {
                val intent = Intent(context, DeviceControlActivity::class.java)
                intent.putExtra("device", device)
                context.startActivity(intent)

                Log.i("AAA", "Item clicked")
            }
        }


    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view, parent, false)
        val viewHolder = DeviceViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {

        if (devices[position].device != null) holder.device = devices[position].device

        if (devices[position].deviceName != null) {
            holder.deviceNameTV.text = devices[position].deviceName
        } else {
            holder.deviceNameTV.text = "null"
        }

        if (devices[position].deviceAddress != null) {
            holder.deviceAddressTV.text = devices[position].deviceAddress
        } else {
            holder.deviceAddressTV.text = "null"
        }
    }
}