package fr.cs.regards.geolocation;

public class Circle implements Shape {
	private Coordinate center = new Coordinate();
	private Distance radias = new Distance();
	
	public Circle(Coordinate center, Distance radias) {
		super();
		this.center = center;
		this.radias = radias;
	}
	
	public Circle() {
		super();
	}

	public Coordinate getCenter() {
		return center;
	}
	public void setCenter(Coordinate center) {
		this.center = center;
	}
	public Distance getRadius() {
		return radias;
	}
	public void setRadias(Distance radias) {
		this.radias = radias;
	}
	
}
