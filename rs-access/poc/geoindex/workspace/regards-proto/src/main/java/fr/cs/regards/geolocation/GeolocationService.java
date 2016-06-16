package fr.cs.regards.geolocation;

import java.util.Collection;

import org.geojson.Feature;

public interface GeolocationService {
	
	void insertFeature(Feature feature) throws Exception;
	
	Collection<Feature> select(GeolocationCriteria<? extends Shape> filters) throws Exception;
	
}
