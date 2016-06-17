package fr.cs.regards.geolocation.elastic.binding;

import org.geojson.Feature;

public class Hit {
	public String _index;
	public String _type;
	public String _id;
	public float _score;
	public Feature _source;
}
