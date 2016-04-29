package com.stazo.project_18;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;

public class EventInfoAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Firebase.setAndroidContext(this);
        // Set textviews to have correct info
        //grabEventInfo(((Project_18) getApplication()).getFB(),
        //        getIntent().getStringExtra("event_id"));

        //Event object to use for testing
        Event tester = new Event("Smash Tournament",
                "No Plebs Allowed.", "Dank Memes", 3, 2034);
        showInfo(tester);
    }

    // Pulls event info and delegates to showInfo to display the correct info
    private void grabEventInfo(Firebase fb, String event_id) {
        fb.child("Events").child(event_id).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // get the info, storage?
                        HashMap<String, Object> event = dataSnapshot.getValue(
                                new GenericTypeIndicator<HashMap<String, Object>>() {
                                });
                        Event e = new Event(
                                (String) event.get("name"),
                                (String) event.get("description"),
                                (String) event.get("creator_id"),
                                ((Integer) event.get("type")).intValue(),
                                ((Integer) event.get("time")).longValue());

                        // display event
                        showInfo(e);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });
    }

    // Called from grabEventInfo, programatically updates the textviews to display the correct info
    // Justin TODO Update the textviews in the layout to show the correct info
    private void showInfo(Event e) {
        //Initializing local variables to display relevant information
        TextView eventName = (TextView) findViewById(R.id.eventName);
        eventName.setText(e.getName());
        TextView eventDescription = (TextView) findViewById(R.id.eventDesc);
        eventDescription.setText(e.getDescription());
        TextView eventCreator = (TextView) findViewById(R.id.eventCreator);
        eventCreator.setText("Created by: " + e.getCreator_id());
        TextView eventTime = (TextView) findViewById(R.id.eventClock);
        //Converstion to turn a long (ex. 2014) into (8:14 PM)
        long hours = e.getTime()/100;
        long minutes = (e.getTime() - (hours*100));
        String timePeriod = "AM";
        if(hours > 12){
            timePeriod = "PM";
            hours = hours - 12;
        }
        eventTime.setText(hours + ":" + minutes + " " + timePeriod);
        //A bit of math to find the time till event.
        Calendar currTime = Calendar.getInstance();
        TextView eventTimeTo = (TextView) findViewById(R.id.eventTimeTo);
        if(timePeriod.equalsIgnoreCase("PM")){
            eventTimeTo.setText(((hours + 12) - currTime.HOUR) + " h " + (minutes - currTime.MINUTE) + " m");
        } else {
            eventTimeTo.setText((hours - currTime.HOUR) + " h " + (minutes - currTime.MINUTE) + " m");
        }
    }

}