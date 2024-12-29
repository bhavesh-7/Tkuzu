package com.example.tkuzudriver.activities.ui.home


import io.reactivex.rxjava3.core.*
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.Button
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tkuzudriver.Constants
import com.example.tkuzudriver.R
import com.example.tkuzudriver.UserUtils
import com.example.tkuzudriver.databinding.FragmentHomeBinding
import com.example.tkuzudriver.model.DriverRequestReceived
import com.example.tkuzudriver.remote.GoogleAPI
import com.example.tkuzudriver.remote.RetrofitClient
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit


class HomeFragment : Fragment(), OnMapReadyCallback {
    //Views
    private lateinit var chip_decline: Chip
    private lateinit var chip_accept: Chip
    private lateinit var chip_otp_verify: Chip
    private lateinit var layout_accept: CardView
    private lateinit var circulProgressBar: CircularProgressBar
    private lateinit var txt_estimate_time: TextView
    private lateinit var txt_estimate_distance: TextView
    private lateinit var root_layout: FrameLayout

    private var driverRequestReceived: DriverRequestReceived? = null
    private var countDownEvent: Disposable? = null

    //Routes
    private val compositeDisposable = CompositeDisposable()
    private lateinit var googleAPI: GoogleAPI
    private var blackPolyLine: Polyline? = null
    private var greyPolyLine: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolyLineOptions: PolylineOptions? = null
    private var polylineList: MutableList<LatLng>? = null

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private var _binding: FragmentHomeBinding? = null

