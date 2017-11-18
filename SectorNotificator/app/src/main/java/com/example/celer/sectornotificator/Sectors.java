package com.example.celer.sectornotificator;

import android.graphics.Color;
import android.location.Location;
import android.widget.TextView;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by celer on 16-Nov-17.
 */

public class Sectors extends MapsActivity{

    public static List<Polygon> polygons = new ArrayList<>();
    public static List<List<LatLng>> sectors = new ArrayList<>();
    public static List<LatLng> middlePoints = new ArrayList<>();

    public static void redrawLine() {
        //ucitavanje poligona, bice zakucano, ukoliko zelis da testiras skini fieldmarker, jednu od najboljih besplatnih aplikacija simple taska
        //i ici catch>catch>catch>catch zatim iscrtaj area i onda imas dole levo opciju za save, cuva se u lokalnoj SQLite bazi telefona
        //zatim odes na onaj view sto iskace pipnes to sto si sacuvao i posaljes sebi na email, dobices ovakve koordinate kao dole ispod
        //nemoj da pomesas latitude i longitude kao ja sto sam pomesao... zatim uradi to i za neke test tacke ili nadji neki pametniji nacin za
        //lokacije pa ih dole kroz debug stavljaj i vidi kako ce se ponasati
        List<LatLng> sector1points = new ArrayList<>(); //sector1
        LatLng sector1pt1 = new LatLng(45.26404556344096, 19.835033230483532);
        LatLng sector1pt2 = new LatLng(45.26316369577091, 19.835243448615074);
        LatLng sector1pt3 = new LatLng(45.26347165486282, 19.836067222058773);
        LatLng sector1pt4 = new LatLng(45.26417960046393, 19.83583554625511);
        sector1points.add(sector1pt1);
        sector1points.add(sector1pt2);
        sector1points.add(sector1pt3);
        sector1points.add(sector1pt4);

        List<LatLng> sector2points = new ArrayList<>(); //sector2
        LatLng sector2pt1 = new LatLng(45.26317148326279, 19.834171235561374);
        LatLng sector2pt2 = new LatLng(45.26359318615203, 19.833991527557373);
        LatLng sector2pt3 = new LatLng(45.263429177846014, 19.833559691905975);
        LatLng sector2pt4 = new LatLng(45.26309077647697, 19.83379304409027);
        sector2points.add(sector2pt1);
        sector2points.add(sector2pt2);
        sector2points.add(sector2pt3);
        sector2points.add(sector2pt4);       

        sectors.add(sector1points); //add sector1
        sectors.add(sector2points); //add sector2
        drawPolygones(sectors);

    }
    static int polygonNumber = 1;
    public static void drawPolygones(List<List<LatLng>> sectors){
        //crta sve te sektore na mapu
        for(List<LatLng> sectorPoints:sectors){
            PolygonOptions options = new PolygonOptions().strokeWidth(5).strokeColor(Color.RED).fillColor(Color.argb(60, 255, 0, 0));

            for (LatLng point:sectorPoints) {
                options.add(point);
            }

            Polygon loadedPolygon = mMap.addPolygon(options);

            polygons.add(loadedPolygon);
            middlePoints.add(getPolygonCenterPoint(sectorPoints, polygonNumber)); //srednja tacka u poligonu, sluzi za proveru da li se korisnik krece iz jednog ka drugom sektoru
            polygonNumber++;
        }
    }

