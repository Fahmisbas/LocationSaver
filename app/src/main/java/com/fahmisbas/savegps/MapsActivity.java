package com.fahmisbas.savegps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mapFragment();
    }

    private void mapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        Intent intent = getIntent();
        if (intent.getIntExtra("position", 0) == 0) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    centerOnMap(location, address);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            setMapPermission();
        } else {
            Location selectedLocatio = new Location(LocationManager.GPS_PROVIDER);
            selectedLocatio.setLatitude(MainActivity.location.get(intent.getIntExtra("position", 0)).latitude);
            selectedLocatio.setLongitude(MainActivity.location.get(intent.getIntExtra("position", 0)).longitude);
            centerOnMap(selectedLocatio, address);
        }
    }

    private void setMapPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        address = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> locationList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (locationList.size() > 0) {
                if (locationList.get(0).getSubThoroughfare() != null) {
                    address += locationList.get(0).getSubThoroughfare() + " ";
                }
                address += locationList.get(0).getThoroughfare();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (address.equals("")) {
            String pattern = "dd-M-yyyy hh:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            address += sdf.format(new Date());
        }

        Toast.makeText(this, "Location is saved!", Toast.LENGTH_SHORT).show();
        mMap.addMarker(new MarkerOptions().position(latLng).title(address));
        MainActivity.places.add(address);
        MainActivity.location.add(latLng);
        MainActivity.adapter.notifyDataSetChanged();


        for (LatLng coord : MainActivity.location) {
            MainActivity.latitude.add(String.valueOf(coord.latitude));
            MainActivity.longitude.add(String.valueOf(coord.longitude));
        }
        SharedPreferences sharedPreferences = this.getSharedPreferences("com.fahmisbas.savegps", MODE_PRIVATE);
        try {
            sharedPreferences.edit().putString("places", ObjectSerializer.serialize(MainActivity.places)).apply();
            sharedPreferences.edit().putString("lats", ObjectSerializer.serialize(MainActivity.latitude)).apply();
            sharedPreferences.edit().putString("lons", ObjectSerializer.serialize(MainActivity.longitude)).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerOnMap(Location location, String address) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(userLocation).title(address));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }
}
