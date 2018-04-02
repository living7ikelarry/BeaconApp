package com.example.bjh20.beaconapp;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.bjh20.beaconapp.activity.MainActivity;
import com.example.bjh20.beaconapp.fragment.BeaconsFragment;
import com.example.bjh20.beaconapp.fragment.SettingsFragment;
import com.example.bjh20.beaconapp.other.FetchData;
import com.squareup.picasso.Picasso;
//import com.google.android.gms.common.api.Response;

import com.squareup.picasso.Picasso;

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
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by bjh20 on 3/13/2018.
 */

public class BeaconApplication extends Application implements BootstrapNotifier , BeaconConsumer, RangeNotifier {
    private static final String TAG = ".BeaconApplication";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private MainActivity rangingActivity = null;
    private BeaconManager beaconManager;
    private ArrayList<Integer> currentTimeInSeconds;
    private ArrayList<Integer> firstSawBeacon;


    public static List<String> notificationList;
    public static List<String> beaconList;
    public static ArrayAdapter<String> notificationAdapter;
    public static ArrayAdapter<String> beaconAdapter;
    public static ListView beaconListView;


    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    RequestQueue requestQueue;
    SharedPreferences sharedPreferences;

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

        //get current value for scanning enabled
        //if true allow scanning to start
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean scanIsEnabled = sharedPreferences.getBoolean(SettingsFragment.SCAN_ENABLED, true);


        //beacon background scanning
        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Region region = new Region("region1", Identifier.parse("b9407f30-f5f8-466e-aff9-25556b57fe6d"), null, null);

        if (scanIsEnabled) {
            regionBootstrap = new RegionBootstrap(this, region);
        }

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.setBackgroundScanPeriod(5000);
        beaconManager.setBackgroundBetweenScanPeriod(1000);
        beaconManager.setForegroundScanPeriod(5000);
        beaconManager.setForegroundBetweenScanPeriod(1000);


        beaconManager.bind(this);


        //request queue for api calls
        requestQueue = Volley.newRequestQueue(this);

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
            firstSawBeacon.add(0);
        }

        //set initial time
        Date currentDate = new Date();
        long currentTime = currentDate.getTime();
        currentTimeInSeconds.add(0, (int)TimeUnit.MILLISECONDS.toSeconds(currentTime));

        //fetch beacon data from json api
        FetchData fetchBeaconData = new FetchData();
        fetchBeaconData.execute();

        //hard-coded names for now. just for local testing
        beaconNames[1] = "Taco Bell";
        beaconNames[2] = "Burger King";
        beaconNames[3] = "Chick Fil A";

        //hard coded notification messages for now. just for local testing
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
        if (state == 0) {
            beaconList.clear();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    if (beaconAdapter != null) {
                        beaconAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    @TargetApi(16)
    private void sendNotification(int majorID, String name, String description, Bitmap adImage) {
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setContentTitle(name)
                        .setContentText(description)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(adImage)
                        .setStyle(new Notification.BigPictureStyle().bigPicture(adImage))
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(Notification.PRIORITY_MAX);
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

        notificationManager.notify(majorID, builder.build());
        int listCount = 0;
        if (notificationList != null) {
            for (int i = 0; i < notificationList.size(); i++) {
                if (notificationList.get(i).equals(description)) {
                    listCount++;
                }
            }
        }
        if (listCount == 0) {
            notificationList.add(description);
        }
    }

    public void notificationWithDelay(int majorID, String name, String description, Bitmap adImage) {
        Date nextDate = new Date();
        nextTime = nextDate.getTime();
        nextTimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(nextTime);
        if(nextTimeInSeconds >= currentTimeInSeconds.get(majorID) + 300 || isFirstNotificationSinceOpening) {

            sendNotification(majorID, name, description, adImage);
            currentTimeInSeconds.set(majorID, nextTimeInSeconds);

            if(isFirstNotificationSinceOpening) {
                isFirstNotificationSinceOpening = false;
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            for (Beacon b : beacons) {
                //set initial time for specific beacon ranged if it doesn't exist yet
                int majorID = b.getId3().toInt();
                if(currentTimeInSeconds.get(majorID) == null) {
                    Date newBeaconDate = new Date();
                    long newBeaconTime = newBeaconDate.getTime();
                    currentTimeInSeconds.add(majorID, (int) TimeUnit.MILLISECONDS.toSeconds(newBeaconTime));
                }
                requestNotificationFromServer(majorID);
                Log.e(TAG, "Beacon with ID " + majorID + " found!");
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(this);
    }

    private void requestNotificationFromServer(final int majorID) {
        String url = "http://192.168.1.206:3000/api/app";
        JSONObject postBody = new JSONObject();
        try {
            postBody.put("beaconID", 1);
            postBody.put("tagid", "5abd1d2bc510020109e3b152");
        } catch (JSONException error) {
            Log.d("api", "Can't create post body");
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, postBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //executes on response from server
                        String name = "";
                        String description = "";
                        String link = "";
                        if (response != null) {
                            try {
                                JSONObject data = response.getJSONObject("data");
                                name = data.getString("name");
                                description = data.getString("description");
                                JSONObject media = data.getJSONObject("mediapath");
                                link = media.getString("link");
                            } catch (JSONException error){
                                Log.d("api", "Can't parse response");
                            }

                            sendImageWithNotification(majorID, name, description, link);

                            int listCount = 0;
                            if (beaconList != null) {
                                for (int i = 0; i < beaconList.size(); i++) {
                                    if (beaconList.get(i).equals(name)) {
                                        listCount++;
                                    }
                                }
                            }
                            if (listCount == 0) {
                                beaconList.add(name);
                            }
                        }
                        else { Log.d("api", "response is null"); }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.d("api", "volley error on text");

                    }
                });
        requestQueue.add(jsonObjectRequest);

    }

    public void sendImageWithNotification(final int majorID, final String name, final String description, String link) {
        //retrieves the image from link, creates bitmap, and passes to the notification builder
        //only does so on response from server
        ImageRequest imageRequest = new ImageRequest(link,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        if (bitmap != null) {
                            notificationWithDelay(majorID, name, description, bitmap);
                        }
                        else { Log.d("api", "issue with bitmap response"); }

                        }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Log.d("api", "volley error on image");
                    }
                });
        requestQueue.add(imageRequest);
    }

}

