package com.stazo.project_18;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebase.client.Firebase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Author: Brian Chan
 * Date: 4/25/2016
 * Description: This class parses the user input into an Event object to create a new event for
 * stazo-project-18.
 */
public class CreateEventAct extends AppCompatActivity {
    private Event event = new Event(); //Create a new event object
    private List<String> typeList; //Array of the types of events
    int normColor = Color.BLACK; //Color of default text
    int errorColor = Color.RED; //Color of error text
    private Toolbar toolbar; // toolbar

    //The Text labeling the EditTexts
    TextView nameText, descText, pickText, startDateText, endDateText, startText, endText;
    //The EditText views for each input
    EditText nameView, descView, startDateView, endDateView, startTimeView, endTimeView;
    // The Spinner to pick what type the event is
    Spinner typeSpinner;
    //Fragments for setting the dates
    DatePickerFragment startDateFrag; //endDateFrag;
    //Fragments for setting the times
    TimePickerFragment startTimeFrag, endTimeFrag;
    //Parsed user inputted values, upload these to Firebase
    String name, desc, startDate, endDate, startTime, endTime; //, type;
    // To hold start date/time and end date/time
    GregorianCalendar startCal, endCal;
    private long startTimeLong, endTimeLong;
    //int typeNum = -1;
    int typeNum = 0;

    private Uri imageUri;

    private String cameraPhotoPath;

    /**
     * Called whenever this layout is created. This should set up the layout so that it is ready
     * to receive user input and to create a new event.
     * @param savedInstanceState Reference to a Bundle object that this activity can use to restore
     *                           itself in the future if needed.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_event);

        // comment out later, needed for testing
        Firebase.setAndroidContext(this);

        //setToolbar();

        setUpTextColors();

        grabEditTextViews();

        //Sets up the Spinner for selecting an Event Type
        typeSpinner = (Spinner) findViewById(R.id.EventType);

        //Add values here to populate the spinner
        typeList = new ArrayList<>();

        startDateFrag = new DatePickerFragment(startDateView);
        //Set up today
        final Calendar c = Calendar.getInstance();
        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH);
        final int day = c.get(Calendar.DAY_OF_MONTH);
        startDateFrag.setYear(year);
        startDateFrag.setMonth(month);
        startDateFrag.setDay(day);

        // TODO: ADD ERROR CHECKING FOR WRAP AROUND DATES
        for(int i = 0; i < 7; i++) {
            String monthStr = "" + (month + 1);
            String dayStr = "" + (day + i);

            if (month + 1 < 10) {
                monthStr = "0" + (month + 1);
            }
            if (day + i < 10) {
                dayStr = "0" + (day + i);
            }
            String date = monthStr+"/"+dayStr;

            if ( i == 0 ) {
                typeList.add( "Today (" + date +")");
            }
            else {
                typeList.add(new SimpleDateFormat("EEEE").format(c.getTime()) + " (" + date+")");
            }
            c.add(Calendar.DATE, 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, typeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        //Actions for spinner selection
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                startDateFrag.setDay(day + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        startTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimeFrag = new TimePickerFragment(startTimeView);
                startTimeFrag.show(getSupportFragmentManager(), "timePicker");

                startTimeView.setError(null);
            }
        });

        endTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTimeFrag = new TimePickerFragment(endTimeView);
                endTimeFrag.show(getSupportFragmentManager(), "timePicker");

                endTimeView.setError(null);
            }
        });

    }

    /**
     * Changes all of the TextViews to the default color black.
     */
    private void setUpTextColors() {
        nameText = (TextView) findViewById(R.id.NameText);
        descText = (TextView) findViewById(R.id.DescText);
        pickText = (TextView) findViewById(R.id.PickText);
        //startDateText = (TextView) findViewById(R.id.StartDateText);
        //endDateText = (TextView) findViewById(R.id.EndDateText);
        startText = (TextView) findViewById(R.id.StartText);
        endText = (TextView) findViewById(R.id.EndText);

        nameText.setTextColor(normColor);
        descText.setTextColor(normColor);
        pickText.setTextColor(normColor);
        //startDateText.setTextColor(normColor);
        //endDateText.setTextColor(normColor);
        startText.setTextColor(normColor);
        endText.setTextColor(normColor);
    }

    /**
     * Grabs all of the EditText Views so they can be referenced in the future.
     */
    private void grabEditTextViews() {
        nameView = (EditText) findViewById(R.id.EventName);
        descView = (EditText) findViewById(R.id.EventDesc);
        //startDateView = (EditText) findViewById(R.id.StartDate);
        //endDateView = (EditText) findViewById(R.id.EndDate);
        startTimeView = (EditText) findViewById(R.id.StartTime);
        endTimeView = (EditText) findViewById(R.id.EndTime);
    }

