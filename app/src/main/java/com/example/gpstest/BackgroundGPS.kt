package com.example.gpstest

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.amazonaws.services.iot.client.AWSIotMqttClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.example.gpstest.SampleUtil.KeyStorePasswordPair
import android.annotation.SuppressLint
import android.app.Notification
import android.util.Log
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.amazonaws.services.iot.client.*
import android.app.NotificationManager

import android.app.NotificationChannel
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat





class BackgroundGPS : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val clientEndpoint = "a3ehaer69y3v5p-ats.iot.eu-west-1.amazonaws.com" // use value returned by describe-endpoint --endpoint-type "iot:Data-ATS"
        val clientId = "mobile-franciscojavier" // replace with your own client ID. Use unique client IDs for concurrent connections.
        val certificateFile = assets.open("certificate.pem.crt") // X.509 based certificate file
        val privateKeyFile = assets.open("private.pem.key") // PKCS#1 or PKCS#8 PEM encoded private key file
        val pair: KeyStorePasswordPair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile)
        val client = AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword)

        client.connect()
        checkLocation(client)
        startLocationUpdates()
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {

        
        var NOTIFICATION_CHANNEL_ID = "gps1"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
            .setContentTitle("Hello \uD83D\uDC40")
            .setContentText("I'm accessing to location in background...")
            .setAutoCancel(true)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()



        startForeground(3, notification)


    }



    private fun checkLocation(client: AWSIotMqttClient){
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
            //finish()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun getLocationUpdates(client: AWSIotMqttClient) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 2000
        locationRequest.smallestDisplacement = 10f
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
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        stopLocationUpdates()
    }
}