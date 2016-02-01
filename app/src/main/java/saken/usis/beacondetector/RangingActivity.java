package saken.usis.beacondetector;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;


public class RangingActivity extends ListActivity implements BeaconConsumer{

    protected static final String TAG = "Ranging";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    //BeaconManager handles all aoperations realated with Beacons
    private BeaconManager beaconManager;
    //BeaconList will store the beacons
    private BeaconListAdapter mBeaconList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Beacon Manager initialize
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        //Verify bluetooth on mobile device
        verifyBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

        //Adding beacon parser according beacon specifications
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=5203,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        mBeaconList = new BeaconListAdapter();
        setListAdapter(mBeaconList);

        beaconManager.bind(this);
    }

    /*
        Following method scans for beacons in the background.
        If it finds some beacons, it will all of them to the Beacon List
    */
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(final Collection<Beacon> beacons, Region region) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (beacons.size() > 0) {
                            for (Beacon beacon : beacons) mBeaconList.addBeacon(beacon);
                            mBeaconList.notifyDataSetChanged();
                        }
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            Log.d(TAG, "Exception caught in ServiceConnect");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    /*
        Class which stores information about beacons.
        Using class methods we can update and add new detected beacons.
        'getView' method will deliver the data to the screen.
     */
    private class BeaconListAdapter extends BaseAdapter {
        private ArrayList<Beacon> mBeacons;
        private LayoutInflater mInflator;

        public BeaconListAdapter() {
            super();
            mBeacons = new ArrayList<Beacon>();
            mInflator = RangingActivity.this.getLayoutInflater();
        }

        public void addBeacon(Beacon beacon) {
            int index=mBeacons.indexOf(beacon);
            if (index==-1){
                mBeacons.add(beacon);
            }
            else{
                mBeacons.set(index,beacon);
            }
        }

        public void clear() {
            mBeacons.clear();
        }

        @Override
        public int getCount() {
            return mBeacons.size();
        }

        @Override
        public Object getItem(int position) {
            return mBeacons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        //Exports the data on to the screen of the mobile phone
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if(view == null) {
                view = mInflator.inflate(R.layout.list_beacon, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceDist = (TextView) view.findViewById(R.id.device_dist);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            Beacon beacon = mBeacons.get(position);
            final String beaconName = beacon.getBluetoothName();
            if(beaconName!=null && beaconName.length() > 0)
                viewHolder.deviceName.setText(beaconName);
            else
                viewHolder.deviceName.setText(R.string.unknown_beacon);

            viewHolder.deviceAddress.setText(beacon.getBluetoothAddress());
            viewHolder.deviceDist.setText(String.valueOf(beacon.getDistance()));

            return view;
        }
    }

    /*
          Simple class to store information about each beacon.
     */
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceDist;
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

        }

    }
}
