package com.example.dermascanai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dermascanai.databinding.FragmentHomeUserBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class UserHomeFragment : Fragment() {

    private var _binding: FragmentHomeUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var mDatabase: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var fullscreenMapContainer: ConstraintLayout
    private lateinit var backFromMap: ImageView
    private lateinit var fullMapContainer: FrameLayout

    private var clinicEventListener: ValueEventListener? = null
    private var tipEventListener: ValueEventListener? = null

    private val LOCATION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    // --------------------------------------------
    // MAIN LOGIC
    // --------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullscreenMapContainer = binding.root.findViewById(R.id.fullscreenMapContainer)
        backFromMap = binding.root.findViewById(R.id.backFromMap)
        fullMapContainer = binding.root.findViewById(R.id.fullMapContainer)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.viewAllNearby.setOnClickListener {
            fullscreenMapContainer.visibility = View.VISIBLE
            setupFullscreenMap()
        }

        backFromMap.setOnClickListener {
            fullscreenMapContainer.visibility = View.GONE
        }

        checkLocationPermission()
        setupDateTime()
        setupFirebase()
        setupDailyTips()
    }

    // --------------------------------------------
    // LOCATION PERMISSION
    // --------------------------------------------
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            setupMap()
        }
    }

    // --------------------------------------------
    // MINI MAP (HOME SCREEN)
    // --------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer)
                as? SupportMapFragment ?: SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction().replace(R.id.mapContainer, it).commit()
        }

        mapFragment.getMapAsync { googleMap ->

            // Fix scroll issue when inside ScrollView
            mapFragment.view?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }

            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isScrollGesturesEnabled = true
            }

            val defaultLocation = LatLng(7.4478, 125.8097)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13f))

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
            }

            // Load registered/partnered clinics from Firebase using addresses and geocode
            loadClinicsUsingAddress(googleMap)
        }
    }

    // --------------------------------------------
    // FULLSCREEN MAP
    // --------------------------------------------
    private fun setupFullscreenMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.fullMapContainer)
                as? SupportMapFragment ?: SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction().replace(R.id.fullMapContainer, it).commit()
        }

        mapFragment.getMapAsync { googleMap ->
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isScrollGesturesEnabled = true
            }

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
            }

            val defaultLocation = LatLng(7.4478, 125.8097)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13f))

            // Load clinics dynamically by address geocoding
            loadClinicsUsingAddress(googleMap)
        }
    }

    // --------------------------------------------
    // LOAD CLINICS FROM FIREBASE USING ADDRESS + GEOCODER
    // --------------------------------------------
    private fun loadClinicsUsingAddress(googleMap: GoogleMap) {
        val clinicRef = FirebaseDatabase.getInstance(
            "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("clinicInfo")

        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        clinicRef.get().addOnSuccessListener { snapshot ->
            for (snap in snapshot.children) {
                val clinic = snap.getValue(ClinicInfo::class.java)

                // Only load clinics with role derma and address present
                if (clinic != null &&
                    clinic.role?.lowercase() == "derma" &&
                    !clinic.address.isNullOrEmpty()
                ) {
                    try {
                        val results = geocoder.getFromLocationName(clinic.address!!, 1)

                        if (results != null && results.isNotEmpty()) {
                            val location = results[0]
                            val latLng = LatLng(location.latitude, location.longitude)

                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(clinic.name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // --------------------------------------------
    // PERMISSION RESULT
    // --------------------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupMap()
        }
    }

    // --------------------------------------------
    // TIMESTAMP
    // --------------------------------------------
    private fun setupDateTime() {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a")
        binding.dateTimeText.text = current.format(formatter)
    }

    // --------------------------------------------
    // FIREBASE USER + CLINIC LIST (HORIZONTAL)
    // --------------------------------------------
    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance(
            "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("userInfo")

        mAuth.currentUser?.uid?.let { getUserData(it) }

        val clinicList = mutableListOf<ClinicInfo>()
        val adapter = AdapterDermaHomeList(clinicList)
        binding.dermaRecycleView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dermaRecycleView.adapter = adapter

        val clinicRef = FirebaseDatabase.getInstance(
            "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("clinicInfo")

        clinicEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                clinicList.clear()

                for (snap in snapshot.children) {
                    val clinic = snap.getValue(ClinicInfo::class.java)
                    if (clinic?.role?.lowercase() == "derma") clinicList.add(clinic)
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        clinicRef.addValueEventListener(clinicEventListener!!)

        binding.dermaList.setOnClickListener {
            startActivity(Intent(requireContext(), DoctorLists::class.java))
        }
    }

    // --------------------------------------------
    // DAILY TIPS
    // --------------------------------------------
    private fun setupDailyTips() {
        val tipRef = FirebaseDatabase.getInstance(
            "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("dailyTips")

        tipEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return

                val tipsList = snapshot.children.toList()
                if (tipsList.isNotEmpty()) {
                    val randomTip = tipsList.random()
                    val tipText = randomTip.child("text").getValue(String::class.java)
                    val imageBase64 = randomTip.child("image_base64").getValue(String::class.java)

                    binding.dailyTips.text = tipText ?: "Stay tuned for more skin care tips!"

                    if (!imageBase64.isNullOrEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            binding.dailyImage.setImageBitmap(bitmap)
                            binding.dailyImage.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            binding.dailyImage.visibility = View.GONE
                        }
                    } else binding.dailyImage.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        tipRef.addValueEventListener(tipEventListener!!)
    }

    // --------------------------------------------
    // USER PROFILE
    // --------------------------------------------
    private fun getUserData(userId: String) {
        mDatabase.child(userId).get().addOnSuccessListener { snapshot ->
            if (_binding == null) return@addOnSuccessListener

            val user = snapshot.getValue(UserInfo::class.java)
            binding.fullName.text = user?.name ?: "User"

            if (!user?.profileImage.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(user.profileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.profileView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Glide.with(this).load(R.drawable.ic_profile).into(binding.profileView)
                }
            }
        }
    }

    // --------------------------------------------
    // CLEANUP
    // --------------------------------------------
    override fun onDestroyView() {
        super.onDestroyView()

        val db = FirebaseDatabase.getInstance(
            "https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )

        clinicEventListener?.let { db.getReference("clinicInfo").removeEventListener(it) }
        tipEventListener?.let { db.getReference("dailyTips").removeEventListener(it) }

        _binding = null
    }
}
