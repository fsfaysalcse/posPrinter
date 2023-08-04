package com.mazenrashed.example

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mazenrashed.example.databinding.ActivityMainBinding
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printable.ImagePrintable
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.data.printer.DefaultPrinter
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback

class MainActivity : AppCompatActivity() {

    private var printing: Printing? = null
    lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkBluetoothPermissions()
        initViews()
        initListeners()
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                Toast.makeText(
                    this,
                    "Bluetooth is not supported on this device",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else if (!bluetoothAdapter.isEnabled) {
                // Bluetooth is disabled, request to enable it
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                // Bluetooth is enabled, check for location permission
                checkLocationPermission()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Location permission not granted, request it
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Location permission granted, proceed with the app
            initializePrintooth()
        }
    }

    private fun initializePrintooth() {
        if (Printooth.hasPairedPrinter())
            printing = Printooth.printer()
    }

    private fun initViews() {
        binding.btnPiarUnpair.text =
            if (Printooth.hasPairedPrinter()) "Un-pair ${Printooth.getPairedPrinter()?.name}" else "Pair with printer"
    }

    private fun initListeners() {
        binding.btnPrint.setOnClickListener {
            if (!Printooth.hasPairedPrinter()) startActivityForResult(
                Intent(
                    this,
                    ScanningActivity::class.java
                ),
                ScanningActivity.SCANNING_FOR_PRINTER
            )
            else printSomePrintable()
        }

        binding.btnPrintImages.setOnClickListener {
            if (!Printooth.hasPairedPrinter()) startActivityForResult(
                Intent(
                    this,
                    ScanningActivity::class.java
                ),
                ScanningActivity.SCANNING_FOR_PRINTER
            )
            else printSomeImages()
        }

        binding.btnPiarUnpair.setOnClickListener {
            if (Printooth.hasPairedPrinter()) Printooth.removeCurrentPrinter()
            else startActivityForResult(
                Intent(this, ScanningActivity::class.java),
                ScanningActivity.SCANNING_FOR_PRINTER
            )
            initViews()
        }

        printing?.printingCallback = object : PrintingCallback {
            override fun connectingWithPrinter() {
                Toast.makeText(this@MainActivity, "Connecting with printer", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun printingOrderSentSuccessfully() {
                Toast.makeText(this@MainActivity, "Order sent to printer", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun connectionFailed(error: String) {
                Toast.makeText(this@MainActivity, "Failed to connect printer", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(error: String) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }

            override fun onMessage(message: String) {
                Toast.makeText(this@MainActivity, "Message: $message", Toast.LENGTH_SHORT).show()
            }

            override fun disconnected() {
                Toast.makeText(this@MainActivity, "Disconnected Printer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun printSomePrintable() {
        val userList = ArrayList<User>()

        userList.add(User("Faysal Hossain", "A4565656", "L", "FX456", "Male"))
        userList.add(User("Abram Varughese", "457647868", "M", "FX457", "Male"))
        userList.add(User("Seema T.", "457647868", "S", "FX458", "Female"))

        val printables = getPrintables(userList)
        printing?.print(printables)
    }

    private fun printSomeImages() {
        val printables = ArrayList<Printable>().apply {
            add(ImagePrintable.Builder(R.drawable.image1, resources).build())
            add(ImagePrintable.Builder(R.drawable.image2, resources).build())
            add(ImagePrintable.Builder(R.drawable.image3, resources).build())
        }

        val thankYou = TextPrintable.Builder()
            .setText("Thank you for your participation")
            .setLineSpacing(DefaultPrinter.LINE_SPACING_30)
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setNewLinesAfter(4)
            .build()
        printables.add(thankYou)
        printing?.print(printables)
    }

    private fun getPrintables(userList: List<User>): ArrayList<Printable> {
        val printables = ArrayList<Printable>()

        val bigHeaderText = "COUNTER : A"
        val bigHeaderPrintable = TextPrintable.Builder()
            .setText(bigHeaderText)
            .setLineSpacing(DefaultPrinter.LINE_SPACING_30)
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
            .setFontSize(2) // You can adjust this value for a larger font size
            .setNewLinesAfter(2)
            .build()

        printables.add(bigHeaderPrintable)

        for (user in userList) {
            val userPrintable = TextPrintable.Builder()
                .setText(
                    "Name: ${user.name}\n" +
                            "IC: ${user.ic}\n" +
                            "T - Shirt Size: ${user.tsize}\n" +
                            "Gender : ${user.gender}\n" +
                            "Bib : ${user.gender}"
                )
                .setLineSpacing(DefaultPrinter.LINE_SPACING_30)
                .setAlignment(DefaultPrinter.ALIGNMENT_LEFT)
                .setNewLinesAfter(1)
                .build()

            printables.add(userPrintable)

            val dotDivider = buildString {
                repeat(32) { append("-") }
            }
            val dottedLinePrintable = TextPrintable.Builder()
                .setText(dotDivider)
                .setLineSpacing(DefaultPrinter.LINE_SPACING_30)
                .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                .setNewLinesAfter(1)
                .build()
            printables.add(dottedLinePrintable)
        }

        return printables
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScanningActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK)
            printSomePrintable()
        initViews()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, initialize Printooth
                initializePrintooth()
            } else {
                // Location permission not granted, show an explanation or disable Bluetooth features
                showLocationPermissionDeniedDialog()
            }
        }
    }

    private fun showLocationPermissionDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
        builder.setMessage("To use Bluetooth, this app requires access to your device's location.")
        builder.setPositiveButton("Settings") { _, _ ->
            // Open app settings to enable location permission manually
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Close the app or disable Bluetooth-related features
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}
