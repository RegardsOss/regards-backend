package fr.cs.regards.geolocation;

public class Coordinate {
	private float lon = 0.0f;
	private float lat = 0.0f;
	
	public Coordinate() {
		super();
	}
	public Coordinate(float lon, float lat) {
		super();
		this.lon = lon;
		this.lat = lat;
	}
	public float getLon() {
		return lon;
	}
	public void setLon(float lon) {
		this.lon = lon;
	}
	public float getLat() {
		return lat;
	}
	public void setLat(float lat) {
		this.lat = lat;
	}
	@Override
	public String toString() {
		return "["+ lon + ", " + lat + "]";
	}
	
	
	
}
