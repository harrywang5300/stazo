package com.stazo.project_18;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by ericzhang on 6/20/16.
 */
public class ViewCommentFrag extends Fragment{
    private String passedEventID;
    private View v;
    private Activity a;
    private int numCommentsLoaded = 0;

    public void submitComment() {
        Firebase fb = ((Converg) this.getActivity().getApplication()).getFB();
        String commentText = ((EditText) v.findViewById(R.id.commentText)).getText().toString();

        //used push instead of updating an arrray list, pushing it into the comments array of
        //the EventComments tied to an Event_ID
        String user_ID = ((Converg)this.getActivity().getApplication()).getMe().getID();
        Comment comment = new Comment(this.passedEventID, commentText, user_ID);
        fb.child("CommentDatabase").child(this.passedEventID).child("comments").push().setValue(comment);

        //hide keyboard and remove text after comment is pushed
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        ((EditText) v.findViewById(R.id.commentText)).setText(null);

        // NOTIFICATION STUFF
        ArrayList<String> usersWhoCare = new ArrayList<>(EventInfoFrag.currEvent.getAttendees());
        if (usersWhoCare.contains(Converg.me.getID())) {
            usersWhoCare.remove(Converg.me.getID());
        }

        ArrayList<String> meList = new ArrayList<String>();
        meList.add(Converg.me.getName());

        // send out notification
        (new NotificationCommentEvent(Notification2.TYPE_COMMENT_EVENT,
                meList,
                passedEventID,
                EventInfoFrag.currEvent.getName(),
                Converg.me.getID())).
                pushToFirebase(fb, usersWhoCare);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.view_comment, container, false);
        final Firebase fb = ((Converg) this.getActivity().getApplication()).getFB();
        final Context context = getContext();
        a = getActivity();
        fb.child("CommentDatabase").child(this.passedEventID).addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //get arraylist of comments from iterable snapshots by iterating through and adding
                        final ArrayList<Comment> commentList = new ArrayList<Comment>();
                        Iterable<DataSnapshot> commentIterable = dataSnapshot.child("comments").getChildren();
                        while (commentIterable.iterator().hasNext()) {
                            commentList.add((Comment) commentIterable.iterator().next().getValue(Comment.class));
                        }
                        //show through views and layouts
                        for (int i = numCommentsLoaded; i < commentList.size(); i++) {

                            //profile pic
                            Bitmap profileImage = null;
                            ImageView profileView = new ImageView(context);
                            profileView.setImageBitmap(profileImage);
                            //get cache and check ID against it
                            //HashMap<String, Bitmap> imageCache = Converg.cachedIdToBitmap;
                            if (((Converg) getActivity().getApplication()).
                                    getBitmapFromMemCache(commentList.get(i).getUser_ID()) != null) {
                                System.out.println("cache hit");
                                profileImage = ((Converg) getActivity().getApplication()).
                                        getBitmapFromMemCache(commentList.get(i).getUser_ID());
                                profileView.setImageBitmap(Converg.BITMAP_RESIZER(profileImage, 150, 150));
                            } else {
                                Thread profileThread = new Thread(new ProfilePicRunnable(profileImage, commentList.get(i).getUser_ID(), profileView));
                                profileThread.start();
                            }

                            //user_id
                            final TextView userText = new TextView(context);
                            if (Converg.cachedIdToName.containsKey(commentList.get(i).getUser_ID())) {
                                userText.setText(Converg.cachedIdToName.get(commentList.get(i).getUser_ID()));
                                System.out.println("name cache hit");
                            } else {
                                fb.child("Users").child(commentList.get(i).getUser_ID()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        userText.setText((String) dataSnapshot.child("name").getValue());
                                        Converg.cachedIdToName.put(dataSnapshot.getKey(), (String) dataSnapshot.child("name").getValue());
                                    }

                                    @Override
                                    public void onCancelled(FirebaseError firebaseError) {

                                    }
                                });
                            }


                            //comment
                            TextView commentText = new TextView(context);
                            commentText.setText(commentList.get(i).getComment());


                            //layout
                            LinearLayout mainLayout = (LinearLayout) v.findViewById(R.id.viewCommentLayout);
                            LinearLayout commentLayout = new LinearLayout(context);
                            commentLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                    LayoutParams.MATCH_PARENT));
                            commentLayout.setOrientation(LinearLayout.HORIZONTAL);
                            LinearLayout textLayout = new LinearLayout(context);
                            textLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT));
                            textLayout.setOrientation(LinearLayout.VERTICAL);

                            textLayout.addView(userText);
                            textLayout.addView(commentText);
                            commentLayout.addView(profileView);
                            commentLayout.addView(textLayout);

                            //layout params for views
                            profileView.setLayoutParams(new LayoutParams(getDPI(60), getDPI(70)));
                            LayoutParams userTextLayoutParams = new LayoutParams((getDPI(250)), getDPI(20));
                            userTextLayoutParams.setMargins(getDPI(10), 0, 0, 0);
                            userText.setLayoutParams(userTextLayoutParams);
                            userText.setTypeface(null, Typeface.BOLD);
                            userText.setTextSize(16);
                            LayoutParams commentTextLayoutParams = new LayoutParams(getDPI(250), LinearLayout.LayoutParams.WRAP_CONTENT);
                            commentTextLayoutParams.setMargins(getDPI(10), 0, 0, 0);
                            commentText.setLayoutParams(commentTextLayoutParams);

                            //spacer and inc counter
                            View space = inflater.inflate(R.layout.spacer, null);
                            getActivity().runOnUiThread(new UpdateViewRunnable(mainLayout, commentLayout, space));
                            numCommentsLoaded++;
                        }

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                    }
                });

        //set up write comment
        final Button submitComment = (Button) v.findViewById(R.id.submitComment);
        submitComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment();
            }
        });
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return v;
    }

    private class ProfilePicRunnable implements Runnable {
        private Bitmap profileImage;
        String user_ID;
        ImageView profileView;
        ProfilePicRunnable(Bitmap profileImage, String user_ID, ImageView profileView) {
            this.profileImage = profileImage;
            this.user_ID = user_ID;
            this.profileView = profileView;
        }
        public void run() {
            try {
                URL imageURL = new URL("https://graph.facebook.com/" + user_ID
                        + "/picture?width=" + Converg.pictureSize);
                profileImage = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
                profileView.post(new Runnable() {
                    @Override
                    public void run() {
                        profileView.setImageBitmap(Converg.BITMAP_RESIZER(profileImage, 150, 150));
                    }
                });

                // add to cache
                ((Converg) getActivity().getApplication()).
                        addBitmapToMemoryCache(user_ID, profileImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private class UpdateViewRunnable implements Runnable {
        private LinearLayout mainLayout;
        private View addedView;
        private View addedView2;

        UpdateViewRunnable(LinearLayout mainLayout, View addedView, View addedView2) {
            this.mainLayout = mainLayout;
            this.addedView = addedView;
            this.addedView2 = addedView2;
        }
        @Override
        public void run() {
            mainLayout.addView(addedView);
            mainLayout.addView(addedView2);
            System.out.println("run on ui thread");
        }
    }

    public int getDPI(int size){
        DisplayMetrics metrics;
        metrics = new DisplayMetrics();
        if (getActivity() == null) {
            System.out.println("null");
        }
        a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }

    public void setEventID(String passedEventID) {
        this.passedEventID = passedEventID;
    }

}
