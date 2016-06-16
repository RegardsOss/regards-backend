package fr.cs.regards.tools.geojson;

import java.util.Arrays;
import java.util.List;

import org.geojson.Crs;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.jackson.CrsType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cs.regards.tools.elastic.ElasticUtils;

public  abstract class GeoJSONTools {
	public static Feature createFeature(String id, GeoJsonObject geometry, String srsName){
		Feature feature = new Feature();
		Crs crs = new Crs();
		crs.setType(CrsType.name);
		crs.getProperties().put("name", srsName);
		feature.setCrs(crs);
		feature.setGeometry(geometry);
		feature.setId(id);
		return feature;
	}
	
	
	public static void main(String[] args) throws JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		org.geojson.Point point = new org.geojson.Point(new LngLatAlt(0, 90));
		Feature pointFeature = createFeature("Point", point, "uai2000:49901");
		
		 org.geojson.Polygon polygon = new org.geojson.Polygon();
		 LngLatAlt p1 = new LngLatAlt(0, 0);
		 LngLatAlt p2 = new LngLatAlt(20, 0);
		 LngLatAlt p3 = new LngLatAlt(20, 20);
		 LngLatAlt p4 = new LngLatAlt(0, 20);
		 List<LngLatAlt> points =Arrays.asList(p1, p2, p3, p4, p1);
		 polygon.add(points);
		 
		Feature polygonFeature = GeoJSONTools.createFeature("polygon", polygon, "uai2000:49901") ;
		
		System.out.println(mapper.writeValueAsString(polygonFeature));
		System.out.println(mapper.writeValueAsString(pointFeature));
	}


}
