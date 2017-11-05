package com.example.joetian.connecttheword;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.FragmentTransaction;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.UUID;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    protected static final String uID = UUID.randomUUID().toString();

    private static final String[] init_perms = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.MAPS_RECEIVE};
    private static final int perm_request_code = 420;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Handle Permissions
        if(!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                !hasPermission(Manifest.permission.MAPS_RECEIVE)) {
            ActivityCompat.requestPermissions(this, init_perms, perm_request_code);
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        MapFragment mfrag = MapFragment.newInstance(R.id.main_frame_holder);
        ft.replace(R.id.main_frame_holder, mfrag);
        ft.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case perm_request_code:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    MapFragment mfrag = MapFragment.newInstance(R.id.main_frame_holder);
                    ft.replace(R.id.main_frame_holder, mfrag);
                    ft.commit();
                }
        }
    }

    private boolean hasPermission(String perm) {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
