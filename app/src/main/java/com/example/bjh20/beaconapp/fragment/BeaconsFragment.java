package com.example.bjh20.beaconapp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.bjh20.beaconapp.MyApplicationName;
import com.example.bjh20.beaconapp.R;

import java.util.List;

/**
 * Created by bjh20 on 3/15/2018.
 */

public class BeaconsFragment extends Fragment{

    private String[] items = {"Banana", "Lime"};

    public BeaconsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        View view = inflater.inflate(R.layout.fragment_beacons, container, false);

        ListView beaconListView = (ListView) view.findViewById(R.id.beacon_list_view);
        MyApplicationName app = new MyApplicationName();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, items);
        beaconListView.setAdapter(adapter);
        //adapter.notifyDataSetChanged();

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);

    }

}
