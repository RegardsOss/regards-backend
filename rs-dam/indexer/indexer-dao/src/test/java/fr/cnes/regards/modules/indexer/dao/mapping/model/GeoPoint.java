package fr.cnes.regards.modules.indexer.dao.mapping.model;

public class GeoPoint {
    public final float lat;
    public final float lon;
    public GeoPoint(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
    }
    public GeoPoint translate(float x, float y) {
        return new GeoPoint(lat + x, lon + y);
    }

    public float[] coordinates() {
        return new float[]{ lat, lon };
    }
}
