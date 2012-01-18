/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.impl.protocol.sip;

import java.util.*;
import javax.sip.header.RouteHeader;
import javax.sip.message.Request;


/**
 *
 * @author Usuario
 */
public class RouteHeaderList {

    private Map<String, RouteHeader> routeList;

    public RouteHeaderList() {
        routeList = new HashMap<String,RouteHeader>();
    }

    public void addRoute(String key, RouteHeader s){
        routeList.put(key,s);
        if (key.equals(Request.REGISTER)) {
            routeList.put(Request.MESSAGE, s);
            routeList.put(Request.PUBLISH, s);
            routeList.put(Request.SUBSCRIBE, s);
        }
    }

    public RouteHeader getRoute(String key){
        try {
            return routeList.get(key);
        }catch(NoSuchElementException ex){
            return null;
        }
    }

}
