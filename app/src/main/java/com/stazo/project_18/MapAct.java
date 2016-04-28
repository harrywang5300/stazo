package com.stazo.project_18;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;


import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

public class MapAct extends AppCompatActivity {

    private static final LatLng REVELLE = new LatLng(32.874447, -117.240914);

    private Firebase fb;
    private GoogleMap map;
    private MapHandler mapHandler;
    private boolean isPlacingMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }); */

        // Initialize Firebase
        Firebase.setAndroidContext(this);

        fb = ((Project_18) getApplication()).getFB();

        // Initialize the map
        MapFragment mapFrag =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        mapFrag.getMapAsync(new MapHandler());

    }

    // Display all the events, should probably be called in onCreate
    private void displayAllEvents() {

        // Listener for pulling the events
        fb.child("Events").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // For every event in fb.child("Events"), create event and displayEvent
                        for (DataSnapshot eventSnapshot : dataSnapshot.getChildren()) {

                            // get the info, storage?
                            HashMap<String, Object> event = eventSnapshot.getValue(
                                    new GenericTypeIndicator<HashMap<String, Object>>() {
                                    });
                            Event e = new Event(
                                    (String) event.get("name"),
                                    (String) event.get("description"),
                                    (String) event.get("creator_id"),
                                    ((Integer) event.get("type")).intValue(),
                                    ((Integer) event.get("time")).longValue());

                            // display event
                            displayEvent(e);
                        }

                        // remove this listener
                        fb.child("Events").removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });
    }

    // Ansel and Matt TODO Should add marker for event
    private void displayEvent(Event e) {
        isPlacingMarker = true;
        // Add marker for single event
        MarkerOptions marker = new MarkerOptions();

        marker.title(e.getName());
        marker.draggable(true);
    }

    public void onMapLongClick(LatLng point) {
        MarkerOptions marker = new MarkerOptions();

        marker.draggable(true);
        marker.position(point);

        map.addMarker(marker);
        map.moveCamera(CameraUpdateFactory.newLatLng(REVELLE));
    }

    private class MapHandler extends FragmentActivity
            implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener
    {
        public void onMapReady(GoogleMap googleMap) {
            // Initialize global variable
            map = googleMap;

            // Set OnMapLongClickListener to add markers
            map.setOnMapLongClickListener(this);

            // Set initial view to Revelle Plaza
            map.addMarker(new MarkerOptions().position(REVELLE).title("Revelle Plaza"));

            // Initial Camera Position
            float zoom = 17;
            float tilt = 0;
            float bearing = 0;

            CameraPosition camPos = new CameraPosition(REVELLE, zoom, tilt, bearing);

            map.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
        }

        // Add a marker where a long click occurs
        public void onMapLongClick(LatLng point) {
            // Add a new marker on the click location
            MarkerOptions marker = new MarkerOptions();

            marker.draggable(true);
            marker.position(point);

            map.addMarker(marker);
        }
    }

}
