package com.example.gpstest

import android.Manifest
import com.example.gpstest.SampleUtil.KeyStorePasswordPair
import android.annotation.SuppressLint
import android.content.*
import android.util.Log
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.*
import com.google.android.gms.location.*
import com.amazonaws.services.iot.client.*
import android.content.Intent
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startService(Intent(this, BackgroundGPS::class.java))


        val clientEndpoint = "a3ehaer69y3v5p-ats.iot.eu-west-1.amazonaws.com" // use value returned by describe-endpoint --endpoint-type "iot:Data-ATS"
        val clientId = "mobile" // replace with your own client ID. Use unique client IDs for concurrent connections.
        val certificateFile = assets.open("certificate.pem.crt") // X.509 based certificate file
        val privateKeyFile = assets.open("private.pem.key") // PKCS#1 or PKCS#8 PEM encoded private key file
        val pair: KeyStorePasswordPair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile)
        val client = AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword)

        client.connect()
        checkLocation(client)
    }

    /*private fun checkLocation(client: AWSIotMqttClient ){
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlertLocation()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationUpdates(client)
    }

    private fun showAlertLocation() {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("Your location settings is set to Off, Please enable location to use this application")
        dialog.setPositiveButton("Settings") { _, _ ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(myIntent)
        }
        dialog.setNegativeButton("Cancel") { _, _ ->
            finish()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun getLocationUpdates(client: AWSIotMqttClient) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 5000
        locationRequest.smallestDisplacement = 170f //170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    val unixTime = System.currentTimeMillis() / 1000L
                    val json = "{ \"payload\": { \"deviceid\": \""+client.clientId+"\", \"timestamp\": "+unixTime+",  \"location\": { \"lat\": "+location.latitude+", \"long\": "+location.longitude+" } } }";
                    Log.e("json:", json)
                    val message = AWSIotMessage("iot/trackers/"+client.clientId, AWSIotQos.QOS0, json.toByteArray())
                    try {
                        client.publish(message)
                    } catch (e: AWSIotException) {
                        println(System.currentTimeMillis().toString() + ": publish failed for " + json)
                    }
                }
            }
        }
    }

    // Start location updates
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }
*/
    // Stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Stop receiving location update when activity not visible/foreground
    /*override fun onPause() {
        super.onPause()
       // stopLocationUpdates()
    }

    // Start receiving location update when activity  visible/foreground
    override fun onResume() {
        super.onResume()
        //startLocationUpdates()
    }*/

}

