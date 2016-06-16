package fr.cs.regards.geolocation.elastic;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.Distance;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.Line;
import fr.cs.regards.geolocation.Polygon;
import fr.cs.regards.geolocation.Distance.Unit;
import fr.cs.regards.geolocation.elastic.ElasticGeolacationService;
import fr.cs.regards.tools.elastic.ElasticUtils;
import fr.cs.regards.tools.geojson.GeoJSONTools;

public class ElasticGeolacationServicePolygonTests {
	private static final String POLYGON_ID = "testPolygon";
	private static String BASE_URL = "http://localhost:9200";
	private static String TEST_INDEX = "test_polygon";
	static String INDEX = String.format("%s/%s/", BASE_URL, TEST_INDEX);
	private static ElasticGeolacationService geolocService = new ElasticGeolacationService(INDEX+"/_search");
	
	
	

	@Before
	public void setUp() throws Exception {
		
		
	}
	
	@After
	public void tearDown() throws Exception{
		//ElasticUtils.restTemplate.delete(INDEX);
	}

	@Test
	public void testResearchPolygonDisjoiningPolygon() throws Exception {
		Polygon cpoly = new Polygon();
		 Coordinate cp1 = new Coordinate(0 + 30, 0); 
		 Coordinate cp2 = new Coordinate(20 + 30, 0);
		 Coordinate cp3 = new Coordinate(20 + 30, 20);
		 Coordinate cp4 = new Coordinate(0 + 30, 20);
		 cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		 GeolocationCriteria<Polygon> crit1= new GeolocationCriteria<>();
		 crit1.setShape(cpoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(0, features.size());
	}
	@Test
	public void testResearchPolygonIncludedInPolygon() throws Exception {
		Polygon cpoly = new Polygon();
		 Coordinate cp1 = new Coordinate(5, 5); 
		 Coordinate cp2 = new Coordinate(15, 5);
		 Coordinate cp3 = new Coordinate(15, 15);
		 Coordinate cp4 = new Coordinate(5, 15);
		 cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		 GeolocationCriteria<Polygon> crit1= new GeolocationCriteria<>();
		 crit1.setShape(cpoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	@Test
	public void testResearchPolygonIntersectingPolygon() throws Exception {
		Polygon cpoly = new Polygon();
		 Coordinate cp1 = new Coordinate(0 + 19, 0); 
		 Coordinate cp2 = new Coordinate(20 + 19, 0);
		 Coordinate cp3 = new Coordinate(20 + 19, 20);
		 Coordinate cp4 = new Coordinate(0 +19, 20);
		 cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		 GeolocationCriteria<Polygon> crit1= new GeolocationCriteria<>();
		 crit1.setShape(cpoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	@Test
	public void testPolygonIncludedInResearchPolygon() throws Exception {
		Polygon researchPoly = new Polygon();
		 Coordinate cp1 = new Coordinate(-10, -10); 
		 Coordinate cp2 = new Coordinate(30, -10);
		 Coordinate cp3 = new Coordinate(30, 30);
		 Coordinate cp4 = new Coordinate(-10, 30);
		 researchPoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		 GeolocationCriteria<Polygon> crit1= new GeolocationCriteria<>();
		 crit1.setShape(researchPoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	
	@Test
	public void testResearchCircleDisjoiningPolygon() throws Exception {
		 Circle researchCircle = new Circle();
		 Coordinate center = new Coordinate(30,10); 
		 Distance radius = new Distance(10, Unit.m);
		 researchCircle.setCenter(center);
		 researchCircle.setRadias(radius);
		 GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		 
		 Collection<Feature> features = geolocService.select(crit);
		 assertEquals(0, features.size());
	}
	@Test
	public void testResearchCircleIncludedInPolygon() throws Exception {
		 Circle researchCircle = new Circle();
		 Coordinate center = new Coordinate(10, 10); 
		 Distance radius = new Distance(5000, Unit.km);
		 researchCircle.setCenter(center);
		 researchCircle.setRadias(radius);
		 GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		 
		 Collection<Feature> features = geolocService.select(crit);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	@Test
	public void testResearchPolygonIntersectingCircle() throws Exception {
		 Circle researchCircle = new Circle();
		 Coordinate center = new Coordinate(0, 0); 
		 Distance radius = new Distance(100, Unit.m);
		 researchCircle.setCenter(center);
		 researchCircle.setRadias(radius);
		 GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		 
		 Collection<Feature> features = geolocService.select(crit);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	@Test
	public void testPolygonIncludedInResearchCircle() throws Exception {
		 Circle researchCircle = new Circle();
		 Coordinate center = new Coordinate(10, 10); 
		 Distance radius = new Distance(100, Unit.m);
		 researchCircle.setCenter(center);
		 researchCircle.setRadias(radius);
		 GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		 
		 Collection<Feature> features = geolocService.select(crit);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	
	@Test
	public void testResearchLineCrossingPolygon() throws Exception {
		 Line cpoly = new Line();
		 Coordinate cp1 = new Coordinate(-10, 10); 
		 Coordinate cp2 = new Coordinate(30, 10);
		 Coordinate cp3 = new Coordinate(30, 10);
		 Coordinate cp4 = new Coordinate(-10, 10 ); 
		 cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		 GeolocationCriteria<Line> crit1= new GeolocationCriteria<>();
		 crit1.setShape(cpoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(1, features.size());
		 assertTrue(POLYGON_ID.equals(features.iterator().next().getId()));
	}
	
	@Test
	public void testResearchLineNotCrossingPolygon() throws Exception {
		 Line cpoly = new Line();
		 Coordinate cp1 = new Coordinate(30, -10); 
		 Coordinate cp2 = new Coordinate(30, 0);
		 Coordinate cp3 = new Coordinate(30, 10);
		 cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3));
		 GeolocationCriteria<Line> crit1= new GeolocationCriteria<>();
		 crit1.setShape(cpoly);
		 
		 Collection<Feature> features = geolocService.select(crit1);
		 assertEquals(0, features.size());
	}
	
	public static void main(String[] args) {
		String mapping = "{\"mappings\":{\"feature\":{\"properties\":{\"geometry\":{\"type\":\"geo_shape\"}}}}}";
		ElasticUtils.put(INDEX, mapping);
		
		 org.geojson.Polygon polygon = new org.geojson.Polygon();
		 LngLatAlt p1 = new LngLatAlt(0, 0);
		 LngLatAlt p2 = new LngLatAlt(20, 0);
		 LngLatAlt p3 = new LngLatAlt(20, 20);
		 LngLatAlt p4 = new LngLatAlt(0, 20);
		 List<LngLatAlt> points =Arrays.asList(p1, p2, p3, p4, p1);
		 polygon.add(points);
		 
		ElasticUtils.insertObject(TEST_INDEX, "feature", 0, GeoJSONTools.createFeature(POLYGON_ID, polygon, "uai2000:49901") );
	}

}
