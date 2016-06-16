package fr.cs.regards.tools.elastic;

import org.geojson.Crs;
import org.geojson.FeatureCollection;

public class FeatureCollectionInfo {
	private Crs crs = null;
	private double[] bbox = null;
	private String name = null;
	
	
	public FeatureCollectionInfo() {
		super();
	}
	
	public FeatureCollectionInfo(FeatureCollection featureCollection, String name) {
		this.crs = featureCollection.getCrs();
		this.bbox = featureCollection.getBbox();
		this.name = name;
	}
	public Crs getCrs() {
		return crs;
	}
	public void setCrs(Crs crs) {
		this.crs = crs;
	}
	public double[] getBbox() {
		return bbox;
	}
	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}
