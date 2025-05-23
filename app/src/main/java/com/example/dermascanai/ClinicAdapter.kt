package com.example.dermascanai

import android.graphics.BitmapFactory
import android.location.Geocoder
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemClinicViewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ClinicAdapter(
    private val activity: AppCompatActivity,
    private val clinics: List<ClinicInfo>,
    private val onClinicClick: (ClinicInfo) -> Unit
) : RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder>() {

    inner class ClinicViewHolder(val binding: ItemClinicViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClinicViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemClinicViewBinding.inflate(inflater, parent, false)
        return ClinicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClinicViewHolder, position: Int) {
        val clinic = clinics[position]
        val binding = holder.binding

        binding.textView47.text = clinic.clinicName
        binding.gmail.text = clinic.email
        binding.phone.text = clinic.clinicPhone

        // Decode Base64 image
        val base64Image = clinic.profileImage
        if (!base64Image.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.profileImageView.setImageResource(R.drawable.default_profile)
            }
        } else {
            binding.profileImageView.setImageResource(R.drawable.default_profile)
        }

        // Load location on map
        val mapFragment = activity.supportFragmentManager
            .findFragmentById(binding.popupMapFragment.id) as? SupportMapFragment

        mapFragment?.getMapAsync { googleMap ->
            val geocoder = Geocoder(activity)
            try {
                val addressList = geocoder.getFromLocationName(clinic.address ?: "", 1)
                if (!addressList.isNullOrEmpty()) {
                    val location = addressList[0]
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap.clear()
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    googleMap.addMarker(MarkerOptions().position(latLng).title(clinic.clinicName))
                }
            } catch (e: Exception) {
                Log.e("MapError", "Geocoding failed: ${e.message}")
            }
        }

        binding.root.setOnClickListener {
            onClinicClick(clinic)
        }
    }

    override fun getItemCount() = clinics.size
}