    /**
     * Parses the user input into strings.
     */
    private void setUpInput() {
        //Grabs user input
        name = nameView.getText().toString();
        desc = descView.getText().toString();
        //type = typeSpinner.getSelectedItem().toString();

        // Get the index of the type in the Event.types array
        /*for(int i = 0; i < Event.types.length; i++) {
            if(type.equals(Event.types[i])) {
                typeNum = i;
            }
        }*/

        //TODO edit startdate
        //startDate = startDateView.getText().toString();
        //endDate = endDateView.getText().toString();
        startTime = startTimeView.getText().toString();
        endTime = endTimeView.getText().toString();

    }

    /**
     * Checks if the user entered valid input. setUpInput MUST be called before checkInput is called
     * @return True/False depending on if we have valid input
     */
    private boolean checkInput() {
        boolean valid = true;
        String blankView = "This field cannot be left blank";
        String dateAfter = "The end date has to be after the start date";
        String timeAfter = "The end time has to be after the start time";

        //Checks that these fields are not left empty
        if (name.isEmpty()) {
            nameView.setError(blankView);
            valid = false;
        }

        /*if (desc.isEmpty()) {
            descView.setError(blankView);
            valid = false;
        }*/

        if (typeNum == -1) {
            //pickText.setTextColor(errorColor);
            valid = false;
        }

        //Checks if the user inputted a date at all
        /*if (startDateView.getText().toString().matches("")) {
            startDateView.setError(blankView);
            valid = false;
        }
        else {
            startDateView.setError(null);
        }

        if (endDateView.getText().toString().matches("")) {
            endDateView.setError(blankView);
            valid = false;
        }
        else {
            endDateView.setError(null);
        } */

        //Checks if the user entered a time at all
        if (startTimeView.getText().toString().matches("")) {
            startTimeView.setError(blankView);
            valid = false;
        }
        else {
            startTimeView.setError(null);
        }

        if (endTimeView.getText().toString().matches("")) {
            endTimeView.setError(blankView);
            valid = false;
        }
        else {
            endTimeView.setError(null);
        }


        startCal = new GregorianCalendar(startDateFrag.getYear(),
                startDateFrag.getMonth(),
                startDateFrag.getDay(),
                startTimeFrag.getHourInt(),
                startTimeFrag.getMinInt());
        endCal = new GregorianCalendar(startDateFrag.getYear(),
                startDateFrag.getMonth(),
                startDateFrag.getDay(),
                endTimeFrag.getHourInt(),
                endTimeFrag.getMinInt());
        startTimeLong = startCal.getTimeInMillis();

        startTimeLong = startCal.getTimeInMillis();
        endTimeLong = endCal.getTimeInMillis();

        // If endTime < startTime, the user meant the next day, so we add a day
        if (endTimeLong < startTimeLong) {
            endTimeLong += 1000 * 60 * 60 * 24;
        }
        System.out.println("startTime: " + startTimeLong);
        System.out.println("endTime: " + endTimeLong);


        //Return validity of user input
        return valid;
    }

    /**
     * Called when the Create Event button is pressed.
     * @param view The view we are currently in
     */
    public void makeEvent(View view) {
        //Sets up user input into respective strings
        setUpInput();

        if (checkInput()) {
            event = new Event(name, desc, ((Converg) getApplication()).getMe().getID(), typeNum,
                    startTimeLong, endTimeLong, "TEST");
//            event.setTimes(startCal, endCal);
            System.out.println("TestIN: " + event.getTest());
            System.out.println("nameIN: " + event.getName());
            goToLocSelectAct();
        }
    }

    /**
     * Navigate to the map activity.
     */
    private void goToLocSelectAct() {
        Intent intent = new Intent(this, LocSelectAct.class);
        intent.putExtra("eventToInit", event);
        if (imageUri != null) {
            intent.putExtra("mainImageUri", imageUri.toString());
        }
        startActivity(intent);
    }

    public void goToProfile() {
        Intent i =  new Intent(this, MainAct.class);
        i.putExtra("toProfile", true);
        startActivity(i);
        finish();
    }


    // Camera stuff

