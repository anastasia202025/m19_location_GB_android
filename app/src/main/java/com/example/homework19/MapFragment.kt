package com.example.homework19

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import com.example.homework19.client.RetrofitClient.fetchLandmarks
import com.example.homework19.model.Landmark
import java.lang.ref.WeakReference
import com.example.homework19.databinding.FragmentMapBinding as FragmentMapBinding1

class MapFragment : Fragment(), LocationListener {

    private var _binding: FragmentMapBinding1? = null
    private val binding get() = _binding!!

    private lateinit var locationManager: LocationManager
    private var map: MapView? = null

    private val loadedLandmarks = mutableListOf<Landmark>()
    private val placemarkDataMap = mutableMapOf<PlacemarkMapObject, Landmark>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey(getApiKeyFromManifest(requireContext()))
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding1.inflate(inflater, container, false)
        return binding.root
    }

    private fun getApiKeyFromManifest(context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        return appInfo.metaData.getString("API_KEY").toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasLocationPermissions()) {
            map = binding.mapView
            locationManager = MapKitFactory.getInstance().createLocationManager()
        } else {
            requestLocationPermissions()
        }
        setupMapView()
        setupButtons()
    }


    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION_PERMISSION
        )
    }

    private fun setupButtons() {
        val buttonZoomIn: ImageButton = binding.root.findViewById(R.id.button_zoom_in)
        val buttonZoomOut: ImageButton = binding.root.findViewById(R.id.button_zoom_out)
        val buttonMyLocation: ImageButton = binding.root.findViewById(R.id.button_my_location)

        buttonZoomIn.setOnClickListener {
            map?.map?.cameraPosition?.target?.let { it1 ->
                CameraPosition(
                    it1,
                    map?.map?.cameraPosition?.zoom?.plus(1.0f) ?: 15.0f,
                    0.0f,
                    0.0f
                )
            }?.let { it2 ->
                map?.map?.move(
                    it2
                )
            }
        }

        buttonZoomOut.setOnClickListener {
            map?.map?.cameraPosition?.target?.let { it1 ->
                CameraPosition(
                    it1,
                    map?.map?.cameraPosition?.zoom?.minus(1.0f) ?: 15.0f,
                    0.0f,
                    0.0f
                )
            }?.let { it2 ->
                map?.map?.move(
                    it2
                )
            }
        }

        buttonMyLocation.setOnClickListener {
            map?.map?.mapObjects?.clear()
            setupMapView()
        }
    }

    private fun setupMapView() {
        map?.let { mapView ->
            mapView.map?.let { yandexMap ->
                yandexMap.mapObjects.clear()

                yandexMap.addCameraListener { map, cameraPosition, cameraUpdateReason, finished ->
                    if (finished) {
                        refreshVisibleLandmarks()
                    }
                }

                locationManager.requestSingleUpdate(
                    object : LocationListener {
                        override fun onLocationUpdated(location: Location) {
                            yandexMap.mapObjects.clear()
                            loadLandmarks(location.position.latitude, location.position.longitude)
                            fetchAndDisplayUserLocation()
                        }

                        override fun onLocationStatusUpdated(p0: LocationStatus) {
                        }
                    }
                )
            }
        }
    }

    private fun loadLandmarks(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val landmarks = fetchLandmarks(requireContext(), latitude, longitude)
                if (!landmarks.isNullOrEmpty()) {
                    displayLandmarks(landmarks)
                } else {
                }
            } catch (e: Exception) {
            } finally {
                fetchAndDisplayUserLocation()
            }
        }
    }

    private fun displayLandmarks(landmarks: List<Landmark>) {
        map?.let { mapView ->
            mapView.map?.let { yandexMap ->
                landmarks.forEach { landmark ->
                    val point = Point(landmark.latitude, landmark.longitude)
                    val placemark = yandexMap.mapObjects.addPlacemark(
                        point,
                        ImageProvider.fromBitmap(
                            drawableToBitmap(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_landmark
                                )!!
                            )
                        ),
                        IconStyle().apply {
                            anchor = PointF(0.5f, 0.5f)
                        }
                    )

                    placemarkDataMap[placemark] = landmark
                    placemark.addTapListener { _, _ ->
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle(landmark.name)
                        builder.setMessage(landmark.description ?: "No description available")
                        builder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        val dialog = builder.create()
                        dialog.show()
                        true
                    }
                }
            }
        }
    }

    private fun isPointInVisibleRegion(point: Point, visibleRegion: VisibleRegion): Boolean {
        val topLeft = visibleRegion.topLeft
        val bottomRight = visibleRegion.bottomRight

        return point.latitude in bottomRight.latitude..topLeft.latitude &&
                point.longitude in topLeft.longitude..bottomRight.longitude
    }

    private fun refreshVisibleLandmarks() {
        val yandexMap = map?.map ?: return

        val visibleRegion = yandexMap.visibleRegion

        val visibleLandmarks = loadedLandmarks.filter { landmark ->
            val point = Point(landmark.latitude, landmark.longitude)
            isPointInVisibleRegion(point, visibleRegion)
        }

        yandexMap.mapObjects.clear()
        visibleLandmarks.forEach { landmark ->
            val point = Point(landmark.latitude, landmark.longitude)
            val placemark = yandexMap.mapObjects.addPlacemark(
                point,
                ImageProvider.fromResource(
                    requireContext(),
                    R.drawable.ic_landmark
                ),
                IconStyle().apply {
                    anchor = PointF(0.5f, 0.5f)
                }
            )
            placemark.zIndex = 0f

            placemarkDataMap[placemark] = landmark
            placemark.addTapListener { _, _ ->
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(landmark.name)
                builder.setMessage(landmark.description ?: "No description available")
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
                true
            }
        }
    }

    private fun fetchAndDisplayUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = Point(location.latitude, location.longitude)
                    map?.map?.move(
                        CameraPosition(userLocation, 15.0f, 0.0f, 0.0f)
                    )
                    val bitmap = drawableToBitmap(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_vector)!!)
                    map?.map?.mapObjects?.addPlacemark(
                        userLocation,
                        ImageProvider.fromBitmap(bitmap)
                    )
                }
            }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    override fun onStart() {
        super.onStart()
        map?.onStart()
    }

    override fun onStop() {
        super.onStop()
        map?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map?.onStop()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMapView()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 1
    }

    override fun onLocationUpdated(p0: Location) {
    }

    override fun onLocationStatusUpdated(p0: LocationStatus) {
    }
}