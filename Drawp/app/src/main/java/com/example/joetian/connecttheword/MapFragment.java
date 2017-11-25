package com.example.joetian.connecttheword;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.location.LocationListener;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.*;
import android.location.LocationManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import android.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener,
                                                        GoogleMap.OnPoiClickListener {

    private FirebaseAuth firebaseAuth;

    private static final long MIN_TIME = 300;
    private static final float MIN_DISTANCE = 700;

    private MapView mapView; //MapView instance that is displayed in UI
    private GoogleMap mMap; //GoogleMap instance that the view interacts with
    private LocationManager locationManager; //Location Manager instance for tracking user position
    private LocationListener listener; //Location listener for verifying position updates

    private int parentFrameHolder; //int id of the frame layout the fragment transactions occur in
    private TextView mapTitle; //title bar text
    private ImageButton logoutBttn;

    private double lastLat = 0;
    private double lastLong = 0;
    private int lastZoom = 20;
    /**
     * Set up new instance of map fragment with parent frame holder
     * @param parent int representing id of the frame layout this fragment is being put into
     * @return new instance of mapfragment with arguments available
     */
    public static MapFragment newInstance(int parent) {
        MapFragment mfrag = new MapFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        mfrag.setArguments(args);
        return mfrag;
    }

    /**
     * Extract arguments from Bundle and do other oncreate routines
     * @param savedInstanceState Bundle instance containing savedinstance state content
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Map:" ,"Entered Map Fragment");
        parentFrameHolder = getArguments().getInt("parent");
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_fragment, parent, false);
        if(savedInstanceState != null) {
            lastLat = savedInstanceState.getDouble("lat");
            lastLong = savedInstanceState.getDouble("long");
        }
        mapView = (MapView)v.findViewById(R.id.user_map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        //Set title bar
        mapTitle = (TextView) v.findViewById(R.id.map_header);
        mapTitle.setText(getString(R.string.map_title, firebaseAuth.getCurrentUser().getDisplayName()));

        //Set up logout functionality
        logoutBttn = (ImageButton) v.findViewById(R.id.logout);
        logoutBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        //Set up location manager and listener functionality
        locationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                MIN_TIME, MIN_DISTANCE, this);

        //Initialize the map before performing camera updates
        MapsInitializer.initialize(this.getActivity());
    }

    @Override
    public void onPoiClick(final PointOfInterest poi) {
        CharSequence[] options = new CharSequence[] {"Leave a Drawp", "View Current Drawps"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(poi.name);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    DrawingFragment dfrag = DrawingFragment.newInstance(parentFrameHolder, poi.placeId,
                            firebaseAuth.getCurrentUser().getDisplayName(), poi.name);
                    ft.hide(MapFragment.this);
                    ft.replace(parentFrameHolder, dfrag);
                    ft.commit();
                }
                else {
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    BrowsingFragment bfrag = BrowsingFragment.newInstance(parentFrameHolder, poi.placeId);
                    ft.hide(MapFragment.this);
                    ft.replace(parentFrameHolder, bfrag);
                    ft.commit();
                }
            }
        });
        builder.show();
    }

    /**
     * Log a user out of the application. Returns to the authentication fragment
     */
    private void logout() {
        firebaseAuth.signOut();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        AuthFragment afrag = AuthFragment.newInstance(parentFrameHolder);
        ft.remove(MapFragment.this);
        ft.replace(parentFrameHolder, afrag);
        ft.commit();
    }

    /**
     * Implementation of Location Listener Abstract Methods
     */

    /**
     * Handle location changes by the user. Updates camera to user position with set zoom
     * @param loc Location instance to extract lattitude and longitude from
     */
    @Override
    public void onLocationChanged(Location loc) {
        LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, 20);
        lastLat = loc.getLatitude();
        lastLong = loc.getLongitude();
        mMap.animateCamera(update);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String s) { }

    @Override
    public void onProviderDisabled(String s) { }

    /**
     * Implementation of On Map ReadyCallback abstract methods
     */

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putDouble("lat", lastLat);
        savedInstanceState.putDouble("long", lastLong);
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
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLat, lastLong), 20));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setMyLocationEnabled(true);
            mMap.setOnPoiClickListener(this);
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