    private void buttonChooser() {
        final CharSequence[] items = { "Take Photo", "Select from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(
                CreateEventAct.this);

        builder.setTitle("Choose option:");
        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    dialog.dismiss();
                    takePhoto();
                } else if (items[item].equals("Select from Library")) {
                    dialog.dismiss();
                    selectPhoto();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                    System.out.println("cancelled");
                }
            }
        });
        builder.show();
    }

    private void selectPhoto() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2); //2 is the callback number code for this specific call #magicnumbersftw #fuckstyle
        }

        //if no permissions, then ask
        else {
            System.out.println("no photo gallery permissions! :(");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);

        }
    }

    private void takePhoto() {
        //just in case the phone doesn't have a camera
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            //check if capable of adding an image
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                //check if camera permissions are granted
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {

                    //preemptively set file location
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        System.out.println("Could not create file for camera photo");
                    }

                    // pass in file location and open camera
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.stazo.project_18.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, 1);
                    }
                }

                //if no permissions, then ask
                else {
                    System.out.println("no camera permissions! :(");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA}, 1);

                }
            }
            else {
                System.out.println("your phone can't use camera for some reason");
            }
        }
        else {
            //tell user u can't or smth
            System.out.println("no camera on ur cheapo phone lmao! :(");
        }
    }

    private File createImageFile() throws IOException {
        //just say the file name is timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "event_photo_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        cameraPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //right after the user decies whether or not to give app camera permissions
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            }
            else {
                System.out.println("You didn't give us permission to use ur shitty camera :(");
            }
        }

        //right after the user decides whether or not to give app photo gallery permissions
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to access photo gallery
                selectPhoto();
            }
            else {
                System.out.println("You didn't give us permission to view ur shitty photos :(");
            }
        }

    }

    // On intent return with bitmap in data
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            //also save the photo to gallery
//            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            File f = new File(cameraPhotoPath);
//            Uri cameraPhotoUri = Uri.fromFile(f);
//            mediaScanIntent.setData(cameraPhotoUri);
//            this.sendBroadcast(mediaScanIntent);

            //display
            try {
//                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), cameraPhotoUri);

                //rotate bitmap and save it back
                Matrix matrix = new Matrix();
                matrix.postRotate(getImageOrientation(cameraPhotoPath));

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                options.inSampleSize = 2;
                Bitmap imageBitmap = BitmapFactory.decodeFile(cameraPhotoPath, options);
                Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

                File file = new File(cameraPhotoPath); // the File to save to
                OutputStream fOut = new FileOutputStream(file);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
                fOut.close();

                ImageView mainImageView = (ImageView) this.findViewById(R.id.MainImageView);
                mainImageView.setImageBitmap(rotatedBitmap);

                //push to firebase
                pushMainImage(Uri.fromFile((new File(cameraPhotoPath))));
            }
            catch (Exception e) {
                System.out.println("Something went wrong when accessing photo from file path or pushing photo");
            }
        }

        if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            //to convert to bitmap
            try {

                Matrix matrix = new Matrix();
                matrix.postRotate(getImageOrientation2(imageUri));
                Bitmap imageBitmap = BitmapFactory.decodeFile(getRealPathFromURI(getBaseContext(), imageUri));
                Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

                File file = new File(getRealPathFromURI(getBaseContext(), imageUri)); // the File to save to
                OutputStream fOut = new FileOutputStream(file);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
                fOut.close();

                Bitmap selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ImageView mainImageView = (ImageView) this.findViewById(R.id.MainImageView);
                mainImageView.setImageBitmap(selectedImage);

                //push to firebase
                pushMainImage(imageUri);
            }
            catch(Exception e) {
                System.out.println("Something went wrong when u selected an image or pushing photo");
            }
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int getImageOrientation(String imagePath){
        int rotate = 0;
        try {

            File imageFile = new File(imagePath);
            System.out.println("getting exif");
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            System.out.println("rotation: " + rotate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public int getImageOrientation2(Uri photoUri) {
        int photoRotation = 0;
        boolean hasRotation = false;
        String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
        try {
            Cursor cursor = getContentResolver().query(photoUri, projection, null, null, null);
            if (cursor.moveToFirst()) {
                photoRotation = cursor.getInt(0);
                hasRotation = true;
            }
            cursor.close();
        } catch (Exception e) {}

        if (!hasRotation) {
            try {
                ExifInterface exif = new ExifInterface(photoUri.getPath());
                int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                switch (exifRotation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: {
                        photoRotation = 90;
                        break;
                    }
                    case ExifInterface.ORIENTATION_ROTATE_180: {
                        photoRotation = 180;
                        break;
                    }
                    case ExifInterface.ORIENTATION_ROTATE_270: {
                        photoRotation = 270;
                        break;
                    }
                }
            } catch(Exception e) {e.printStackTrace();};
        }
        System.out.println("photo rotation: " + photoRotation);
        return photoRotation;
    }

    public void pushMainImage(Uri imageFile) {
        //just for passing to next intent
        imageUri = imageFile;
    }


    public void cancel(View view) {
        onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        Firebase.setAndroidContext(this);
    }
}