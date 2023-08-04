package com.mazenrashed.printooth.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.databinding.BluetoothDeviceRowBinding

class BluetoothDevicesAdapter(private val onItemClick: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<BluetoothDevicesAdapter.ViewHolder>() {

    private val deviceList: MutableList<BluetoothDevice> = mutableListOf()

    override fun getItemCount(): Int {
        return deviceList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = BluetoothDeviceRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceList[position])
    }

    fun updateList(list: List<BluetoothDevice>) {
        deviceList.clear()
        deviceList.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: BluetoothDeviceRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("MissingPermission")
        fun bind(device: BluetoothDevice) {
            binding.name.text = if (device.name.isNullOrEmpty()) device.address else device.name

            if (device.bondState != BluetoothDevice.BOND_NONE) {
                binding.pairStatus.visibility = View.VISIBLE
                binding.pairStatus.text = when (device.bondState) {
                    BluetoothDevice.BOND_BONDED -> "Paired"
                    BluetoothDevice.BOND_BONDING -> "Pairing.."
                    else -> ""
                }
            } else {
                binding.pairStatus.visibility = View.INVISIBLE
            }

           /* if (Printooth.getPairedPrinter()?.address == device.address) {
                binding.pairStatus.visibility = View.VISIBLE
            } else {
                binding.pairStatus.visibility = View.GONE
            }*/


            binding.root.setOnClickListener {
                onItemClick.invoke(device)
            }
        }
    }
}
