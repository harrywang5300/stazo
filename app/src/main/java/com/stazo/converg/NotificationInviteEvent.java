package com.stazo.converg;

import android.content.Context;

import com.firebase.client.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by isaacwang on 6/30/16.
 */


public class NotificationInviteEvent extends Notification2 {

    private String eventId;
    private String eventName;
    private ArrayList<String> userNames = new ArrayList<String>();

    public NotificationInviteEvent(int type, ArrayList<String> userNames,
                                    String eventId,
                                    String eventName,
                                   String pictureId) {
        super(type, pictureId);
        this.userNames = userNames;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public NotificationInviteEvent(HashMap<String, Object> notifMap) {
        super(((Long) notifMap.get("type")).intValue(), (String) notifMap.get("notifID"),
                (String) notifMap.get("pictureId"));
        for (String s: (Iterable<String>) notifMap.get("userNames")) {
            this.userNames.add(s);
        }
        this.eventId = (String) notifMap.get("eventId");
        this.eventName = (String) notifMap.get("eventName");
        setViewed((Boolean) notifMap.get("viewed"));
    }

    public NotificationInviteEvent(NotificationInviteEvent other) {
        super(other.getType(), other.getNotifID(), other.getPictureId());
        this.userNames = other.getUserNames();
        this.eventId = other.getEventId();
        this.eventName = other.getEventName();
        setViewed(other.isViewed());
    }

    public void onNotificationClicked(Context context) {
        ((MainAct) context).goToEventInfo(eventId, true);
        setViewed(true);
    }

    public String generateMessage(){
        String message = "";
        message += userNames.get(0);
        if (userNames.size() > 1) {
            message += " and " + (userNames.size()-1) + " other";
        }
        if (userNames.size() > 2) {
            message += "s";
        }
        message += " invited you to \"" + eventName + "\".";
        return filterMessageByLength(message);
    }

    public SnapToBase hasConflict(DataSnapshot userNotifs) {
        for (DataSnapshot notif: userNotifs.getChildren()) {
            HashMap<String, Object> notifMap = (HashMap<String, Object>) notif.getValue();
            if (((Long) notifMap.get("type")).intValue() != Notification2.TYPE_INVITE_EVENT) {
                continue;
            }
            NotificationInviteEvent nie = new NotificationInviteEvent(notifMap);
            if (nie.getEventId().equals(eventId) ) {
                return new SnapToBase(notif, notif.getRef());
            }
        }
        return null;
    }

    public Notification2 handleConflict(SnapToBase stb) {

        // get old notif
        NotificationInviteEvent conflictNotif = new NotificationInviteEvent(
                (HashMap<String, Object>) stb.getSnap().getValue());

        // update new notif
        for (String s: conflictNotif.getUserNames()) {
            if (!userNames.contains(s)) {
                userNames.add(s);
            }
        }

        // remove old conflict
        stb.getBase().setValue(null);

        return new NotificationInviteEvent(this);
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public ArrayList<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(ArrayList<String> userNames) {
        this.userNames = userNames;
    }

    public String getEventId() {

        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}
