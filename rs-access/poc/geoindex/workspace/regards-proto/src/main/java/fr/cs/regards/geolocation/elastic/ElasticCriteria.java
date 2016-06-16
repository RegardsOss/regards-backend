package fr.cs.regards.geolocation.elastic;



import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.Line;
import fr.cs.regards.geolocation.Polygon;

public interface ElasticCriteria {
	String toJSON();
	
	@SuppressWarnings("unchecked")
	static ElasticCriteria convert(GeolocationCriteria<?> criteria){
		Object objShape = criteria.getShape();
		if(objShape instanceof Circle){
			return new ElasticCircleCriteria((GeolocationCriteria<Circle>) criteria);
		}
		else if(objShape instanceof Polygon){
			return new ElasticPolygonCriteria((GeolocationCriteria<Polygon>) criteria);
		}
		else if(objShape instanceof Line){
			return new ElasticLineCriteria((GeolocationCriteria<Line>) criteria);
		}
		return null;
	}
}