    private static LatLng getPolygonCenterPoint(List<LatLng> sectorPoints, int polygonNumber){
        LatLng centerLatLng = null;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for(int i = 0 ; i < sectorPoints.size() ; i++)
        {
            builder.include(sectorPoints.get(i));
        }

        LatLngBounds bounds = builder.build();
        centerLatLng =  bounds.getCenter();
        mMap.addMarker(new MarkerOptions().position(centerLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title("Sector "+polygonNumber));
        return centerLatLng; //srednja tacka poligona
    }

    public static LatLng currentNearestSector = null;
    public static boolean alarm= false;
    
    public static boolean goingToAnotherSector(Location location){
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        //dakle ukoliko je null znaci da je tek upalio aplikaciju i necu ni da sacuvavam dok se ne priblizi 500m nekom sektoru
        //kada se priblizi moram da aktiviram alarm jer mozda neko dolazi spolja
        //najblizi sektor je taj sektor, ostatak se desava u else
        if(currentNearestSector == null){
            LatLng nearestMiddlePoint = nearestMiddlePoint(currentLocation);
            if(distance(nearestMiddlePoint,currentLocation)>500){
                return false;
            }
            currentNearestSector = nearestMiddlePoint;
            alarmForSector(currentNearestSector, currentLocation);
            alarm = true;
            //pukne ako se otkomentarise ovo za alarme?!
            //metode su bile synchronized jer sam mislio da je veca verovatnoca da sve radi kako treba ali posto je handler runnable dolazilo mi nekada do
            //zaglavljivanja tredova, sklonio sam to i meni sada radi, probaj kod tebe na tvom pametnom telefonu huehe
            return false;
        }else{
            //ukoliko prosli najblizi sektor nije jednak sa sadasnjim najblizim sektorom znaci da je razdaljina izmedju osobe i srednje tacke tog drugog
            //sektora manja nego razdaljina od proslog sektora i treba da se aktivira alarm odnosno notifikacija odnosno firebase ukoliko je coek motorizovan
            if(!currentNearestSector.equals(nearestMiddlePoint(currentLocation))){
                currentNearestSector = nearestMiddlePoint(currentLocation); //novi sektor je sada najblizi
                alarmForSector(currentNearestSector, currentLocation);
                alarm=true;
                return true;
            }
        }
        currentNearestSector = nearestMiddlePoint(currentLocation); //novi sektor je sada najblizi
        if(alarm){ //alarm se jos nije oglasio? znaci ustanovili smo da se krece ka sektoru samo mu nije dosao dovoljno blizu, 20m konkretno
            alarmForSector(currentNearestSector, currentLocation);
        }else{
            ALARM = "NEMA OBAVESTENJA"; //ovo sam ja samo da bih testirao to cemo srediti
        }
        return false;
    }

    public static LatLng nearestMiddlePoint(LatLng currentLocation){
        LatLng currentNearestSector = middlePoints.get(0);
        float min = distance(currentNearestSector, currentLocation);
        for(int i=1; i<middlePoints.size(); i++) {
            if(distance(middlePoints.get(i), currentLocation)<min){
                min = distance(middlePoints.get(i), currentLocation);
                currentNearestSector=middlePoints.get(i);
            }
        }
        return currentNearestSector; //pronalazi najblizu centralnu tacku poligona
    }

    private static float distance(LatLng current, LatLng last){
        if(last==null)
            return 0;
        Location cL = new Location("");
        cL.setLatitude(current.latitude);
        cL.setLongitude(current.longitude);

        Location lL = new Location("");
        lL.setLatitude(last.latitude);
        lL.setLongitude(last.longitude);

        return lL.distanceTo(cL); //vraca distancu u metrima izmedju dve tacke
    }

    public static void alarmForSector(LatLng currentNearestSector, LatLng currentLocation){
        if(currentLocation != null && currentNearestSector != null) {
            int index = middlePoints.indexOf(currentNearestSector);
            List<LatLng> points = polygons.get(index).getPoints();
            LatLng min1 = points.get(0);
            float minDistance1 = distance(currentLocation, min1);
            LatLng min2 = points.get(1);
            float minDistance2 = distance(currentLocation, min2);
            for (int i = 2; i < points.size(); i++) {
                float distance = distance(points.get(i), currentLocation);
                if (distance < minDistance1 || distance < minDistance2) {
                    if (minDistance1 == minDistance2) {
                        minDistance1 = distance;
                        min1 = points.get(i); //svejedno jedan ili dva
                    } else {
                        if (minDistance1 < minDistance2) {
                            minDistance2 = distance;
                            min2 = points.get(i);
                        } else {
                            minDistance1 = distance;
                            min1 = points.get(i);
                        }
                    }
                }
            }
            //znaci nasao sam dve najblize tacke poligona, sad cu da nadjem i srednju, znas ti sta je LatLngBounds? znas ti kurac moj
            LatLngBounds bounds = new LatLngBounds(min1, min2);
            LatLng centerLatLng = bounds.getCenter();
            if (distance(centerLatLng, currentLocation) < 20.00 || distance(min1, currentLocation) < 20.00 || distance(min1, currentLocation) < 20.00) {
                //ako je 20m od najblize tacke najblizeg sektora (srednje ili dve za koje postoje koordinate) notifikacija i gasenje booleana
                //kako bi se sprecilo neprestano notifikovanje, nek kada se uoci da trenutni najblizi sektor nije isti kao prosli
                //alarm se pali opet i proverava se koliko smo udaljeni
                ALARM = "Priblizavate se sektoru" + index;
                alarm = false;
            } else {
                ALARM = "Nalazite se" + distance(centerLatLng, currentLocation) + "od sektora" + index;
                //kada ovo nestane pise ono nemate obavestenja i znaci dobrodosao u klub, sektor
            }
        }
    }
}
