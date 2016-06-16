package fr.cs.regards.geolocation.elastic.gdal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

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
import fr.cs.regards.tools.geojson.GeoJSONTools;

public class ElasticGeolacationServicePointTests {
	private static final String POINT_ID = "testPoint";
	
	private static GeolocationService geolocService  = new GDALGeolacationService("http://localhost:9200",  "test_point");

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
		assertTrue(POINT_ID.equals(features.iterator().next().getId()));
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
		assertTrue(POINT_ID.equals(features.iterator().next().getId()));
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
		assertTrue(POINT_ID.equals(features.iterator().next().getId()));
	}

	public static void main(String[] args) throws JsonProcessingException {
		org.geojson.Point polygon = new org.geojson.Point(new LngLatAlt(0, 0));
		GDALUtils.insertObject("geomars", "test_point", 0, GeoJSONTools.createFeature(POINT_ID, polygon, "uai2000:49901"));
	}

}
