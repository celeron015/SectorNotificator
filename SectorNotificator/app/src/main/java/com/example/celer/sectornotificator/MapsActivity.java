package com.example.celer.sectornotificator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    boolean activeButton = false;
    public static GoogleMap mMap;
    private GPSTracker gpsTracker;
    private Location mLocation;
    static int mInterval = 3000; // interval na koliko se poziva hendler
    static double speed=0.0;
    private Handler mHandler; //hendler
    List<LatLng> checkPointList = new ArrayList<>();
    public static String KRETANJE = "";
    public static String ALARM = "";
    public static boolean changedWay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mHandler = new Handler();
    }

    void startRepeatingTask() {
        mStatusChecker.run(); //start hendlera
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker); //stop hendlera
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                gpsTracker=new GPSTracker(getApplicationContext());
                mLocation = gpsTracker.getLocation(); //TRENUTNA LOKACIJA

                LatLng checkpoint = new LatLng( mLocation.getLatitude(), mLocation.getLongitude());
                gpsTracker.onLocationChanged(mLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(checkpoint));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(checkpoint));

                checkPointList.add(checkpoint);
                TextView kretanje_tf = (TextView) findViewById(R.id.TFkretanje);
                kretanje_tf.setText(KRETANJE);

                TextView alarm_tf = (TextView) findViewById(R.id.TFalarm);
                alarm_tf.setText(ALARM);
//                if(Sectors.alarm){
//                    alarm_tf.setTextColor(((int) Math.random())); //hteo da menjam boju teksta kada se promeni put mada mi nesto nije radilo
//                }
                TextView podaci_tf=(TextView) findViewById(R.id.TFpodaci);
                if (speed<2.778) {
                    podaci_tf.setText("Number of checkpoints:" + checkPointList.size() + "  Speed: " + speed + "m/s");
                }else{//pokazuje u km/h posle 9.9km/h
                    podaci_tf.setText("Number of checkpoints:" + checkPointList.size() + "  Speed: " + speed*3.6 + "km/h");
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception

                mHandler.postDelayed(mStatusChecker, mInterval);

            }
        }
    };

    public void onStart(View view) {
        activeButton = !activeButton;
        Button button = (Button) findViewById(R.id.Bstart);
        TextView podaci_tf = (TextView) findViewById(R.id.TFpodaci);
        if (activeButton) {
            Sectors.currentNearestSector=null;
            changedWay = false;
            podaci_tf.setText(null);
            button.setText("STOP");
            checkPointList.clear();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startRepeatingTask(); //startuje hendler nesto pukne kad se zove alarm pri prvoj lokaciji, tamo sam napisao to u klasi sectors, a nekad i radi nmpm zasto
            try { //dodao sleep da vidim da odlozim kao to malo mada mozda je do mog telefona on jebe to sa lokacijom nesto
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //trekovanje treba da traje 9 sati ???
        } else {
            Sectors.currentNearestSector=null;
            changedWay = false;
            button.setText("START");
            stopRepeatingTask();  //stopira hendler
            podaci_tf.setText("Number of checkpoints:" + checkPointList.size());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        Sectors.redrawLine(); //ucrtavaju se sektori sirotinji ovo govno gore mi je sam izgenerisao iz njemu znanih razloga
    }
}
