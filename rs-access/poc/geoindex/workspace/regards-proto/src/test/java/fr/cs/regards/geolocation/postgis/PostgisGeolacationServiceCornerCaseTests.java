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

import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.GeolocationService;
import fr.cs.regards.geolocation.Polygon;
import fr.cs.regards.geolocation.gdal.GDALGeolacationService;
import fr.cs.regards.geolocation.gdal.GDALUtils;
import fr.cs.regards.tools.geojson.GeoJSONTools;

public class PostgisGeolacationServiceCornerCaseTests {
	private static String TEST_INDEX = "test_corner_case";
	
	private static String PG_CONN_TPL = "PG: dbname=%s host=%s port=%d user=%s password=%s";
	private static String PG_CONN = String.format(PG_CONN_TPL, "geomars", "localhost", 5432, "regards", "regards");
	private static GeolocationService geolocService  = new GDALGeolacationService(PG_CONN, TEST_INDEX);

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
		// ElasticUtils.restTemplate.delete(INDEX);
	}

	@Test
	public void testNorthPole() throws Exception {
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(-10, 80);
			Coordinate cp2 = new Coordinate(10, 80);
			Coordinate cp3 = new Coordinate(10, 90);
			Coordinate cp4 = new Coordinate(-10, 90);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			assertTrue(features.iterator().next().getId().equals("NORTH_POLE"));
		}
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(0, 80);
			Coordinate cp2 = new Coordinate(90, 80);
			Coordinate cp3 = new Coordinate(180, 81);
			Coordinate cp4 = new Coordinate(-90, 81);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			assertTrue(features.iterator().next().getId().equals("NORTH_POLE"));
		}
	}

	@Test
	public void testSouthPole() throws Exception {
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(-10, -80);
			Coordinate cp2 = new Coordinate(10, -80);
			Coordinate cp3 = new Coordinate(10, -90);
			Coordinate cp4 = new Coordinate(-10, -90);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			assertTrue(features.iterator().next().getId().equals("SOUTH_POLE"));
		}
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(-90, -80);
			Coordinate cp2 = new Coordinate(0, -80);
			Coordinate cp3 = new Coordinate(90, -80);
			Coordinate cp4 = new Coordinate(180, -80);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			assertTrue(features.iterator().next().getId().equals("SOUTH_POLE"));
		}
	}

	@Test
	public void testEstAndWestPole() throws Exception {
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(-180, 10);
			Coordinate cp2 = new Coordinate(-170, 10);
			Coordinate cp3 = new Coordinate(-170, -10);
			Coordinate cp4 = new Coordinate(-180, -10);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			Iterator<Feature> it = features.iterator();
			String id1 = it.next().getId();

			List<String> ids = Arrays.asList(id1);
			assertTrue(ids.contains("WEST_POLE"));
		}
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(180, 10);
			Coordinate cp2 = new Coordinate(170, 10);
			Coordinate cp3 = new Coordinate(170, -10);
			Coordinate cp4 = new Coordinate(180, -10);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(1, features.size());
			Iterator<Feature> it = features.iterator();
			String id1 = it.next().getId();

			List<String> ids = Arrays.asList(id1);
			assertTrue(ids.contains("EAST_POLE"));
		}
		{
			Polygon cpoly = new Polygon();
			Coordinate cp1 = new Coordinate(170, 10);
			Coordinate cp2 = new Coordinate(-170, 10);
			Coordinate cp3 = new Coordinate(-170, -10);
			Coordinate cp4 = new Coordinate(170, -10);
			cpoly.setCoordinates(Arrays.asList(cp1, cp2, cp3, cp4));
			GeolocationCriteria<Polygon> crit1 = new GeolocationCriteria<>();
			crit1.setShape(cpoly);

			Collection<Feature> features = geolocService.select(crit1);
			assertEquals(2, features.size());
			Iterator<Feature> it = features.iterator();
			String id1 = it.next().getId();
			String id2 = it.next().getId();

			List<String> ids = Arrays.asList(id1, id2);
			assertTrue(ids.contains("WEST_POLE"));
			assertTrue(ids.contains("EAST_POLE"));
		}

	}

	//
	// @Test
	// public void testPointIncludedInResearchCircle() throws Exception {
	// Circle researchCircle = new Circle();
	// Coordinate center = new Coordinate(0, 1);
	// Distance radius = new Distance(200, Unit.km);
	// researchCircle.setCenter(center);
	// researchCircle.setRadias(radius);
	// GeolocationCriteria<Circle> crit = new
	// GeolocationCriteria<Circle>(researchCircle);
	//
	// Collection<Feature> features = geolocService.select(crit);
	// assertEquals(1, features.size());
	// assertTrue(POINT_ID.equals(features.iterator().next().getId()));
	// }

	public static void main(String[] args) throws JsonProcessingException {

		org.geojson.Point northPole = new org.geojson.Point(new LngLatAlt(0, 90));
		GDALUtils.insertObject("geomars", TEST_INDEX, 0, GeoJSONTools.createFeature("NORTH_POLE", northPole, "uai2000:49901"));

		org.geojson.Point southPole = new org.geojson.Point(new LngLatAlt(0, -90));
		GDALUtils.insertObject("geomars", TEST_INDEX,1, GeoJSONTools.createFeature("SOUTH_POLE", southPole, "uai2000:49901"));

		org.geojson.Point westPole = new org.geojson.Point(new LngLatAlt(-180, 0));
		GDALUtils.insertObject("geomars", TEST_INDEX, 2, GeoJSONTools.createFeature("WEST_POLE", westPole, "uai2000:49901"));

		org.geojson.Point eastPole = new org.geojson.Point(new LngLatAlt(180, 0));
		GDALUtils.insertObject("geomars", TEST_INDEX, 3, GeoJSONTools.createFeature("EAST_POLE", eastPole, "uai2000:49901"));
	}

}
