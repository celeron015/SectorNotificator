package com.example.celer.sectornotificator;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import com.example.celer.sectornotificator.MapsActivity;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by celer on 13-Mar-17.
 */

public class GPSTracker extends Service implements LocationListener {
    private final Context context;
    boolean isGPSEnabled=false;
    boolean isNetworkEnabled=false;
    boolean canGetLocation=false;

    Location location;
    protected LocationManager locationManager;
    public GPSTracker(Context context){
        this.context=context;
    }

    public Location getLocation(){
        try{
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            isGPSEnabled=locationManager.isProviderEnabled(locationManager.GPS_PROVIDER);
            isNetworkEnabled=locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER);

            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                if(isGPSEnabled){
                    if(location==null){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,50,this);
                        if(locationManager!=null){
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                    }
                }

                if(location==null){
                    if(isNetworkEnabled){

                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000,30,this);
                        if(locationManager!=null){
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }

                    }
                }
            }
            //OVO SAM SVE KOPIRAO ZNAM JA KURAC MOJ STA JE
            //RADIM PAGINACIJU
        }catch (Exception ex){

        }
        return location;
    }

    public static Location mLastLocation;

    public void onLocationChanged(Location location){
        //calcul manually speed

        boolean b = Sectors.goingToAnotherSector(location); //provere svakakve neke da vidimo kud ide iksan
        if (b){
            MapsActivity.KRETANJE="KRECES SE";
        }else{
            MapsActivity.KRETANJE="NE KRECES SE";
        }
        double speed = 0;
        if (this.mLastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude(),
                    results);
            speed = results[0] / (location.getTime() - this.mLastLocation.getTime());
        }
        //if there is speed from location
        if (location.hasSpeed())
            //get location speed
            speed = location.getSpeed();

        this.mLastLocation = location;

        MapsActivity.speed=speed;
        MapsActivity.speed=(double)Math.round(MapsActivity.speed * 1000d)/1000d;
        boolean isCurentLocationSoFarOfPolygon=false;
        for (LatLng middlePont:Sectors.middlePoints) {
            float[] results = new float[1];
            Location.distanceBetween(
                    middlePont.latitude,
                    middlePont.longitude,
                    location.getLatitude(),
                    location.getLongitude(),
                    results);
            if(results[0] > 500){
                isCurentLocationSoFarOfPolygon=true; //ako je 500m od poligona sledeci cp hvatam za 2min valjda za to vreme ne moze napraviti sranje
                //stedimo bateriju sirotinji
            }
        }




        if(isCurentLocationSoFarOfPolygon){
            MapsActivity.mInterval=120000; //2 minutes
        }else{
            if (speed < 5){
                MapsActivity.mInterval=3000;
            }else if (speed>4 && speed<10){
                MapsActivity.mInterval=3333;
            }else if(speed>9 && speed<15){
                MapsActivity.mInterval=3666;
            }else if(speed>14){
                MapsActivity.mInterval=3999;
            }
        } //u zavisnosti od brzine menjam interval mada isti k je samo sam gledao jel umem da uradim pa mi zao da izbrisem

    }

    public void onStatusChanged(String Provider, int status, Bundle extras){

    }

    public void onProviderEnabled(String Provider){

    }

    public void onProviderDisabled(String Provider){

    }

    public IBinder onBind(Intent arg0){
        return null;
    }
}
