package com.christian.kotlineatv2shipper

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.christian.kotlineatv2shipper.common.Common
import com.christian.kotlineatv2shipper.model.ShippingOrderModel
import com.christian.kotlineatv2shipper.remote.IGoogleApi
import com.christian.kotlineatv2shipper.remote.RetrofitClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_shipping.*
import org.json.JSONObject

import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var shipperMarker:Marker?=null
    private var shippingOrderModel:ShippingOrderModel?=null
    var isInit = false
    var previusLocation:Location?=null

    private var handler:Handler?=null
    private var index:Int=-1
    private var next:Int=0
    private var startPosition:LatLng?=LatLng(0.0,0.0)
    private var endPosition:LatLng?=LatLng(0.0,0.0)
    private var v:Float=0f
    private var lat:Double = 1.0
    private var lng:Double = 1.0

    private var blackPolyline:Polyline?=null
    private var greyPolyline:Polyline?=null
    private var polylineOptions:PolylineOptions? = null
    private var blackPolylineOptions:PolylineOptions? = null
    private var redPolyline:Polyline? = null
    private var yellowPolyline:Polyline?=null

    private var polylineList:List<LatLng> = ArrayList<LatLng>()
    private var iGoogleApi:IGoogleApi?=null
    private var compositeDisposable = CompositeDisposable()
    //google places
    private lateinit var places_fragment:AutocompleteSupportFragment
    private lateinit var placesClient:PlacesClient
    private val placeField = Arrays.asList(Place.Field.ID,Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipping)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        iGoogleApi = RetrofitClient.instance!!.create(IGoogleApi::class.java)
        initPlaces()

        buildLocationRequest()
        buildLocationCallback()


        //PERMISO PARA ACCEDER A LA UBICACION ACTUAL
        Dexter.withContext(this@ShippingActivity)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                @SuppressLint("MissingPermission")
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


        initViews()
    }

    private fun initPlaces() {
        Places.initialize(this,getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
        setupPlaceAutocomplete()


    }

    private fun setupPlaceAutocomplete() {

        places_fragment = supportFragmentManager
                .findFragmentById(R.id.places_autocomplete_fragment) as AutocompleteSupportFragment
        places_fragment.setPlaceFields(placeField)
        places_fragment.setOnPlaceSelectedListener(object:PlaceSelectionListener{
            override fun onPlaceSelected(place: Place) {
                drawRoutes(place)
                /*Toast.makeText(this@ShippingActivity,StringBuilder(p0.name)
                    .append("-")
                    .append(p0.latLng).toString(),Toast.LENGTH_LONG).show()*/
            }
            override fun onError(p0: Status) {
                Toast.makeText(this@ShippingActivity,""+p0.statusMessage,Toast.LENGTH_LONG).show()
            }

        })

    }

    private fun initViews(){
        btn_start_trip.setOnClickListener {
            val data = Paper.book().read<String>(Common.SHIPPING_DATA)
            Paper.book().write(Common.TRIP_START,data)
            btn_start_trip.isEnabled = false
            shippingOrderModel=Gson().fromJson(data,object:TypeToken<ShippingOrderModel?>(){}.type)


           fusedLocationProviderClient.lastLocation.addOnSuccessListener {
               location->

               compositeDisposable.add(iGoogleApi.getDirections("driving"
                   ,"less_driving",
               Common.buildLocationString(location),
                   StringBuilder().append(shippingOrderModel!!.orderModel!!.lat)
                       .append(",")
                       .append(shippingOrderModel!!.orderModel!!.lng).toString(),
               getString(R.string.google_maps_key))!!
                   .subscribeOn(Schedulers.io())
                   .observeOn(AndroidSchedulers.mainThread())
                   .subscribe({s->

                       //get estimate time from API


                       val update_data = HashMap<String,Any>()
                       update_data.put("currentLat",location.latitude)
                       update_data.put("currentLng",location.longitude)

                       FirebaseDatabase.getInstance().getReference(Common.SHIPPING_ORDER_REF)
                           .child(shippingOrderModel!!.key!!)
                           .updateChildren(update_data)
                           .addOnFailureListener{error->
                               Toast.makeText(this@ShippingActivity,error.message,Toast.LENGTH_LONG).show()
                           }
                           .addOnSuccessListener {  aVoid->
                               drawRoutes(data)
                           }

                   },{t->
                       Toast.makeText(this@ShippingActivity,t.message,Toast.LENGTH_LONG).show()

                   }))

           }
        }

        btn_show.setOnClickListener {
            if (expandable_layout.isExpanded)
                btn_show.text = "MOSTRAR"
            else
                btn_show.text = "OCULTAR"
            expandable_layout.toggle()

        }

    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        var data:String?=""

        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_START))){
            data = Paper.book().read<String>(Common.SHIPPING_DATA)
            btn_start_trip.isEnabled = true
        }else{
            data = Paper.book().read<String>(Common.TRIP_START)
            btn_start_trip.isEnabled = false

        }


        if (!TextUtils.isEmpty(data))
        {
            drawRoutes(data)
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

    private fun drawRoutes(place: Place) {


        mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            .title(place.name)
            .snippet(place.address)
            .position(place.latLng!!))

        fusedLocationProviderClient.lastLocation
            .addOnFailureListener{e->Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()}
            .addOnSuccessListener {
                    location ->
                val to = StringBuilder().append(place.latLng!!.latitude)
                    .append(",")
                    .append(place.latLng!!.longitude).toString()
                val from = StringBuilder().append(location.latitude)
                    .append(",")
                    .append(location.longitude)
                    .toString()

                compositeDisposable.add(iGoogleApi!!.getDirections("driving","less_driving",
                    from,to,getString(R.string.google_maps_key))!!
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->

                            try {
                                val jsonObject = JSONObject(s)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for(i in 0 until jsonArray.length())
                                {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    polylineList = Common.decodePoly(polyline)
                                }

                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.YELLOW)
                                polylineOptions!!.width(12.0f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.endCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList)
                                yellowPolyline = mMap.addPolyline(polylineOptions)


                            }catch (e:Exception){
                                Log.d("DEBUG",e.message)

                            }

                        },{
                            Toast.makeText(this,""+it.message,Toast.LENGTH_SHORT).show()

                        }))
            }
    }

    private fun drawRoutes(data: String?) {
        val shippingOrderModel = Gson()
            .fromJson<ShippingOrderModel>(data,object:TypeToken<ShippingOrderModel>(){}.type)

        mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
            .title(shippingOrderModel.orderModel!!.userName)
            .snippet(shippingOrderModel.orderModel!!.shippingAddress)
            .position(LatLng(shippingOrderModel.orderModel!!.lat,shippingOrderModel.orderModel!!.lng)))

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener{e->Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()}
            .addOnSuccessListener {
                location ->
                val to = StringBuilder().append(shippingOrderModel.orderModel!!.lat)
                    .append(",")
                    .append(shippingOrderModel.orderModel!!.lng).toString()
                val from = StringBuilder().append(location.latitude)
                    .append(",")
                    .append(location.longitude)
                    .toString()

                compositeDisposable.add(iGoogleApi!!.getDirections("driving","less_driving",
                  from,to,getString(R.string.google_maps_key))
                    !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {s->

                            try {
                                val jsonObject = JSONObject(s)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for(i in 0 until jsonArray.length())
                                {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    polylineList = Common.decodePoly(polyline)
                                }

                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.RED)
                                polylineOptions!!.width(12.0f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.endCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList)
                                redPolyline = mMap.addPolyline(polylineOptions)


                            }catch (e:Exception){
                                Log.d("DEBUG",e.message)

                            }

                        },{
                            Toast.makeText(this,""+it.message,Toast.LENGTH_SHORT).show()

                        }))
            }





    }

    private fun buildLocationCallback() {
        locationCallback = object:LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                val locationShipper = LatLng(p0!!.lastLocation.latitude,p0!!.lastLocation.longitude)

                updateLocation(p0.lastLocation)

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
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18f))
                }


                if (isInit && previusLocation != null)
                {
                    /*val previousLocationLatLng = LatLng(previusLocation!!.latitude,previusLocation!!.longitude)
                    MarkerAnimation.animateMarkerToGB(shipperMarker!!,locationShipper,LatLngInterpolator.Spherical())
                    shipperMarker!!.rotation = Common.getBearing(previousLocationLatLng,locationShipper)
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLng(locationShipper))*/

                    val from = StringBuilder()
                        .append(previusLocation!!.latitude)
                        .append(",")
                        .append(previusLocation!!.longitude)
                    val to = StringBuilder()
                        .append(locationShipper.latitude)
                        .append(",")
                        .append(locationShipper.longitude)

                    moveMarkerAnimation(shipperMarker,from,to)
                    previusLocation = p0.lastLocation

                }
                if (!isInit){
//                    isInit = true
                    previusLocation = p0.lastLocation
                }
            }
        }

    }

    private fun updateLocation(lastLocation: Location?) {
        val update_data = HashMap<String,Any>()
        update_data.put("currentLat",lastLocation!!.latitude)
        update_data.put("currentLng",lastLocation!!.longitude)

        val data = Paper.book().read<String>(Common.TRIP_START)
        if (!TextUtils.isEmpty(data))
        {
            //PARSER TO JSON
            val shippingOrder = Gson().fromJson<ShippingOrderModel>(data,object:TypeToken<ShippingOrderModel>(){}.type)
            if (shippingOrder != null){
                FirebaseDatabase.getInstance()
                    .getReference(Common.SHIPPING_ORDER_REF)
                    .child(shippingOrder.key!!)
                    .updateChildren(update_data)
                    .addOnFailureListener{e->
                        Toast.makeText(this@ShippingActivity,""+e.message,Toast.LENGTH_LONG).show()
                    }
            }

        }else{
            Toast.makeText(this@ShippingActivity,"Porfavor presione Start Trip",Toast.LENGTH_LONG).show()
        }


    }

    private fun moveMarkerAnimation(marker: Marker?, from: StringBuilder, to: StringBuilder) {
        compositeDisposable.add(iGoogleApi!!.getDirections("driving",
            "less_driving",
            from.toString(),
            to.toString(),
            getString(R.string.google_maps_key))!!.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {s->
                    Log.d("DEBUG",s)
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for(i in 0 until jsonArray.length())
                        {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GRAY)
                        polylineOptions!!.width(5.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        greyPolyline = mMap!!.addPolyline(polylineOptions)

                        blackPolylineOptions = PolylineOptions()
                        blackPolylineOptions!!.color(Color.GRAY)
                        blackPolylineOptions!!.width(5.0f)
                        blackPolylineOptions!!.startCap(SquareCap())
                        blackPolylineOptions!!.endCap(SquareCap())
                        blackPolylineOptions!!.jointType(JointType.ROUND)
                        blackPolylineOptions!!.addAll(polylineList)
                        blackPolyline = mMap.addPolyline(blackPolylineOptions)

                        //Animator
                        val polylineAnimator = ValueAnimator.ofInt(0,100)
                        polylineAnimator.setDuration(2000)
                        polylineAnimator.setInterpolator(LinearInterpolator())
                        polylineAnimator.addUpdateListener {
                                valueAnimator: ValueAnimator ->
                            val points=greyPolyline!!.points
                            val porcentValue =Integer.parseInt(valueAnimator.animatedValue.toString())
                            val size = points.size
                            val newPoints = (size *(porcentValue /100.0f)).toInt()
                            val p = points.subList(0,newPoints)
                            blackPolyline!!.points = p

                        }

                        polylineAnimator.start()
                        //cart moving
                        index = -1
                        next = -1

                        val r = object:Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1)
                                {
                                    index++
                                    next=index+1
                                    startPosition = polylineList[index]
                                }

                                val valueAnimator = ValueAnimator.ofInt(0,1)
                                valueAnimator.setDuration(1500)
                                valueAnimator.setInterpolator(LinearInterpolator())
                                valueAnimator.addUpdateListener{ valueAnimator->
                                    v=valueAnimator.animatedFraction
                                    lat = v * endPosition!!.latitude + (1-v)*startPosition!!.latitude
                                    lng = v * endPosition!!.longitude +(1-v) * startPosition!!.longitude
                                    val newPos = LatLng(lat,lng)
                                    marker!!.position = newPos
                                    marker!!.setAnchor(0.5f,0.5f)
                                    marker!!.rotation = Common.getBearing(startPosition!!,newPos)

                                    mMap!!.moveCamera(CameraUpdateFactory.newLatLng(marker.position))

                                }
                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this,1500)
                            }


                        }

                        handler = Handler()
                        handler!!.postDelayed(r,1500)




                    }catch (e:Exception){
                        Log.d("DEBUG",e.message)

                    }

                },{
                    Toast.makeText(this,""+it.message,Toast.LENGTH_SHORT).show()

            }))



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
        setShippingOrderModel()

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
        compositeDisposable.clear()
        super.onDestroy()
    }
}
