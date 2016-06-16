package fr.cs.regards.business.geolocation.postgis;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.business.geolocation.Circle;
import fr.cs.regards.business.geolocation.Coordinate;
import fr.cs.regards.business.geolocation.Distance.Unit;

public class PostgisCircleCriteria implements PostgisCriteria {
	private Circle circle;

	public PostgisCircleCriteria(Circle circle) {
		this.circle = circle;
	}

	@Override
	public Geometry toGeometry() {
		Geometry point = new Geometry(ogrConstants.wkbPoint);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4("+proj=longlat +a=3396190 +b=3376200 +no_defs  <>");
		point.AssignSpatialReference(targetRef);
		
		Coordinate center = circle.getCenter();
		point.AddPoint(center.getLon(), center.getLat());
		
		SpatialReference sourceRef = new SpatialReference();
		sourceRef.ImportFromProj4("+proj=tmerc +lat_0=0 +lon_0=0 +k=0.9996 +x_0=0 +y_0=0 +a=3396190 +b=3376200 +units=m +no_defs  <>");
		
		point.TransformTo(sourceRef);	

		float value = circle.getRadius().getValue(Unit.m);
		Geometry buffer = point.Buffer(value);
		buffer.TransformTo(targetRef);
		
		return buffer;
		
	}

}
