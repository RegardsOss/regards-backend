package fr.cs.regards.business.geolocation.postgis;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.business.geolocation.Coordinate;
import fr.cs.regards.business.geolocation.Polygon;

public class PostgisPolygonCriteria implements PostgisCriteria {
	private Polygon polygon;

	public PostgisPolygonCriteria(Polygon polygon) {
		this.polygon = polygon;
	}

	@Override
	public Geometry toGeometry() {

		Geometry ring = new Geometry(ogrConstants.wkbLinearRing);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4("+proj=longlat +a=3396190 +b=3376200 +no_defs  <>");
		ring.AssignSpatialReference(targetRef);
		Coordinate first = polygon.getCoordinates().get(0);
		polygon.getCoordinates().forEach(c -> {
			ring.AddPoint(c.getLon(), c.getLat());
		});
		ring.AddPoint(first.getLon(), first.getLat());

		Geometry poly = new Geometry(ogrConstants.wkbPolygon);
		poly.AssignSpatialReference(targetRef);
		poly.AddGeometry(ring);
		return poly;
		
	}

}
