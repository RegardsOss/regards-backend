package fr.cs.regards.geolocation.elastic;

import java.util.List;

import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.Line;

public class ElasticLineCriteria implements ElasticCriteria{

	private GeolocationCriteria<Line> criteria = null;
	public ElasticLineCriteria(GeolocationCriteria<Line> criteria) {
		this.criteria = criteria;
	}
	private static final String LINE_CRITERIA_TPL = "\"type\":\"linestring\",\"coordinates\":%s";
	@Override
	public String toJSON() {
		StringBuilder sb = new StringBuilder("[");
		
		List<Coordinate> coordinates = criteria.getShape().getCoordinates();
		coordinates.forEach(coord -> {
			sb.append(coord).append(",");
		});
		sb.replace(sb.length()-1, sb.length(), "]");
		
		
		return String.format(LINE_CRITERIA_TPL,sb);
	}
	
}
