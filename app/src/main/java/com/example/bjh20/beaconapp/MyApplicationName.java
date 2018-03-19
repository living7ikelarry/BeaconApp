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

import java.util.Collection;

/**
 * Created by bjh20 on 3/13/2018.
 */

public class MyApplicationName extends Application implements BootstrapNotifier , BeaconConsumer, RangeNotifier {
    private static final String TAG = ".MyApplicationName";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private MainActivity rangingActivity = null;
    private BeaconManager beaconManager;

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

        beaconManager.setBackgroundScanPeriod(5000l);
        beaconManager.setBackgroundBetweenScanPeriod(1000l);
        //beaconManager.setForegroundBetweenScanPeriod(10000l);
        //beaconManager.setForegroundScanPeriod(50000l);

        beaconManager.bind(this);
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "did enter region.");
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        }
        catch (RemoteException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Can't start ranging");
        }
    }

    @Override
    public void didExitRegion(Region region) {
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG,"I have just switched from seeing/not seeing beacons: " + state);
    }

    @TargetApi(16)
    private void sendNotification(String text) {
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
        notificationManager.notify(1, builder.build());
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            for (Beacon b : beacons) {
                if(b.getId3().toInt() == 1) {
                    Log.e(TAG, "Beacon with my Instance ID found!");
                    sendNotification("Beacon with my Instance ID found!");
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(this);
    }
}

