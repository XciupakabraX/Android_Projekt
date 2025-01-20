package com.example.projekt;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

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

        // Ustawienie kamery na Białystok
        LatLng bialystok = new LatLng(53.1325, 23.1688);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bialystok, 13));

        // Szukaj sklepów Biedronka
        searchBiedronkaStores(bialystok);
    }

    private void searchBiedronkaStores(LatLng location) {
        String apiKey = "AIzaSyDNzsjFrY6CSpHJKSvKA1uRjtyKEdSKPSU";
        String url = String.format(
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=10000&type=store&keyword=Biedronka&key=%s",
                location.latitude, location.longitude, apiKey
        );

        new Thread(() -> {
            try {
                String response = HttpHandler.makeServiceCall(url);
                if (response != null) {
                    // Logowanie odpowiedzi
                    Log.d("GooglePlacesAPI", response);

                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray results = jsonObject.getJSONArray("results");

                    if (results.length() > 0) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");

                            String name = place.getString("name");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");

                            // Logowanie nazwy sklepu i współrzędnych
                            Log.d("PlaceInfo", "Name: " + name + ", Lat: " + lat + ", Lng: " + lng);

                            // Dodanie markera na mapie
                            runOnUiThread(() -> {
                                mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lng))
                                        .title(name));
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MapsActivity.this, "Brak sklepów Biedronka w pobliżu.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.d("GooglePlacesAPI", "Brak odpowiedzi z API");
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MapsActivity.this, "Błąd podczas wyszukiwania sklepów", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


}
