package fr.cs.regards.geolocation.elastic;

import java.util.List;

import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.Polygon;

public class ElasticPolygonCriteria implements ElasticCriteria{

	private GeolocationCriteria<Polygon> criteria = null;
	public ElasticPolygonCriteria(GeolocationCriteria<Polygon> criteria) {
		this.criteria = criteria;
	}
	private static final String POLYGON_CRITERIA_TPL = "\"type\":\"polygon\",\"coordinates\":[%s]";
	@Override
	public String toJSON() {
		StringBuilder sb = new StringBuilder("[");
		
		List<Coordinate> coordinates = criteria.getShape().getCoordinates();
		coordinates.forEach(coord -> {
			sb.append(coord).append(",");
		});
		sb.append(coordinates.get(0)).append("]");
		
		
		return String.format(POLYGON_CRITERIA_TPL,sb);
	}
	
}
