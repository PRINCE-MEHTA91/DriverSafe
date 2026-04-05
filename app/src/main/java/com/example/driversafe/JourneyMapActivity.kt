package com.example.driversafe

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.driversafe.databinding.ActivityJourneyMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage

class JourneyMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityJourneyMapBinding
    private lateinit var map: GoogleMap
    private lateinit var mapView: MapView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences

    private val sleepThresholdMs = 2000L
    private var lastClosedTime = 0L
    private var eyesClosed = false
    private var isAlertTriggered = false

    // ✅ Modern Permission Handler
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (cameraGranted && locationGranted) {
            startCamera()
            startLocation()
        } else {
            Toast.makeText(this, "Permissions required!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityJourneyMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val destination = intent.getStringExtra("to")

        Toast.makeText(this, "Destination: $destination", Toast.LENGTH_SHORT).show()

        prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)

        // MAP INIT
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // CHECK PERMISSIONS
        checkPermissions()
    }

    // ================= PERMISSION =================
    private fun checkPermissions() {
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (camera == PackageManager.PERMISSION_GRANTED &&
            location == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
            startLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ================= MAP =================
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }

    private fun startLocation() {
        val client = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        client.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val start = LatLng(it.latitude, it.longitude)
                val end = LatLng(it.latitude + 0.02, it.longitude + 0.02)

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 15f))
                map.addPolyline(PolylineOptions().add(start, end).width(8f))
            }
        }
    }

    // ================= CAMERA =================
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build()
            val selector = CameraSelector.DEFAULT_FRONT_CAMERA

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analyzer.setAnalyzer(cameraExecutor) {
                processImage(it)
            }

            preview.setSurfaceProvider(
                binding.cameraPreview.holder.surface.let { surface ->
                    Preview.SurfaceProvider { request ->
                        request.provideSurface(surface, cameraExecutor) { }
                    }
                }
            )
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, selector, preview, analyzer)

        }, ContextCompat.getMainExecutor(this))
    }

    // ================= ML KIT =================
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val image = InputImage.fromByteBuffer(
            imageProxy.planes[0].buffer,
            imageProxy.width,
            imageProxy.height,
            rotationDegrees,
            InputImage.IMAGE_FORMAT_NV21
        )

        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        detector.process(image)
            .addOnSuccessListener { detectSleep(it) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun detectSleep(faces: List<Face>) {
        for (face in faces) {
            val left = face.leftEyeOpenProbability ?: 1f
            val right = face.rightEyeOpenProbability ?: 1f
            val time = System.currentTimeMillis()

            if (left < 0.3 && right < 0.3) {
                if (!eyesClosed) {
                    eyesClosed = true
                    lastClosedTime = time
                } else if (time - lastClosedTime > sleepThresholdMs && !isAlertTriggered) {
                    isAlertTriggered = true

                    runOnUiThread {
                        binding.alertText.visibility = View.VISIBLE
                        playAlert()
                        saveAlert()
                    }
                }
            } else {
                eyesClosed = false
                isAlertTriggered = false
                runOnUiThread {
                    binding.alertText.visibility = View.GONE
                }
            }
        }
    }

    // ================= ALERT =================
    private fun playAlert() {
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)

        if (!mediaPlayer!!.isPlaying)
            mediaPlayer!!.start()
    }

    private fun saveAlert() {
        val user = prefs.getString("username", null) ?: return

        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val data = mapOf("time" to time, "date" to date)

        FirebaseDatabase.getInstance()
            .getReference("drivers")
            .child(user)
            .child("alerts")
            .push()
            .setValue(data)
    }

    // ================= LIFECYCLE =================
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
    }
}