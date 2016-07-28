package fr.cs.regards.geolocation.postgis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.Distance;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.GeolocationService;
import fr.cs.regards.geolocation.Polygon;
import fr.cs.regards.geolocation.Distance.Unit;
import fr.cs.regards.geolocation.gdal.GDALGeolacationService;
import fr.cs.regards.geolocation.gdal.GDALUtils;
import fr.cs.regards.tools.elastic.ElasticUtils;
import fr.cs.regards.tools.geojson.GeoJSONTools;

public class PostgisGeolacationServicePointTests {
	private static final String POINT_ID = "testPoint";
	
	private static String PG_CONN_TPL = "PG: dbname=%s host=%s port=%d user=%s password=%s";
	private static String PG_CONN = String.format(PG_CONN_TPL, "geomars", "localhost", 5432, "regards", "regards");
	private static GeolocationService geolocService  = new GDALGeolacationService(PG_CONN, "test_point");

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
		// ElasticUtils.restTemplate.delete(INDEX);
	}

	@Test
	public void testResearchPolygonDisjoiningPoint() throws Exception {
		Polygon cpoly = new Polygon();
		Coordinate cp1 = new Coordinate(5, 5);
		Coordinate cp2 = new Coordinate(20, 5);
		Coordinate cp3 = new Coordinate(20, 20);
		Coordinate cp4 = new Coordinate(5, 20);
		cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
		crit1.setShape(cpoly);

		Collection<Feature> features = geolocService.select(crit1);
		assertEquals(0, features.size());
	}

	

	@Test
	public void testPointIncludedInResearchPolygon() throws Exception {
		Polygon researchPoly = new Polygon();
		Coordinate cp1 = new Coordinate(-10, -10);
		Coordinate cp2 = new Coordinate(30, -10);
		Coordinate cp3 = new Coordinate(30, 30);
		Coordinate cp4 = new Coordinate(-10, 30);
		researchPoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
		crit1.setShape(researchPoly);

		Collection<Feature> features = geolocService.select(crit1);
		assertEquals(1, features.size());
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("0")));
	}

	@Test
	public void testResearchCircleDisjoiningPoint() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(30, 10);
		Distance radius = new Distance(10, Unit.m);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);

		Collection<Feature> features = geolocService.select(crit);
		assertEquals(0, features.size());
	}


	@Test
	public void testPointIncludedInResearchCircle() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(0, 1);
		Distance radius = new Distance(200, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);

		Collection<Feature> features = geolocService.select(crit);
		assertEquals(1, features.size());
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("0")));
	}
	
	@Test
	public void testPointOutsideInResearchCircle() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(0, 5);
		Distance radius = new Distance(20, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);

		Collection<Feature> features = geolocService.select(crit);
		assertEquals(0, features.size());
	}	
	
	@Test
	public void testPointInsideConcavePolyHole() throws Exception {
		Polygon researchPoly = new Polygon();
		Coordinate cp1 = new Coordinate(-20, 20);
		Coordinate cp2 = new Coordinate(-10, 20);
		Coordinate cp3 = new Coordinate(-10, -10);
		Coordinate cp4 = new Coordinate(10, -10);
		Coordinate cp5 = new Coordinate(10, 10);
		Coordinate cp6 = new Coordinate(20, 10);
		Coordinate cp7 = new Coordinate(20, -20);
		Coordinate cp8 = new Coordinate(-20, -20);
		researchPoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4, cp5, cp6, cp7, cp8));
		GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
		crit1.setShape(researchPoly);

		Collection<Feature> features = geolocService.select(crit1);
		System.out.println("-> "+features.iterator().next().getId());
		assertEquals(0, features.size());
	}
	
	@Test
	public void testPointInsideConcavePoly() throws Exception {
		Polygon researchPoly = new Polygon();
		Coordinate cp1 = new Coordinate(-20, 35);
		Coordinate cp2 = new Coordinate(-10, 35);
		Coordinate cp3 = new Coordinate(-10, 5);
		Coordinate cp4 = new Coordinate(10, 5);
		Coordinate cp5 = new Coordinate(10, 25);
		Coordinate cp6 = new Coordinate(20, 25);
		Coordinate cp7 = new Coordinate(20, -5);
		Coordinate cp8 = new Coordinate(-20, -5);
		researchPoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4, cp5, cp6, cp7, cp8));
		GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
		crit1.setShape(researchPoly);

		Collection<Feature> features = geolocService.select(crit1);
		assertEquals(1, features.size());
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("0")));
	}
	
	@Test
	public void testPointIncludedInResearchCircleIntoLat90N() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(0,90);
		Distance radius = new Distance(1, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);

		Collection<Feature> features = geolocService.select(crit);
		assertEquals(2, features.size());
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("1","2")));
	}

	@Test
	public void testPointIncludedInResearchCircleIntoLat90S() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(0,-90);
		Distance radius = new Distance(1, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);

		Collection<Feature> features = geolocService.select(crit);
		assertEquals(2, features.size());
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("3","4")));
	}

	@Test
	public void testPointIncludedInResearchCircleIntoLon180W() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(-180,45);
		Distance radius = new Distance(1, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		
		Collection<Feature> features = geolocService.select(crit);
		System.out.println(features.size() + " objets trouvés");
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("5","7")));
		assertEquals(2, features.size());
	}
	@Test
	public void testPointIncludedInResearchCircleIntoLon180E() throws Exception {
		Circle researchCircle = new Circle();
		Coordinate center = new Coordinate(180,45);
		Distance radius = new Distance(1, Unit.km);
		researchCircle.setCenter(center);
		researchCircle.setRadias(radius);
		GeolocationCriteria<Circle> crit = new GeolocationCriteria<Circle>(researchCircle);
		
		Collection<Feature> features = geolocService.select(crit);
		System.out.println(features.size() + " objets trouvés");
		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("5","7")));
		assertEquals(2, features.size());
	}
	/**
	 * Check if id's results are the same as expected
	 * @param features result of search
	 * @param list ids to check
	 * @return
	 */
	private boolean getIdInResults(Collection<Feature> features,List<String> list) {
		// First check : same size !
		if (features.size() != list.size() ) {
			return false;
		}
		
		boolean result = true;
		String id = null;
		Iterator<Feature> iterator = features.iterator();
		
		while(iterator.hasNext()) {
			id = iterator.next().getId();
			System.out.println("Check "+id);
			result = result && list.contains(id);
		}
		
		return result;
	}
	
	@Test
	public void testPointInsideConcaveResearch() throws Exception {
		Polygon researchPoly = new Polygon();
		Coordinate cp1 = new Coordinate(-170,60);
		Coordinate cp2 = new Coordinate(170,60);
		Coordinate cp3 = new Coordinate(170,-30);
		Coordinate cp4 = new Coordinate(-170,30);
		researchPoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
		GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
		crit1.setShape(researchPoly);

		Collection<Feature> features = geolocService.select(crit1);

		assertEquals(true,getIdInResults(features,(List<String>) Arrays.asList("5","7")));
		assertEquals(2, features.size());
	}



	public static void main(String[] args) throws JsonProcessingException {
		System.out.println(PG_CONN);
		org.geojson.Point point = null;
		
		point = new org.geojson.Point(new LngLatAlt(0, 0));
		GDALUtils.insertObject("geomars", "test_point", 0, GeoJSONTools.createFeature("0", point, "iau2000:49901"));
		point = new org.geojson.Point(new LngLatAlt(-90,90));
		GDALUtils.insertObject("geomars", "test_point", 1, GeoJSONTools.createFeature("1", point, "iau2000:49901"));
		point = new org.geojson.Point(new LngLatAlt(90, 90));
		GDALUtils.insertObject("geomars", "test_point", 2, GeoJSONTools.createFeature("2", point, "iau2000:49901"));


		point = new org.geojson.Point(new LngLatAlt(90,-90));
		GDALUtils.insertObject("geomars", "test_point", 3, GeoJSONTools.createFeature("3", point, "iau2000:49901"));
		point = new org.geojson.Point(new LngLatAlt(-90,-90));
		GDALUtils.insertObject("geomars", "test_point", 4, GeoJSONTools.createFeature("4", point, "iau2000:49901"));

		// Points sur la longitude -180
		point = new org.geojson.Point(new LngLatAlt(-180,45));
		GDALUtils.insertObject("geomars", "test_point", 5, GeoJSONTools.createFeature("5", point, "iau2000:49901"));

		// Points sur la longitude +180
		point = new org.geojson.Point(new LngLatAlt(180,45));
		GDALUtils.insertObject("geomars", "test_point", 7, GeoJSONTools.createFeature("7", point, "iau2000:49901"));

		// Points dans la recheche convexe
		point = new org.geojson.Point(new LngLatAlt(120,10));
		GDALUtils.insertObject("geomars", "test_point", 8, GeoJSONTools.createFeature("8", point, "iau2000:49901"));

	
	}

}
