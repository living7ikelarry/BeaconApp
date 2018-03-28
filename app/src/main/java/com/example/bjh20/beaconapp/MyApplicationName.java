package com.example.bjh20.beaconapp;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.bjh20.beaconapp.activity.MainActivity;

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
    private List<String> notificationList;


    int notificationCount = 0;

    //starting time
    Date currentDate = new Date();
    long currentTime = currentDate.getTime();
    int currentTimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(currentTime);

    //create next time
    long nextTime;
    int nextTimeInSeconds;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
        Region region = new Region("region1", Identifier.parse("b9407f30-f5f8-466e-aff9-25556b57fe6d"), null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.setBackgroundScanPeriod(5000);
        beaconManager.setBackgroundBetweenScanPeriod(1000);
        beaconManager.setForegroundScanPeriod(5000);
        beaconManager.setForegroundBetweenScanPeriod(1000);


        beaconManager.bind(this);

        notificationList = new ArrayList<>();

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
    private void sendNotification(String text, int notificationCount) {
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
        if (notificationCount >= 50) {
            notificationManager.cancelAll();
        }
        notificationManager.notify(notificationCount, builder.build());
        notificationList.add(text);
    }

    public void notificationWithDelay() {
        Date nextDate = new Date();
        nextTime = nextDate.getTime();
        nextTimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(nextTime);
        if(nextTimeInSeconds >= currentTimeInSeconds + 300) {
            sendNotification("Beacon with my ID found!", notificationCount);
            if (notificationCount < 50) {
                notificationCount++;
            }
            else {
                notificationCount = 0;
            }
            currentTimeInSeconds = nextTimeInSeconds;
        }
    }

    public String[] getList() {
        String [] notificationListArray = new String[notificationList.size()];
        notificationListArray = notificationList.toArray(notificationListArray);
        return notificationListArray;
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            for (Beacon b : beacons) {
                if(b.getId3().toInt() == 1) {
                    Log.e(TAG, "Beacon with my ID found!");
                    notificationWithDelay();
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(this);
    }
}