    //Location
    private lateinit var locationCallBack: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Online system
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire


    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserRef != null) {
                currentUserRef.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }

    }

    private val binding get() = _binding!!


    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initViews(root)
        init()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    private fun init() {
        googleAPI = RetrofitClient.instance!!.create(GoogleAPI::class.java)

        onlineRef = FirebaseDatabase.getInstance().reference.child(" info/connected")

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 15000
        ).build()

        locationCallBack = object : LocationCallback() {
            override fun onLocationAvailability(p0: LocationAvailability) {

            }

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                val addressList: List<Address>?
                try {
                    addressList = geoCoder.getFromLocation(
                        locationResult.lastLocation!!.latitude,
                        locationResult.lastLocation!!.longitude, 1
                    )
                    val cityName = addressList!![0].locality
                    Log.d("ORAS", cityName)

                    driversLocationRef =
                        FirebaseDatabase.getInstance()
                            .getReference(Constants.DRIVERS_LOCATION_REFERENCE)
                            .child(cityName)
                    currentUserRef = driversLocationRef
                        .child(FirebaseAuth.getInstance().currentUser?.uid!!)

                    geoFire = GeoFire(driversLocationRef)

                    //Update Location
                    geoFire.setLocation(
                        FirebaseAuth.getInstance().currentUser?.uid,
                        GeoLocation(
                            locationResult.lastLocation!!.latitude,
                            locationResult.lastLocation!!.longitude
                        )
                    ) { key: String, error: DatabaseError? ->
                        if (error != null) {
                            Snackbar.make(
                                mapFragment.requireView(),
                                error.message,
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(
                                mapFragment.requireView(),
                                "You're online!",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }

                    registerOnlineSystem()


                } catch (e: IOException) {
                    Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                }
            }
        }
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallBack,
            Looper.getMainLooper()
        )

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
     fun onDriverRequestReceived(event: DriverRequestReceived) {
        driverRequestReceived = event
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(), "You need permission!", Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                //Copied code from RiderClone
                compositeDisposable.add(
                    googleAPI.getDirections(
                        "driving",
                        "less_driving",
                        StringBuilder()
                            .append(location.latitude)
                            .append(",")
                            .append(location.longitude)
                            .toString(),
                        event.pickupLocation,
                        getString(R.string.api_key)
                    )
                    !!.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { returnResult ->
                                Log.d("API_RETURN", returnResult)
                                try {

                                    val jsonObject = JSONObject(returnResult)
                                    val jsonArray = jsonObject.getJSONArray("routes")
                                    for (i in 0 until jsonArray.length()) {
                                        val route = jsonArray.getJSONObject(i)
                                        val poly = route.getJSONObject("overview_polyline")
                                        val polyLine = poly.getString("points")
                                        polylineList = Constants.decodePoly(polyLine)
                                    }

                                    polylineOptions = PolylineOptions()
                                    polylineOptions!!.color(Color.GRAY)
                                    polylineOptions!!.width(12f)
                                    polylineOptions!!.startCap(SquareCap())
                                    polylineOptions!!.jointType(JointType.ROUND)
                                    polylineOptions!!.addAll(polylineList!!)
                                    greyPolyLine = mMap.addPolyline(polylineOptions!!)

                                    blackPolyLineOptions = PolylineOptions()
                                    blackPolyLineOptions!!.color(Color.BLACK)
                                    blackPolyLineOptions!!.width(5f)
                                    blackPolyLineOptions!!.startCap(SquareCap())
                                    blackPolyLineOptions!!.jointType(JointType.ROUND)
                                    blackPolyLineOptions!!.addAll(polylineList!!)
                                    blackPolyLine = mMap.addPolyline(blackPolyLineOptions!!)

                                    //Animator
                                    val valueAnimator = ValueAnimator.ofInt(0, 100)
                                    valueAnimator.duration = 1100
                                    valueAnimator.repeatCount = ValueAnimator.INFINITE
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener { value ->
                                        val points = greyPolyLine!!.points
                                        val percentValue =
                                            valueAnimator.animatedValue.toString().toInt()
                                        val size = points.size
                                        val newPoints = (size * (percentValue / 100f)).toInt()
                                        val p = points.subList(0, newPoints)
                                        blackPolyLine!!.points = p
                                    }
                                    valueAnimator.start()


                                    val origin = LatLng(location.latitude, location.longitude)
                                    val destination = LatLng(
                                        event.pickupLocation.split(",")[0].toDouble(),
                                        event.pickupLocation.split(",")[1].toDouble()
                                    )

                                    val latLngBound =
                                        LatLngBounds.Builder()
                                            .include(origin)
                                            .include(destination)
                                            .build()

                                    val objects = jsonArray.getJSONObject(0)
                                    val legs = objects.getJSONArray("legs")
                                    val legsObject = legs.getJSONObject(0)

                                    val time = legsObject.getJSONObject("duration")
                                    val duration = time.getString("text")

                                    val distanceEstimate = legsObject.getJSONObject("distance")
                                    val distance = distanceEstimate.getString("text")

                                    txt_estimate_time.text = duration
                                    txt_estimate_distance.text = distance

                                    mMap.addMarker(
                                        MarkerOptions()
                                            .position(destination)
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_map_location))
                                            .title("Pickup Location")
                                    )

                                    mMap.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            latLngBound,
                                            160
                                        )
                                    )
                                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1))

                                    //Display layout
                                    chip_decline.visibility = View.VISIBLE
                                    chip_accept.visibility = View.VISIBLE
                                    layout_accept.visibility = View.VISIBLE

                                    //Countdown
                                   countDownEvent = Observable.interval(
                                        100,
                                        TimeUnit.MICROSECONDS
                                    )
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doOnNext { x ->
                                            circulProgressBar.progress += 1f
                                        }
                                        .takeUntil { aLong -> aLong == "1000".toLong() } //100 seconds
                                        .doOnComplete {
                                            Toast.makeText(
                                                requireContext(),
                                                "Request denied",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.subscribe(
                                            { _ -> },
                                            { error ->
                                                Log.d("reactivex", error.message!!)
                                                // Handle the error here, e.g., show an error message
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Error: ${error.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            )

                                } catch (e: Exception) {
                                    Snackbar.make(
                                        mapFragment.requireView(),
                                        e.message!!,
                                        Snackbar.LENGTH_LONG
                                    )
                                        .show()
                                }
                            },
                            { error ->
                                // Handle the error case
                                Log.e("API_ERROR", error.message, error)
                                // Display an error message
                            }
                        )

                    )
            }
    }

    private fun initViews(view: View?) {
        chip_decline = view?.findViewById(R.id.chip_decline) as Chip
        chip_accept = view?.findViewById(R.id.chip_accept) as Chip
        chip_otp_verify = view?.findViewById(R.id.chip_otp_verify) as Chip
        layout_accept = view.findViewById(R.id.layout_accept) as CardView
        circulProgressBar = view.findViewById(R.id.circul_progress_bar) as CircularProgressBar
        txt_estimate_time = view.findViewById(R.id.text_estimate_time)
        txt_estimate_distance = view.findViewById(R.id.text_estimate_distance)
        root_layout = view.findViewById(R.id.root_layout)

        chip_decline.setOnClickListener {
            if (driverRequestReceived != null) {
                if (countDownEvent != null) {
                    countDownEvent!!.dispose()
                }

                chip_decline.visibility = View.GONE
                chip_accept.visibility = View.GONE
                layout_accept.visibility = View.GONE
                mMap.clear()
                circulProgressBar.progress = 0f
                UserUtils.sendDeclineRequest(root_layout,activity,driverRequestReceived!!.key)
                driverRequestReceived = null
            }
        }
        chip_accept.setOnClickListener {
            if (driverRequestReceived != null) {
                if (countDownEvent != null) {
                    countDownEvent!!.dispose()
                }

                chip_decline.visibility = View.GONE
                chip_accept.visibility = View.GONE
                chip_otp_verify.visibility = View.VISIBLE
                //layout_accept.visibility = View.GONE

                UserUtils.driverConfirmed(root_layout,activity,driverRequestReceived!!.key, txt_estimate_time.text.toString())

            }
        }
        chip_otp_verify.setOnClickListener {
            if (driverRequestReceived != null) {
                if (countDownEvent != null) {
                    countDownEvent!!.dispose()
                }

                // Generate OTP
                val otp = generateOTP()

                // Send OTP to the user
                UserUtils.sendOTPRequest(root_layout, activity, driverRequestReceived!!.key, otp)

                // Show OTP popup for the driver
                showOTPPopup(requireContext(), otp)
            }
        }
    }
    fun showOTPPopup(context: Context, otp: String) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_otp_popup, null)

        // Find views in the OTP popup layout
        val otpTitleTextView = view.findViewById<TextView>(R.id.text_otp_title)
        val otpEditText = view.findViewById<EditText>(R.id.edit_otp)
        val verifyButton = view.findViewById<Button>(R.id.btn_verify_otp)

        // Set the OTP text and show the OTP popup
        otpTitleTextView.text = "Enter OTP"

        // Build the OTP dialog
        val otpDialogBuilder = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)

        // Create and show the OTP dialog
        val otpDialog = otpDialogBuilder.create()
        otpDialog.show()

        // Set a click listener for the verify button
        verifyButton.setOnClickListener {
            // Get the entered OTP from the EditText
            val enteredOTP = otpEditText.text.toString()

            // Perform OTP verification logic
            if (enteredOTP == otp) {
                otpDialog.dismiss()
                chip_otp_verify.visibility = View.GONE
                UserUtils.tkuzuConfirmed(root_layout,activity,driverRequestReceived!!.key)
                //fetchRouteAndDisplayOnMap()
            } else {
                otpEditText.error = "Invalid OTP"
            }
        }
    }

    fun generateOTP(): String {
        val otpLength = 6
        val otp = StringBuilder()

        repeat(otpLength) {
            otp.append((0..9).random())
        }

        return otp.toString()
    }



    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser?.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()

        if (EventBus.getDefault().hasSubscriberForEvent(HomeFragment::class.java))
            EventBus.getDefault().removeStickyEvent(HomeFragment::class.java)
        EventBus.getDefault().unregister(this)

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Request Permissions
        Dexter.withContext(context)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(permissions: PermissionGrantedResponse?) {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationButtonClickListener() {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener {
                                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                            }.addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        10f
                                    )
                                )
                            }
                        true
                    }


                    val view = mapFragment.view?.findViewById<View>("1".toInt())?.parent as View
                    val locationButton = view.findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 250
                }

                override fun onPermissionDenied(permissions: PermissionDeniedResponse?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()

        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.tkuzu_maps_style
                )
            )
            if (!success) {
                Log.d("Google Map", "error")
            }
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        }
    }
}