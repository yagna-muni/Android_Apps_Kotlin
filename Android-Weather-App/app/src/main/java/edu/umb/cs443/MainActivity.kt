package edu.umb.cs443

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import java.net.URLConnection

private const val APIkey: String = "8118ed6ee68db2debfaaa5a44c832918"

class MainActivity : FragmentActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    lateinit var JSONResponsedata: String

    override fun onCreate(savedInstanceState: Bundle?) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mFragment.getMapAsync(this)
    }


    fun getWeatherInfo(v: View?) {

        try {
            val inputDataField = findViewById<EditText>(R.id.editText)
            val outputDataField = findViewById<TextView>(R.id.textView)
            val weatherIconField = findViewById<ImageView>(R.id.imageView)

            when {
                inputDataField.text.toString().isEmpty() -> {
                    Toast.makeText(
                        applicationContext,
                        "Enter some valid data !!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                inputDataField.text.toString().isDigitsOnly() -> {
                    JSONResponsedata =
                        URL("https://api.openweathermap.org/data/2.5/weather?zip=${inputDataField.text}&units=metric&appid=$APIkey").readText(
                            Charsets.UTF_8
                        )
                }
                else -> {
                    JSONResponsedata =
                        URL("https://api.openweathermap.org/data/2.5/weather?q=${inputDataField.text}&units=metric&appid=$APIkey").readText(
                            Charsets.UTF_8
                        )
                }
            }

            val coordJsonObject = JSONObject(JSONResponsedata).getJSONObject("coord")
            val longitude = coordJsonObject.getDouble("lon")
            val latitude = coordJsonObject.getDouble("lat")

            val icon = JSONObject(JSONResponsedata).getJSONArray("weather").getJSONObject(0)
                .getString("icon")
            val displayOutputTemperature =
                JSONObject(JSONResponsedata).getJSONObject("main").getString("temp") + " °C"
            outputDataField.text = displayOutputTemperature


            try {
                val urlConnection: URLConnection =
                    URL("https://openweathermap.org/img/wn/$icon.png").openConnection()
                urlConnection.connect()
                val bufferedInputStream = BufferedInputStream(urlConnection.getInputStream())
                weatherIconField.setImageBitmap(BitmapFactory.decodeStream(bufferedInputStream))
                bufferedInputStream.close()

            } catch (e: IOException) {
                Log.e("MYTAG", "Error Icon image")
                Toast.makeText(
                    applicationContext,
                    "Error Loading Bitmap Icon !!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))
            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12f))

        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Enter some valid data !!", Toast.LENGTH_SHORT)
                .show()
        }

    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
    }

    companion object {
        const val DEBUG_TAG = "edu.umb.cs443.MYMSG"
    }
}
