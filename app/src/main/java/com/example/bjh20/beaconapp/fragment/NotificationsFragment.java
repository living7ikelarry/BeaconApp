package com.example.bjh20.beaconapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.bjh20.beaconapp.R;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.bjh20.beaconapp.BeaconApplication.beaconAdapter;
import static com.example.bjh20.beaconapp.BeaconApplication.beaconList;
import static com.example.bjh20.beaconapp.BeaconApplication.beaconListView;
import static com.example.bjh20.beaconapp.BeaconApplication.notificationAdapter;
import static com.example.bjh20.beaconapp.BeaconApplication.notificationList;

/**
 * Created by bjh20 on 3/15/2018.
 */

public class NotificationsFragment extends Fragment {

    private String[] notificationListArray;
    private Timer timer;
    private ListView notificationListView;

    public NotificationsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        notificationListView = (ListView) view.findViewById(R.id.notification_list_view);
        notificationListArray = new String[notificationList.size()];
        notificationListArray = notificationList.toArray(notificationListArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, notificationListArray);
        notificationListView.setAdapter(adapter);

        updateList();

        return view;
    }

    private void updateList() {
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                String[] newListArray = new String[notificationList.size()];
                newListArray = notificationList.toArray(newListArray);
                if (notificationListArray != null && newListArray != notificationListArray) {
                    notificationListArray = newListArray;
                    notificationAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, notificationListArray);
                    Handler refresh = new Handler(Looper.getMainLooper());
                    refresh.post(new Runnable() {
                        public void run() {
                            notificationListView.setAdapter(notificationAdapter);
                        }
                    });
                }

            }

        },0,1000);//Check list every second
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timer.cancel();
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);

    }
}
