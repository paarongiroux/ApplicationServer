package appserver.server;

import java.util.ArrayList;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class LoadManager {

    static ArrayList<String> satellites = null;
    static int lastSatelliteIndex = -1;

    public LoadManager() {
        satellites = new ArrayList<String>();
    }

    public void satelliteAdded(String satelliteName) {
        // add satellite
        satellites.add(satelliteName);
    }


    public String nextSatellite() throws Exception {
        
        int numberSatellites = satellites.size();
        
        synchronized (satellites) {
            // increment last Satellite Index to get the next satellite.
            lastSatelliteIndex ++;
            // if lastSatelliteIndex is over the arrayList size, reset to zero.
            if (lastSatelliteIndex >= numberSatellites)
            {
                lastSatelliteIndex = 0;
            }
        }
        
        // return string at the current index.
        return satellites.get(lastSatelliteIndex);
    }
}
