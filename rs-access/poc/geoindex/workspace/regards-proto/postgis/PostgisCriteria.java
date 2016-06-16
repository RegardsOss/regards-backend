package fr.cs.regards.business.geolocation.postgis;

import org.gdal.ogr.Geometry;

public interface PostgisCriteria {
	Geometry toGeometry();

}
