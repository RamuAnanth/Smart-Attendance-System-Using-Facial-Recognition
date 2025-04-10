package com.example.smartattendance;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.example.smartattendance.Attendance.AttendanceActivity;
import com.example.smartattendance.LearnMore.LearnMoreActivity;
import com.example.smartattendance.Profile.ProfileActivity;
import com.example.smartattendance.SignIn.SignInActivity;
import com.example.smartattendance.aboutus.AboutUsActivity;
import com.example.smartattendance.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationMatched = false;
    private Location lastKnownLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
//This is used for the multi location access purpose
//    private final double[][] allowedLocations = {
//            {15.890116924536413, 80.43730276688215},
//            {28.704060, 77.102493},
//            {19.076090, 72.877426}
//    };
    double targetLat = 15.8890248;
    double targetLng = 80.4406932;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();

        binding.menuOption.setOnClickListener(v -> {
            alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to Log Out")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        mAuth.signOut();
                        startActivity(new Intent(MainActivity.this, SignInActivity.class));
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        binding.attendanceList.setOnClickListener(v -> {
            if (lastKnownLocation != null && isLocationMatched) {
                Intent intent = new Intent(MainActivity.this, AttendanceActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "You are not in the correct location.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.learnMore.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LearnMoreActivity.class);
            startActivity(intent);
        });

        binding.profile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        binding.aboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutUsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isLocationEnabled()) {
            showLocationEnableDialog();
        } else {
            requestLocationUpdates();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationEnableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Location is required for attendance. Please turn it on.")
                .setCancelable(false)
                .setPositiveButton("Enable", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Location is required for attendance", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lastKnownLocation = location;
                isLocationMatched = checkLocationMatch(location);
            }
        });

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    lastKnownLocation = location;
                    isLocationMatched = checkLocationMatch(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
// In this if the location not matched then the it will dispaly the how much distance to the orginal location to access the attendence menu card.
    
    private boolean checkLocationMatch(Location location) {
        double userLat = location.getLatitude();
        double userLng = location.getLongitude();

        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, targetLat, targetLng, results);
        float distanceInMeters = results[0];

        if (distanceInMeters <= 1.0f) {
            Toast.makeText(this, "Location matched within 1 meter!", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(this, "You are " + distanceInMeters + " meters away from target.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
//checking to provide permission for the accessing the attendence menu with in the 20 meters from the location
    private boolean isWithinRange(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] <= 20;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
