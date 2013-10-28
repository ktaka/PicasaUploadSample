/**
 * 
 */
package org.ktaka.api.services.picasa.model;

import com.google.api.client.util.Key;

/**
 * @author ktaka
 *
 */
public class GmlPoint {

	@Key("gml:pos")
	public String gmlPos;

	public static GmlPoint createLatLon(double lat, double lon) {
		GmlPoint point = new GmlPoint();
		point.gmlPos = lat + " " + lon;
		return point;
	}

}
