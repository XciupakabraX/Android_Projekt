package com.example.projekt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private Marker userMarker;
    private List<Marker> markers = new ArrayList<>();
    private boolean isCameraInitialized = false;
    private Location previousLocation = null;
    private Location lastSearchLocation = null; // Lokalizacja ostatniego wyszukiwania

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Sprawdzenie uprawnień
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }

        // Inicjalizacja mapy
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Nie można załadować mapy", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }
        Log.d("MAP_READY", "Mapa została załadowana i jest gotowa.");
    }

    private boolean checkLocationPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d("PERMISSION_CHECK", "Czy uprawnienia lokalizacji są przyznane: " + hasPermission);
        return hasPermission;
    }

    private void requestLocationPermission() {
        Log.d("PERMISSION_REQUEST", "Żądanie uprawnień do lokalizacji.");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(5000);  // Aktualizacja co 5 sekund
                locationRequest.setFastestInterval(2000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            Log.d("LOCATION_UPDATE", "Otrzymano lokalizację: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                            updateMapWithLocation(location);
                        }
                    }
                };

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                Log.d("LOCATION_UPDATES", "Rozpoczęto nasłuchiwanie aktualizacji lokalizacji.");
            } catch (SecurityException e) {
                Toast.makeText(this, "Brak uprawnień do uzyskania lokalizacji.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateMapWithLocation(Location location) {
        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d("LOCATION_CHANGE", "Aktualizacja mapy dla lokalizacji: " + myLocation);

        if (previousLocation == null || location.distanceTo(previousLocation) > 5) { // Odległość większa niż 5 metrów
            // Usuń wszystkie poprzednie znaczniki
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 18));
            previousLocation = location;

            // Sprawdź, czy odległość od ostatniego wyszukiwania wynosi co najmniej 1 km
            if (lastSearchLocation == null || location.distanceTo(lastSearchLocation) >= 1000) {
                lastSearchLocation = location;
                searchBiedronkaStores(myLocation);
            }
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d("PERMISSION_RESULT", "Uprawnienia przyznane: " + permissionGranted);
            if (permissionGranted) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchBiedronkaStores(LatLng location) {
        String apiKey = "AIzaSyDNzsjFrY6CSpHJKSvKA1uRjtyKEdSKPSU";
        String url = String.format(
                Locale.US, // Wymuszenie użycia kropki jako separatora dziesiętnego
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=10000&type=store&keyword=Biedronka&key=%s",
                location.latitude, location.longitude, apiKey
        );
        Log.d("API_CALL", "Wysyłanie zapytania do API: " + url);

        new Thread(() -> {
            try {
                String response = HttpHandler.makeServiceCall(url);
                if (response != null) {
                    Log.d("API_RESPONSE", "Odpowiedź z API: " + response);
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray results = jsonObject.getJSONArray("results");

                    if (results.length() > 0) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");

                            String name = place.getString("name");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");

                            runOnUiThread(() -> {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lng))
                                        .title(name));
                                if (marker != null) {
                                    markers.add(marker);
                                }
                            });
                        }
                    } else {
                        Log.d("API_RESULT", "Brak wyników w pobliżu.");
                        runOnUiThread(() -> {
                            Toast.makeText(MapsActivity.this, "Brak sklepów Biedronka w pobliżu.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("API_ERROR", "Błąd podczas wysyłania żądania: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MapsActivity.this, "Błąd podczas wyszukiwania sklepów", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
