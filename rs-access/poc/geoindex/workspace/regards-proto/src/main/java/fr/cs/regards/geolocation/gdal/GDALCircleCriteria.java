package fr.cs.regards.geolocation.gdal;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.Distance.Unit;

public class GDALCircleCriteria implements GDALCriteria {
	private Circle circle;

	public GDALCircleCriteria(Circle circle) {
		this.circle = circle;
	}

	@Override
	public Geometry toGeometry() {
		Geometry point = new Geometry(ogrConstants.wkbPoint);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4(GDALMarsProj.LATLON);
		point.AssignSpatialReference(targetRef);
		
		
		Coordinate center = circle.getCenter();
		point.AddPoint(center.getLon(), center.getLat());
		
		SpatialReference sourceRef = new SpatialReference();
		sourceRef.ImportFromProj4(GDALMarsProj.MERCATOR);
		
		point.TransformTo(sourceRef);	

		float value = circle.getRadius().getValue(Unit.m);
		Geometry buffer = point.Buffer(value);
		buffer.TransformTo(targetRef);
		
		return buffer;
		
	}

}
