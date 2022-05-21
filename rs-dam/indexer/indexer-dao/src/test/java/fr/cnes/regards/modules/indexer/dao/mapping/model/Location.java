package fr.cnes.regards.modules.indexer.dao.mapping.model;

public class Location {

    public final String city;

    public final String country;

    public final GeoPoint center;

    public final GeoTile tile;

    public Location(String city, String country, GeoPoint center, GeoTile tile) {
        this.city = city;
        this.country = country;
        this.center = center;
        this.tile = tile;
    }
}
