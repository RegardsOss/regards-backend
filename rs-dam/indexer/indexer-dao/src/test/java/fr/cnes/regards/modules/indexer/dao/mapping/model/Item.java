package fr.cnes.regards.modules.indexer.dao.mapping.model;

public class Item {
    final DateRange dateRange;
    final Location location;
    final String name;
    final String content;
    final String city;
    final String country;
    final int size;
    final float probability;
    public Item(DateRange dateRange, Location location, String name, String content, String city, String country, int size, float probability) {
        this.dateRange = dateRange;
        this.location = location;
        this.name = name;
        this.content = content;
        this.city = city;
        this.country = country;
        this.size = size;
        this.probability = probability;
    }
}
