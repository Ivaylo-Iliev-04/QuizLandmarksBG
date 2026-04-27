package com.example.quizlandmarksbg;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity {
    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1002;
    FusedLocationProviderClient fusedLocationClient;
    EditText etCity;
    Button btnStartQuiz;
    String detectedCity = "";
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        String username = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        etCity = findViewById(R.id.etCity);
        btnStartQuiz = findViewById(R.id.btnStartQuiz);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnStartQuiz.setOnClickListener(v -> {
            if (detectedCity.isEmpty()) {
                Toast.makeText(this, "Няма открит град!", Toast.LENGTH_SHORT).show();
                return;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            calendar.setMinimalDaysInFirstWeek(4);
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            int year = calendar.getWeekYear();
            String currentWeek = year + "_" + week;
            db.collection("quizzes")
                    .document(detectedCity)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            Toast.makeText(this, "Няма анкета за този град!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        db.collection("leaderboards")
                                .document(detectedCity)
                                .collection("results")
                                .whereEqualTo("username", username)
                                .whereEqualTo("week", currentWeek)
                                .get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        Toast.makeText(this, "Вече сте играли тази седмица!", Toast.LENGTH_LONG).show();
                                    } else {
                                        Intent intent = new Intent(this, QuizActivity.class);
                                        intent.putExtra("city", detectedCity);
                                        intent.putExtra("username", username);
                                        startActivity(intent);
                                    }
                                });
                    });
        });
        etCity.setText("Зареждане...");
        checkLocationSettings();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            getLocation();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(LocationActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    //sendEx.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getLocation();
            } else {
                Toast.makeText(this, "Трябва да включите местоположението, за да продължите", Toast.LENGTH_LONG).show();
                etCity.setText("Местоположението е изключено");
            }
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        if (locationResult.getLastLocation() == null) {
                            etCity.setText("Неуспешно откриване");
                            return;
                        }
                        double lat = locationResult.getLastLocation().getLatitude();
                        double lng = locationResult.getLastLocation().getLongitude();
                        getCityFromLocation(lat, lng);
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                },
                getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                Toast.makeText(this, "Няма разрешение за местоположение!", Toast.LENGTH_SHORT).show();
                etCity.setText("Няма разрешение!");
            }
        }
    }

    private void getCityFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, new Locale("bg"));
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                if (address.getLocality() != null) {
                    detectedCity = address.getLocality();
                } else if (address.getSubAdminArea() != null) {
                    detectedCity = address.getSubAdminArea();
                } else {
                    detectedCity = address.getAdminArea();
                }
                etCity.setText(detectedCity);
            } else {
                etCity.setText("Неизвестен град!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
