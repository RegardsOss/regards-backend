package fr.cs.regards.geolocation;



public class GeolocationCriteria <S extends Shape>{
	private S shape;
	
	
	public GeolocationCriteria() {
		super();
	}
	public GeolocationCriteria(S shape) {
		super();
		this.shape = shape;
	}
	
	public S getShape() {
		return shape;
	}
	public void setShape(S shape) {
		this.shape = shape;
	}
	
}
