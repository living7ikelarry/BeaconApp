package com.example.bjh20.beaconapp;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.bjh20.beaconapp.activity.MainActivity;
import com.example.bjh20.beaconapp.other.FetchData;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.Identifier;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by bjh20 on 3/13/2018.
 */

public class MyApplicationName extends Application implements BootstrapNotifier , BeaconConsumer, RangeNotifier {
    private static final String TAG = ".MyApplicationName";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private MainActivity rangingActivity = null;
    private BeaconManager beaconManager;
    private ArrayList<Integer> currentTimeInSeconds;
    private ArrayList<Integer> firstSawBeacon;


    public static List<String> notificationList;
    public static List<String> beaconList;

    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


    int notificationCount = 0;

    String[] beaconNames = new String[4];
    String[] beaconMessages = new String[4];


    //is this the first notification? necessary boolean for the timer/delay
    boolean isFirstNotificationSinceOpening = true;



    //create next time
    long nextTime;
    int nextTimeInSeconds;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Region region = new Region("region1", Identifier.parse("b9407f30-f5f8-466e-aff9-25556b57fe6d"), null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.setBackgroundScanPeriod(5000);
        beaconManager.setBackgroundBetweenScanPeriod(1000);
        beaconManager.setForegroundScanPeriod(5000);
        beaconManager.setForegroundBetweenScanPeriod(1000);


        beaconManager.bind(this);

        notificationList = new ArrayList<>();
        beaconList = new ArrayList<>();
        currentTimeInSeconds = new ArrayList<>();
        firstSawBeacon = new ArrayList<>();
        //fill arrayList so not null initially. can add/alter values later this way
        for (int i = 0; i < 10; i++) {
            currentTimeInSeconds.add(0);
        }
        //fill beaconList as well
        for (int i = 0; i < 3; i++) {
            beaconList.add("");
            notificationList.add("");
            firstSawBeacon.add(0);
        }

        //set initial time
        Date currentDate = new Date();
        long currentTime = currentDate.getTime();
        currentTimeInSeconds.add(0, (int)TimeUnit.MILLISECONDS.toSeconds(currentTime));

        //fetch beacon data from json api
        FetchData fetchBeaconData = new FetchData();
        fetchBeaconData.execute();

        //hard-coded names for now
        beaconNames[1] = "Taco Bell";
        beaconNames[2] = "Burger King";
        beaconNames[3] = "Chick Fil A";

        //hard coded notification messages for now
        beaconMessages[1] = "Welcome to Taco Bell!";
        beaconMessages[2] = "Welcome to Burger King!";
        beaconMessages[3] = "Welcome to Chick Fil A!";

    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "did enter region.");
    }

    @Override
    public void didExitRegion(Region region) {
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //Found at then when already in the range of beacon, didEnterRegion will fail to activate
    //Instead, utilizing didDetermineStateForRegion worked in the way I needed it to
    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG,"I have just switched from seeing/not seeing beacons: " + state);
        if (state == 1) {
            try {
                beaconManager.startRangingBeaconsInRegion(region);
            }
            catch (RemoteException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Can't start ranging");
            }
        }
    }

    @TargetApi(16)
    private void sendNotification(String text, int majorID) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= 21) builder.setVibrate(new long[0]);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        //unnecessary now
        /*
        if (notificationCount >= 50) {
            notificationManager.cancelAll();
        }
        */
        notificationManager.notify(majorID, builder.build());
        notificationList.add(majorID, text);
    }

    public void notificationWithDelay(int majorID) {
        Date nextDate = new Date();
        nextTime = nextDate.getTime();
        nextTimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(nextTime);
        if(nextTimeInSeconds >= currentTimeInSeconds.get(majorID) + 300 || isFirstNotificationSinceOpening) {
            sendNotification(beaconMessages[majorID], majorID);
            //this continually adds notifications which is not what we want moving forward
            //instead notifications should match beaconID. one notification per beacon for now
            /*
            if (notificationCount < 50) {
                notificationCount++;
            }
            else {
                notificationCount = 0;
            }
            */
            currentTimeInSeconds.add(majorID, nextTimeInSeconds);

            if(isFirstNotificationSinceOpening) {
                isFirstNotificationSinceOpening = false;
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            for (Beacon b : beacons) {
                //pass id to notification manager instead of this
                /*
                if(b.getId3().toInt() == 1) {
                    Log.e(TAG, "Beacon with my ID found!");
                    notificationWithDelay();
                }
                */

                //set initial time for specific beacon ranged if it doesn't exist yet
                if(currentTimeInSeconds.get(b.getId3().toInt()) == null) {
                    Date newBeaconDate = new Date();
                    long newBeaconTime = newBeaconDate.getTime();
                    currentTimeInSeconds.add(b.getId3().toInt(), (int) TimeUnit.MILLISECONDS.toSeconds(newBeaconTime));
                }
                beaconList.set(b.getId3().toInt()-1, beaconNames[b.getId3().toInt()]);
                notificationWithDelay(b.getId3().toInt());
                Log.e(TAG, "Beacon with ID " + b.getId3().toInt() + " found!");
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(this);
    }
}

