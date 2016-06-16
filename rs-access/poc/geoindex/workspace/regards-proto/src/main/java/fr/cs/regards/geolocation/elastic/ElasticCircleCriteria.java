package fr.cs.regards.geolocation.elastic;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.GeolocationCriteria;

public class ElasticCircleCriteria implements ElasticCriteria {

	
	private GeolocationCriteria<Circle> criteria = null;
	public ElasticCircleCriteria(GeolocationCriteria<Circle> criteria) {
		this.criteria = criteria;
	}
	private static final String CIRCLE_CRITERIA_TPL = "\"type\":\"circle\",\"coordinates\":%s,\"radius\":\"%s\"";
	@Override
	public String toJSON() {
		return String.format(CIRCLE_CRITERIA_TPL,
				criteria.getShape().getCenter(),
				criteria.getShape().getRadius());
	}

}
