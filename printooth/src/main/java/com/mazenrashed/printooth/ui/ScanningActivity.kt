package com.mazenrashed.printooth.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.DiscoveryCallback
import com.mazenrashed.printooth.databinding.ActivityScanningBinding
import com.mazenrashed.printooth.utilities.Bluetooth

private const val TAG = "ScanningActivity"

class ScanningActivity : AppCompatActivity(), (BluetoothDevice) -> Unit {

    private lateinit var bluetooth: Bluetooth
    private var devices = ArrayList<BluetoothDevice>()
    private lateinit var adapter: BluetoothDevicesAdapter

    private lateinit var binding: ActivityScanningBinding

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BluetoothDevicesAdapter(this)
        bluetooth = Bluetooth(this)


        initViews()
        setup()
    }

    private fun setup() {
        binding.refreshLayout.setOnRefreshListener { startBluetoothScanning() }

        if (hasBluetoothPermissions()) {
            initDeviceCallback()
            startBluetoothScanning()
        } else {
            requestBluetoothPermissions()
        }
    }

    private fun initDeviceCallback() {
        bluetooth.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
               // binding.refreshLayout.isRefreshing = true
                binding.toolbar.title = "Scanning.."
                devices.clear()
                adapter.updateList(devices)
            }

            override fun onDiscoveryFinished() {
                binding.toolbar.title =
                    if (devices.isNotEmpty()) "Select a Printer" else "No devices"
             //   binding.refreshLayout.isRefreshing = false
                adapter.updateList(devices)
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceFound(device: BluetoothDevice) {
                if (!devices.contains(device) && (device.name != null)) {
                    devices.add(device)
                    adapter.updateList(devices)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDevicePaired(device: BluetoothDevice) {
                Printooth.setPrinter(device.name, device.address)
                Toast.makeText(this@ScanningActivity, "Device Paired", Toast.LENGTH_SHORT).show()
                adapter.updateList(devices)
                setResult(Activity.RESULT_OK)
                this@ScanningActivity.finish()
                Log.d(TAG, "onDevicePaired: ${device.name}")
            }

            override fun onDeviceUnpaired(device: BluetoothDevice) {
                Toast.makeText(this@ScanningActivity, "Device unpaired", Toast.LENGTH_SHORT).show()
                val pairedPrinter = Printooth.getPairedPrinter()
                if (pairedPrinter != null && pairedPrinter.address == device.address)
                    Printooth.removeCurrentPrinter()
                devices.remove(device)
                adapter.updateList(devices)
                bluetooth.startScanning()
            }

            override fun onError(message: String) {
                Toast.makeText(this@ScanningActivity, "Error while pairing", Toast.LENGTH_SHORT).show()
                adapter.updateList(devices)
            }
        })
    }


    private fun initViews() {
        binding.printers.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ScanningActivity)
            adapter = this@ScanningActivity.adapter
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        for (permission in bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, bluetoothPermissions, REQUEST_BLUETOOTH)
    }

    private fun startBluetoothScanning() {
        runWithPermissions(Permission.ACCESS_FINE_LOCATION) {
            bluetooth.onStart()
            if (!bluetooth.isEnabled) {
                bluetooth.enable()
            }
            binding.root.postDelayed({
                bluetooth.startScanning()
            }, 1000)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasBluetoothPermissions()) {
            startBluetoothScanning()
        } else {
            requestBluetoothPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        bluetooth.onStop()
    }

    override fun invoke(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Printooth.setPrinter(device.name, device.address)
                setResult(Activity.RESULT_OK)
                finish()
            } else if (device.bondState == BluetoothDevice.BOND_NONE)
                bluetooth.pair(device)
            adapter.updateList(devices)
        }
    }

    companion object {
        const val REQUEST_BLUETOOTH = 1
        const val SCANNING_FOR_PRINTER = 115
    }
}
