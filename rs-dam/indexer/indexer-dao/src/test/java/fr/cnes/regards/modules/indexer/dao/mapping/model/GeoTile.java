package fr.cnes.regards.modules.indexer.dao.mapping.model;

public class GeoTile {
    public final String type = "polygon";
    public final float[][][] coordinates;
    public GeoTile(GeoPoint from, GeoPoint to) {
        this.coordinates = new float[][][] { new float[][] {
            from.coordinates(),
            new float[]{from.lat, to.lon},
            to.coordinates(),
            new float[]{to.lat, from.lon},
            from.coordinates()
        }};
    }
    public GeoPoint center() {
        return new GeoPoint((coordinates[0][0][0] + coordinates[0][1][0]) / 2,
                            (coordinates[0][0][1] + coordinates[0][1][1]) / 2);
    }
}
