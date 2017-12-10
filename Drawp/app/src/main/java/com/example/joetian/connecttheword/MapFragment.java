package com.example.joetian.connecttheword;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.Activity;
import android.widget.*;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.*;
import android.location.LocationManager;
import com.google.android.gms.maps.model.*;
import android.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import android.support.annotation.NonNull;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener,
                                                        GoogleMap.OnPoiClickListener {

    private FirebaseAuth firebaseAuth;

    private static final long MIN_TIME = 300;   //time between update query in ms
    private static final float MIN_DISTANCE = 700;  //min distance travel allowed before update query
    private static final long  MIN_RADIUS = 100; //min distance allowed to interact with other objects

    private MapView mapView; //MapView instance that is displayed in UI
    private GoogleMap mMap; //GoogleMap instance that the view interacts with
    private LocationManager locationManager; //Location Manager instance for tracking user position
    private LocationListener listener; //Location listener for verifying position updates
    private Circle interactionBound;  //radius bound on POI interaction
    private Marker currPosMarker;       //marker for current user position

    private Bitmap posMarkerBitmap;     //imageview that holds the image for the currPosMarker

    private int parentFrameHolder; //int id of the frame layout the fragment transactions occur in
    private TextView mapTitle; //title bar text
    private ImageButton logoutBttn;
    private ImageButton profileBttn;
    private String uID;

    private double lastLat = 0;
    private double lastLong = 0;

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
        uID = firebaseAuth.getCurrentUser().getDisplayName();
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

        //Set up User Profile functionality
        profileBttn = (ImageButton) v.findViewById(R.id.profile);
        profileBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view_profile();
            }
        });

        //Set up location manager and listener functionality
        locationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                MIN_TIME, MIN_DISTANCE, this);

        //Get A Default marker and initialize the map
        Bitmap rawIcon = BitmapFactory.decodeResource(getResources(), R.drawable.user_icon);
        posMarkerBitmap = Bitmap.createScaledBitmap(rawIcon, 50, 50, true);
        //Initialize the map before performing camera updates
        MapsInitializer.initialize(getActivity());
    }

    /**
     * If the user has a custom marker (profile picture) then we download it in a byte array and conver it into
     * a bitmap for custom marker use.
     */
    public void initializeMarker() {
        //Load user image
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child("users").child(uID).child("profile_url").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    StorageReference markerRef = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(dataSnapshot.getValue(String.class));

                    //Set a File Cap for Markers to 3MB
                    final long THREE_MEGABYTE = 3 * 1024 * 1024;
                    markerRef.getBytes(THREE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            //Set the Custome marker icon
                            Bitmap rawIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            posMarkerBitmap = Bitmap.createScaledBitmap(rawIcon, 50, 50, true);
                            currPosMarker.setIcon(BitmapDescriptorFactory.fromBitmap(posMarkerBitmap));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //Print a DL failure
                            Toast.makeText(getActivity(), "Custom Marker Failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DB Request", databaseError.getMessage());
            }
        });
    }

    /**
     * Handles a POI click as follows:
     * If the POI is in circle indicator range of the user, generate a menu dialog to make a drawp or view drawps
     * If the POI is out of range, generate a toast error dialog and do nothing
     * @param poi - PointOfInterest instance that was clicked by the user
     */
    @Override
    public void onPoiClick(final PointOfInterest poi) {
        //Case Where the POI is out of range of the user's indicator
        if(getDistance(new LatLng(lastLat, lastLong), poi.latLng) > MIN_RADIUS) {
            Toast.makeText(getContext(), "Out of Range!", Toast.LENGTH_SHORT).show();
            return;
        }

        //POI is in range, handle accordingly
        CharSequence[] options = new CharSequence[] {"Leave a Drawp", "View Current Drawps"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(poi.name);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    DrawingFragment dfrag = DrawingFragment.newInstance(parentFrameHolder, poi.placeId,
                            firebaseAuth.getCurrentUser().getDisplayName(), poi.name);
                    ft.hide(MapFragment.this);
                    ft.add(parentFrameHolder, dfrag);
                    ft.addToBackStack(null);
                    ft.commit();
                }
                else {
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    BrowsingFragment bfrag = BrowsingFragment.newInstance(parentFrameHolder, poi.placeId, poi.name);
                    ft.hide(MapFragment.this);
                    ft.add(parentFrameHolder, bfrag);
                    ft.addToBackStack(null);
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
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        AuthFragment afrag = AuthFragment.newInstance(parentFrameHolder);
        ft.remove(MapFragment.this);
        ft.add(parentFrameHolder, afrag);
        ft.commit();
    }

    /**
     * Take a user to the profile view
     */
    private void view_profile() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ProfileFragment pfrag = ProfileFragment.newInstance(parentFrameHolder);
        ft.hide(MapFragment.this);
        ft.add(parentFrameHolder, pfrag);
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Get the euclidean distance between the user's current latlng location and a point of interest's latlng location
     * @param userLoc LatLng object denoting the user's last seen location
     * @param poiLoc LatLng object denoting the pointofinterest's location
     */
    private long getDistance(LatLng userLoc, LatLng poiLoc) {
        float[] results = new float[1];
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, poiLoc.latitude, poiLoc.longitude, results);
        return (long)results[0];
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
        interactionBound.setCenter(new LatLng(lastLat, lastLong));
        currPosMarker.setPosition(new LatLng(lastLat, lastLong));
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
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(false);
            mMap.setIndoorEnabled(false);
            mMap.setOnPoiClickListener(this);
            interactionBound = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lastLat, lastLong))
                .radius(MIN_RADIUS)
                .strokeColor(Color.RED)
                .fillColor(ContextCompat.getColor(getContext(), R.color.tseagreen))
            );
            currPosMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lastLat, lastLong))
                .icon(BitmapDescriptorFactory.fromBitmap(posMarkerBitmap))
            );
            initializeMarker();
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
