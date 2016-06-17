package fr.cs.regards.business.geolocation.postgis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gdal.ogr.Layer;
import org.geojson.Feature;

import fr.cs.regards.business.geolocation.Circle;
import fr.cs.regards.business.geolocation.GeolocationCriteria;
import fr.cs.regards.business.geolocation.GeolocationService;
import fr.cs.regards.business.geolocation.Line;
import fr.cs.regards.business.geolocation.Polygon;
import fr.cs.regards.business.geolocation.Shape;
import fr.cs.regards.business.tools.postgis.PostGISUtils;

public class PostgisGeolacationService implements GeolocationService {
	private Layer layer;

	public PostgisGeolacationService(String dataSourceName, String layerName) {
		layer = PostGISUtils.GetLayer(dataSourceName, layerName);
	}


	
	private Collection<Feature> select(Polygon polygon) throws Exception {
		PostgisPolygonCriteria c = new PostgisPolygonCriteria(polygon);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while(feature != null){
			features.add(PostGISUtils.convert(feature));
			feature = layer.GetNextFeature();
		}
		return features;
	}

	private Collection<Feature> select(Circle circle) throws Exception {
		PostgisCircleCriteria c = new PostgisCircleCriteria(circle);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while(feature != null){
			features.add(PostGISUtils.convert(feature));
			feature = layer.GetNextFeature();
		}
		return features;
	}
	
	private Collection<Feature> select(Line line) throws Exception {
		PostgisLineCriteria c = new PostgisLineCriteria(line);
		layer.SetSpatialFilter(c.toGeometry());
		org.gdal.ogr.Feature feature = layer.GetNextFeature();
		List<Feature> features = new ArrayList<>();
		while(feature != null){
			features.add(PostGISUtils.convert(feature));
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

}
