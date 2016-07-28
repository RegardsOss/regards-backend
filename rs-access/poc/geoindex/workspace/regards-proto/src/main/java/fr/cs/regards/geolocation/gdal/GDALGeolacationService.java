package fr.cs.regards.geolocation.gdal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.GeolocationService;
import fr.cs.regards.geolocation.Line;
import fr.cs.regards.geolocation.Polygon;
import fr.cs.regards.geolocation.Shape;

public class GDALGeolacationService implements GeolocationService {
	private final Layer layer;
	private final DataSource dataSource;
	private final ObjectMapper mapper = new ObjectMapper();
	private static boolean OGR__NOT_REGISTERED = true;
	
	

	public GDALGeolacationService(String dataSourceName, String layerName) {
		System.out.println(dataSourceName);
		if(OGR__NOT_REGISTERED){
			ogr.RegisterAll();
			OGR__NOT_REGISTERED = false;
		}
		int nb = ogr.GetDriverCount();
		for (int i=0;i<nb;i++) {
			Driver myDriver = ogr.GetDriver(i);
			System.out.println("num "+i+" : "+myDriver.getName());
		}
		
		dataSource = ogr.Open(dataSourceName, 1);
		System.out.println("data source name = "+dataSourceName);
		if(dataSource == null){
			throw new RuntimeException("exc:"+gdal.GetLastErrorMsg());
//			Driver driver = ogr.GetDriverByName("ElasticSearch");
//			dataSource = driver.CreateDataSource(dataSourceName);
		}
		System.out.println(dataSource.getName());
		Layer existingLayer = dataSource.GetLayer(layerName);
		
		if (existingLayer == null) {
//			Vector options = new Vector();
//			options.add("ES_WRITEMAP=C:\\Users\\admin\\Desktop\\projets\\REGARDS\\workspace\\proto-business\\mapping.json");
//		
			layer = dataSource.CreateLayer(layerName);
			FeatureDefn schema = layer.GetLayerDefn();
			addField(layer, schema, "fid", "");
		}
		else{
			layer = existingLayer;
		}

	}
	
	private static void addField(Layer layer, FeatureDefn schema, String k, Object v) {
		if (schema.GetFieldIndex(k) >= 0)
			return;
		if (v instanceof String) {
			 layer.CreateField(new FieldDefn(k, ogrConstants.OFTString));
		}
		// TODO TO BE CONTINUED see ogr.ogrConstants.OFT...

	}

	private Collection<Feature> select(Polygon polygon) throws Exception {
		GDALPolygonCriteria c = new GDALPolygonCriteria(polygon);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while (feature != null) {
			features.add(GDALUtils.convert(feature));
			feature = layer.GetNextFeature();
		}
		return features;
	}

	private Collection<Feature> select(Circle circle) throws Exception {
		GDALCircleCriteria c = new GDALCircleCriteria(circle);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while (feature != null) {
			features.add(GDALUtils.convert(feature));
			feature = layer.GetNextFeature();
		}
		return features;
	}

	private Collection<Feature> select(Line line) throws Exception {
		GDALLineCriteria c = new GDALLineCriteria(line);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while (feature != null) {
			features.add(GDALUtils.convert(feature));
			feature = layer.GetNextFeature();
		}
		return features;
	}

	@Override
	public Collection<Feature> select(GeolocationCriteria<? extends Shape> criteria) throws Exception {
		Shape shape = criteria.getShape();
		if (shape instanceof Polygon) {
			return select((Polygon) shape);
		} else if (shape instanceof Circle) {
			return select((Circle) shape);
		} else if (shape instanceof Line) {
			return select((Line) shape);
		}
		return Collections.emptyList();
	}

	
	private Geometry convert(GeoJsonObject geometry) throws JsonProcessingException {
		Geometry geo = ogr.CreateGeometryFromJson(mapper.writeValueAsString(geometry));
		return geo;
	}
	
	private static void setField(org.gdal.ogr.Feature feature, String k, Object v) {
		if (v instanceof String) {
			feature.SetField(k, (String) v);
		}
		// TODO TO BE CONTINUED see ogr.ogrConstants.OFT...
	}
	
	@Override
	public void insertFeature(Feature feature) throws Exception {
	
		FeatureDefn schema = layer.GetLayerDefn();
		// add eventual new fields to the schema
		final Layer l = layer;
		feature.getProperties().forEach((k, v) -> {
			addField(l, schema, k, v);
		});
		org.gdal.ogr.Feature ofeature = new org.gdal.ogr.Feature(schema);
		feature.getProperties().forEach((k, v) -> {
			setField(ofeature, k, v);
		});
		GeoJsonObject geometry = feature.getGeometry();
		Geometry geo = convert(geometry);
		ofeature.SetGeometry(geo);
		ofeature.SetField("fid", feature.getId());
		layer.CreateFeature(ofeature);
		
	}

}
