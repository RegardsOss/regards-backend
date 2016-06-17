package fr.cs.regards.tools.elastic;

import org.geojson.Crs;
import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.jackson.CrsType;

public class TestsDataSetCreator {

	public static void main(String[] args) {
		//ElasticUtils.createIndex("test");
		int size_lon = 100;
		int size_lat = 50;
		int cpt = 0;
		Feature feature = new Feature();
		Crs crs = new Crs();
		crs.setType(CrsType.name);
		crs.getProperties().put("name", "uai2000:49901");
		feature.setCrs(crs);
		for (float lon = -180.0f; lon < 180.0f; lon += 360.0 / size_lon) {
			for (float lat = -90.0f; lat < 90.0f; lat += 180.0 / size_lat) {
				feature.setGeometry(new Point(new LngLatAlt(lon, lat)));
				feature.setId("" + cpt);
				ElasticUtils.insertObject("test", "feature", cpt++, feature);
			}
		}
	}
}
// {
// "mappings": {
// "feature": {
// "_all": {
// "enabled": false
// },
// "properties": {
// "geometry": {
// "type": "geo_shape"
// }
// }
// }
// }
// }