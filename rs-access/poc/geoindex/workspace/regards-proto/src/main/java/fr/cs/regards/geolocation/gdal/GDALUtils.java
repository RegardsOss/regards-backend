package fr.cs.regards.geolocation.gdal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.ogr.ogrConstants;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class GDALUtils {
	private static String URL = "PG: host=%s dbname=%s user=%s password=%s";
	private static String HOST = "localhost";
	private static String USER = "regards";
	private static String PASSWORD = "regards";
	private static Map<String, DataSource> DATA_SOURCES = new HashMap<String, DataSource>();
	static final ObjectMapper MAPPER = new ObjectMapper();

	public static <T> void insertObject(String index, String type, int id, Feature object) throws JsonProcessingException {
		Layer layer = GetLayer(index, type);
		FeatureDefn schema = layer.GetLayerDefn();
		// add eventual new fields to the schema
		final Layer l = layer;
		object.getProperties().forEach((k, v) -> {
			addField(l, schema, k, v);
		});
		org.gdal.ogr.Feature feature = new org.gdal.ogr.Feature(schema);
		

		object.getProperties().forEach((k, v) -> {
			setField(feature, k, v);
		});
		GeoJsonObject geometry = object.getGeometry();
		Geometry geo = convert(geometry);
		System.out.println(geo.ExportToJson());
		feature.SetGeometry(geo);
		feature.SetField("fid", object.getId());
		layer.CreateFeature(feature);

		

	}

	public static Geometry convert(GeoJsonObject geometry) throws JsonProcessingException {
		Geometry geo = ogr.CreateGeometryFromJson(MAPPER.writeValueAsString(geometry));
		return geo;
	}
	
	public static org.geojson.Feature convert(org.gdal.ogr.Feature feature) throws JsonParseException, JsonMappingException, IOException{
		int fieldCount = feature.GetFieldCount();
		org.geojson.Feature fgeo = new Feature();
		fgeo.setId(feature.GetFieldAsString("fid"));
		for(int fieldNumber = 0; fieldNumber<fieldCount; fieldNumber++){
			FieldDefn fdef = feature.GetFieldDefnRef(fieldNumber);
			String fname = fdef.GetName();
			if(fname.equals("fid")) continue;
			if (fdef.GetFieldType() == ogrConstants.OFTString){
				fgeo.setProperty(fname, feature.GetFieldAsString(fieldNumber));
			}
			//TODO to be continued see ogrConstants.OFT...
		}
//		String geoJsonExport = feature.GetGeometryRef().ExportToJson();
//		GeoJsonObject geo = MAPPER.readValue(geoJsonExport, GeoJsonObject.class);
//		fgeo.setGeometry(geo);
		return fgeo;
		
	}

	public static Layer GetLayer(String index, String type) {
		
		DataSource dataSource = GetDataSource(index);
		Layer layer = dataSource.GetLayer(type);
		
		if (layer == null) {
			layer = dataSource.CreateLayer(type);
			FeatureDefn schema = layer.GetLayerDefn();
			addField(layer, schema, "fid", "");
		}
		return layer;
	}

	private static DataSource GetDataSource(String index) {
		if (DATA_SOURCES.isEmpty())
			ogr.RegisterAll();

		if (!DATA_SOURCES.containsKey(index)) {
			String connectString = String.format(URL, HOST, index, USER, PASSWORD);
			System.out.println(connectString);
			DATA_SOURCES.put(index, ogr.Open(connectString, 1));
		}
		DataSource dataSource = DATA_SOURCES.get(index);
		return dataSource;
	}

	private static void addField(Layer layer, FeatureDefn schema, String k, Object v) {
		if (schema.GetFieldIndex(k) >= 0)
			return;
		if (v instanceof String) {
			 layer.CreateField(new FieldDefn(k, ogrConstants.OFTString));
		}
		// TODO TO BE CONTINUED see ogr.ogrConstants.OFT...

	}

	private static void setField(org.gdal.ogr.Feature feature, String k, Object v) {
		if (v instanceof String) {
			feature.SetField(k, (String) v);
		}
		// TODO TO BE CONTINUED see ogr.ogrConstants.OFT...
	}

	public static void main(String[] args) {
		ogr.RegisterAll();

	}
}
