package fr.cs.regards.business.geolocation.postgis;

import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.gdal.osr.SpatialReference;

import fr.cs.regards.business.geolocation.Line;

public class PostgisLineCriteria implements PostgisCriteria {
	private Line line;

	public PostgisLineCriteria(Line line) {
		this.line = line;
	}

	@Override
	public Geometry toGeometry() {
		Geometry lineString = new Geometry(ogrConstants.wkbLineString);
		SpatialReference targetRef = new SpatialReference();
		targetRef.ImportFromProj4("+proj=longlat +a=3396190 +b=3376200 +no_defs  <>");
		lineString.AssignSpatialReference(targetRef);
		line.getCoordinates().forEach(c -> {
			lineString.AddPoint(c.getLon(), c.getLat());
		});
		
		return lineString;
	
		
	}

}
