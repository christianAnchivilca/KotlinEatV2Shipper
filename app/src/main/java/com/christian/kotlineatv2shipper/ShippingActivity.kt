package com.christian.kotlineatv2shipper

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.christian.kotlineatv2shipper.common.Common
import com.christian.kotlineatv2shipper.model.ShippingOrderModel
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_shipping.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var shipperMarker:Marker?=null
    private var shippingOrderModel:ShippingOrderModel?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipping)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        buildLocationRequest()
        buildLocationCallback()
        setShippingOrderModel()

        Dexter.withContext(this@ShippingActivity)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this@ShippingActivity)
                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@ShippingActivity)
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                        Looper.myLooper())
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(this@ShippingActivity,"Permiso denegado", Toast.LENGTH_SHORT).show()
                }

            }).check()



    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.SHIPPING_DATA)
        if (!TextUtils.isEmpty(data))
        {
            shippingOrderModel = Gson().fromJson<ShippingOrderModel>(data,object:TypeToken<ShippingOrderModel>(){}.type)
            if (shippingOrderModel != null){
                Common.setSpanStringColor("Nombre: ",shippingOrderModel!!.orderModel!!.userName,txt_name,
                    Color.parseColor("#333639"))
                Common.setSpanStringColor("Direcciòn: ",shippingOrderModel!!.orderModel!!.shippingAddress,txt_address,
                    Color.parseColor("#673ab7"))
                Common.setSpanStringColor("No.: ",shippingOrderModel!!.orderModel!!.key,txt_order_number,
                    Color.parseColor("#795548"))
                txt_date.text = StringBuilder(SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(
                    shippingOrderModel!!.orderModel!!.createDate
                ))

                Glide.with(this).load(shippingOrderModel!!.orderModel!!.cartItemList!![0]!!.foodImage)
                    .into(img_food_image)
            }


        }else{
            Toast.makeText(this,"Shipping order model is null", Toast.LENGTH_SHORT).show()
        }


    }

    private fun buildLocationCallback() {
        locationCallback = object:LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                val locationShipper = LatLng(p0!!.lastLocation.latitude,p0!!.lastLocation.longitude)
                if (shipperMarker == null)
                {
                    val height = 80
                    val width = 80
                    val bitmapDrawable = ContextCompat.getDrawable(this@ShippingActivity,R.drawable.ic_motorcycle_black_24dp)
                    val b = bitmapDrawable!!.toBitmap()
                    val smallMarker = Bitmap.createScaledBitmap(b,width,height,false)
                    shipperMarker = mMap!!.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                        .position(locationShipper)
                        .title("Tù"))

                }
                else
                {
                    shipperMarker!!.position = locationShipper

                }
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,15f))



            }
        }

    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(15000) // 15 seg
        locationRequest.setFastestInterval(10000) //10 seg
        locationRequest.setSmallestDisplacement(20f)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        /* Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.uber_light_with_label))
            if (!success)
                Log.d("EDMTDEV","Failed to load map style")
        }catch (ex:Resources.NotFoundException){
            Log.d("EDMTDEV","not found json string for map style")

        }

    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}
