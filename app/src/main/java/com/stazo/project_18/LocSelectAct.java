package com.stazo.project_18;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


/**
 * Created by Ansel on 4/28/16.
 */
public class LocSelectAct extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener,
        OnConnectionFailedListener
{
    private GoogleMap map;
    private PlaceAutocompleteFragment autocompleteFragment;
    private Event eventToInit;
    private Marker eventMarker;
    private GoogleApiClient mGoogleApiClient;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_loc_selector);

        // The intent that led to this activity
        Intent callingIntent = getIntent();

        // Get the event to initialize
        eventToInit = (Event) callingIntent.getParcelableExtra("eventToInit");
        System.out.println("Start time: " + eventToInit.getStartTime());
        System.out.println("End time: " + eventToInit.getEndTime());

        // Initialize the map_overview
        MapFragment mapFrag =
                (MapFragment) getFragmentManager().findFragmentById(R.id.mapLocSelector);

        mapFrag.getMapAsync(this);

        // init API client
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        //directions
        Toast dir = Toast.makeText(getApplicationContext(),
                "Tap and hold to choose the event's location", Toast.LENGTH_LONG);
        //this centers the text in the toast
        TextView v = (TextView) dir.getView().findViewById(android.R.id.message);
        if( v != null) v.setGravity(Gravity.CENTER);
        dir.show();

        //private method defined below
        setUpSearchFragment();
    }

    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapLongClickListener(this);

        // Initial Camera Position
        float zoom = 15;
        float tilt = 0;
        float bearing = 0;

        CameraPosition camPos = new CameraPosition(MapFrag.REVELLE, zoom, tilt, bearing);

        map.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }

    // Add a marker where a long click occurs
    public void onMapLongClick(LatLng point) {
        // Set the marker's location
        MarkerOptions markerOpts = new MarkerOptions();
        markerOpts.position(point);
        markerOpts.draggable(true);
        // Set the color of the marker
        markerOpts.icon(
                //BitmapDescriptorFactory.defaultMarker(Event.typeColors[eventToInit.getType()]));
                BitmapDescriptorFactory.fromResource(R.drawable.marker_light_blue_3x));
        // Remove the previous marker if there is one on the map
        if (eventMarker != null) {
            eventMarker.remove();
        }

        // Add marker to map
        eventMarker = map.addMarker(markerOpts);

        // Intitialize the event with the Lat/Lng of the event
        eventToInit.setLocation(point);

        // Initialize the event's id
        //eventToInit.setEvent_id(eventMarker.getId());
        //eventToInit.generateID();
    }

    public void goToMap(View view) {
        // Check if user placed a marker for event location
        if (eventToInit.getLocation() == null) {
            Context context = getApplicationContext();

            // Error message
            CharSequence text = "Please select a location";

            // How long to display the toast
            int duration = Toast.LENGTH_SHORT;

            // display the toast
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

        else {
//            System.out.println("Start: " + eventToInit.getStartDate().getTime());
//            System.out.println("End: " + eventToInit.getEndDate().getTime());
            System.out.println("startTimeToBePushed: " + eventToInit.getStartTime());
            System.out.println("endTimeToBePushed: " + eventToInit.getEndTime());

            // Push the event to the database
            eventToInit.pushToFirebase(((Project_18) getApplication()).getFB());

            // Go to the map screen
            Intent intent = new Intent(this, MainAct.class);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Firebase.setAndroidContext(this);
    }

    private void setUpSearchFragment(){
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.

                LatLng point = place.getLatLng();
                // Set the marker's location
                MarkerOptions markerOpts = new MarkerOptions();
                markerOpts.position(point);
                markerOpts.draggable(true);
                // Set the color of the marker
                markerOpts.icon(
                        //BitmapDescriptorFactory.defaultMarker(Event.typeColors[eventToInit.getType()]));
                        BitmapDescriptorFactory.fromResource(R.drawable.marker_light_blue_3x));
                // Remove the previous marker if there is one on the map
                if (eventMarker != null) {
                    eventMarker.remove();
                }

                // Add marker to map
                eventMarker = map.addMarker(markerOpts);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                //Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //CONNECTION FAILED??
        //OH NO
    }
}