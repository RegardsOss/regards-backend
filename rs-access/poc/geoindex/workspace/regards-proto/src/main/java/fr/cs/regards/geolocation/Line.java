package fr.cs.regards.geolocation;

import java.util.ArrayList;
import java.util.List;

public class Line implements Shape {
	private List<Coordinate> coordinates = new ArrayList<>();

	public List<Coordinate> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<Coordinate> coordinates) {
		this.coordinates = coordinates;
	}
	
	
	
	
}
