package fr.cs.regards.tools.elastic;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.GeometryCollection;
import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.geojson.MultiPoint;
import org.geojson.MultiPolygon;
import org.geojson.Point;
import org.geojson.Polygon;
import org.geojson.jackson.CrsType;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;



public class ElasticTestDataImporter {

	static final RestTemplate restTemplate = new RestTemplate();
	static final ObjectMapper mapper = new ObjectMapper();
	static final CRSFactory CRS_FACTORY = new CRSFactory();
	static final CoordinateReferenceSystem REF_CRS = CRS_FACTORY.createFromName("IAU2000:49901");

	public static void main(String[] args) throws URISyntaxException, IOException {
		saveDataInCache();
		createIndex("mars");
		final int[] cpt = { 0, 0, 0 };
		
		File dataCache = new File("test_data_set");
		File[] files = dataCache.listFiles();
		for (File file : files) {
			FeatureCollection featureCollection;
			try {
				featureCollection = mapper.readValue(file, FeatureCollection.class);
				String name = file.getName().split("\\.")[0];
				featureCollection.forEach(feature -> {
					feature.getProperties().clear();
					feature.setProperty("collection", name);
					feature.setProperty("number", cpt[0]);
					reprojFeature(featureCollection, feature);
					insertObject("mars", "Feature", cpt[1], feature);
					cpt[0] = cpt[0] + 1;
					cpt[1] = cpt[1] + 1;
				});
				featureCollection.setFeatures(Collections.emptyList());
				insertObject("mars", "FeatureCollection", cpt[2],
						new FeatureCollectionInfo(featureCollection, name));
				cpt[0] = 0;
				cpt[2] = cpt[2] + 1;
			} catch (Exception e) {
				Logger.getLogger("Main").warning("Unable to import "+file.getAbsolutePath());;
			}
		}
		

	}

	public static void reproj(LngLatAlt coord, CoordinateTransform transform) {

		ProjCoordinate src = new ProjCoordinate(coord.getLongitude(), coord.getLatitude(), coord.getAltitude());
		ProjCoordinate tgt = new ProjCoordinate();
		transform.transform(src, tgt);
		coord.setLatitude(tgt.y);
		coord.setLongitude(tgt.x);
		coord.setAltitude(tgt.z);
	}

	public static void reproj(Point p, CoordinateTransform transform) {
		reproj(p.getCoordinates(), transform);
	}

	public static void reproj(Polygon p, CoordinateTransform transform) {
		p.getCoordinates().forEach(list -> {
			list.forEach(lla -> {
				reproj(lla, transform);
			});
		});
	}

	public static void reproj(MultiPolygon p, CoordinateTransform transform) {
		p.getCoordinates().forEach(listOflist -> {
			listOflist.forEach(list -> {
				list.forEach(lla -> {
					reproj(lla, transform);
				});
			});
		});
	}

	public static void reproj(MultiPoint p, CoordinateTransform transform) {
		p.getCoordinates().forEach(lla -> {
			reproj(lla, transform);
		});
	}

	public static void reproj(MultiLineString p, CoordinateTransform transform) {
		p.getCoordinates().forEach(list -> {
			list.forEach(lla -> {
				reproj(lla, transform);
			});
		});
	}

	public static void reproj(GeoJsonObject geometry, CoordinateTransform transform) {
		if (geometry instanceof Point) {
			reproj((Point) geometry, transform);
		} else if (geometry instanceof Polygon) {
			reproj((Polygon) geometry, transform);
		} else if (geometry instanceof MultiPolygon) {
			reproj((MultiPolygon) geometry, transform);
		} else if (geometry instanceof MultiPoint) {
			reproj((MultiPoint) geometry, transform);
		} else if (geometry instanceof MultiLineString) {
			reproj((MultiLineString) geometry, transform);
		} else if (geometry instanceof GeometryCollection) {
			reproj((GeometryCollection) geometry, transform);
		}
	}

	public static void reproj(GeometryCollection g, CoordinateTransform transform) {
		g.forEach(geometry -> {
			reproj(geometry, transform);
		});
	}

	public static void reprojFeature(FeatureCollection featureCollection, Feature feature) {

		CoordinateReferenceSystem srcCRS = null;
		if (featureCollection.getCrs().getType() == CrsType.name) {
			Map<String, Object> properties = featureCollection.getCrs().getProperties();
			if (!properties.containsKey("name"))
				return;
			String crsName = normalizeCRSName((String) properties.get("name"));
			srcCRS = CRS_FACTORY.createFromName(crsName);

		} else {
			/// TODO
			return;
		}
		if (srcCRS == null)
			return;
		CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
		CoordinateTransform transform = transformFactory.createTransform(srcCRS, REF_CRS);

		GeoJsonObject geometry = feature.getGeometry();
		reproj(geometry, transform);

	}

	public static String normalizeCRSName(String crsName) {
		if (crsName.contains("::")) {
			String[] tokens = crsName.split("::");
			String refNumber = tokens[1];
			String auth = tokens[0];
			tokens = auth.split(":");
			auth = tokens[tokens.length - 1];
			return String.format("%s:%s", auth, refNumber);
		}
		return crsName;
	}

	public static void saveDataInCache() throws URISyntaxException, FileNotFoundException, IOException {

		File localCache = new File("test_data_set");
		if (localCache.isDirectory() && localCache.exists() && localCache.listFiles().length > 0) {
			return;
		}
		File file = new File(ElasticTestDataImporter.class.getResource("/test_data_urls.txt").toURI());
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Map<String, FeatureCollection> featureCollections = new HashMap<>();
		reader.lines().forEach(url -> {
			url = url.trim();
			if (!(url.startsWith("#") || url.isEmpty())) {
				featureCollections.put(readDataNameFromUrl(url), loadFeatureCollection(url));
			}

		});
		reader.close();

		featureCollections.forEach((name, featureCollection) -> {

			String fileName = String.format("test_data_set/%s.json", name);
			System.out.println("Start saving features " + fileName);
			File fcFile = new File(fileName);
			try {
				mapper.writeValue(fcFile, featureCollection);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static FeatureCollection loadFeatureCollection(final String url) {

		return restTemplate.getForObject(url, FeatureCollection.class);
	}

	public static boolean indexExists(String index) {
		try {
			restTemplate.headForHeaders(String.format("http://localhost:9200/%s/", index));
		} catch (RestClientException e) {
			return false;
		}
		return true;
	}

	public static <T> void insertObject(String index, String type, int id, T message) {
		String url = String.format("http://localhost:9200/%s/%s/%d/", index, type, id);

		// Set the Content-Type header
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(new MediaType("application", "json"));
		HttpEntity<T> requestEntity = new HttpEntity<T>(message, requestHeaders);

		// Create a new RestTemplate instance
		RestTemplate restTemplate = new RestTemplate();

		// Add the Jackson and String message converters
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

		// Make the HTTP PUT request, marshaling the request to JSON, and the
		// response to a String
		// ignore response for the moment
		restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);

	}

	public static String readDataNameFromUrl(String url) {
		String name = "";
		try {
			if (url.startsWith("http://psup.ias.u-psud.fr")) {
				String[] tokens = url.split("/");
				name = tokens[tokens.length - 1].split("\\.")[0];
			} else if (url.startsWith("http://emars.univ-lyon1.fr")) {
				String[] tokens = url.split("&");
				name = tokens[3].split(":")[1];
			}
		} catch (Exception e) {
			return name;
		}
		return name;

	}

	public static void createIndex(String index) {
		if (!indexExists(index)) {
			restTemplate.put(String.format("http://localhost:9200/%s/", index), null);
		}
	}

}
