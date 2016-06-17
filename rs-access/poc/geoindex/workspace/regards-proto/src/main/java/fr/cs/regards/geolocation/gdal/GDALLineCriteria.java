package fr.cs.regards.geolocation.gdal;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.geolocation.Line;

public class GDALLineCriteria implements GDALCriteria {
	private Line line;

	public GDALLineCriteria(Line line) {
		this.line = line;
	}

	@Override
	public Geometry toGeometry() {
		Geometry lineString = new Geometry(ogrConstants.wkbLineString);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4(GDALMarsProj.LATLON);
		lineString.AssignSpatialReference(targetRef);
		line.getCoordinates().forEach(c -> {
			lineString.AddPoint(c.getLon(), c.getLat());
		});
		
		return lineString;
	
		
	}

}
