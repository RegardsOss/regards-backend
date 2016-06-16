package fr.cs.regards.geolocation.gdal;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.Polygon;

public class GDALPolygonCriteria implements GDALCriteria {
	private Polygon polygon;

	public GDALPolygonCriteria(Polygon polygon) {
		this.polygon = polygon;
	}

	@Override
	public Geometry toGeometry() {

		Geometry ring = new Geometry(ogrConstants.wkbLinearRing);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4(GDALMarsProj.LATLON);
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
