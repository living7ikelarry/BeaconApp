package com.example.bjh20.beaconapp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.bjh20.beaconapp.R;

import static com.example.bjh20.beaconapp.BeaconApplication.notificationList;

/**
 * Created by bjh20 on 3/15/2018.
 */

public class NotificationsFragment extends Fragment {

    public NotificationsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        ListView notificationListView = (ListView) view.findViewById(R.id.notification_list_view);
        String[] notificationListArray = new String[notificationList.size()];
        notificationListArray = notificationList.toArray(notificationListArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, notificationListArray);
        notificationListView.setAdapter(adapter);
        //adapter.notifyDataSetChanged();
        //this is likely unnecessary^^

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);

    }
}
