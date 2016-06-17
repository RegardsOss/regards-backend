package fr.cs.regards.geolocation;

public class Distance {
	@Override
	public String toString() {
		return value + "" + unit;
	}

	public static enum Unit {
		m, km
	}

	float value = 0.0f;
	Unit unit = Unit.m;

	public Distance() {
		super();
	}

	public Distance(float value, Unit unit) {
		super();
		this.value = value;
		this.unit = unit;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public float getValue(Unit unit) {
		switch (unit) {
		case m:
			return valueInMeter();
		case km:
			return valueInKilometer();
		default:
			throw new IllegalArgumentException("Unknown unit " + unit.name());
		}
	}

	private float valueInKilometer() {
		switch (unit) {
		case m:
			return value / 1000.0f;
		case km:
			return value;
		default:
			throw new IllegalArgumentException("Unknown unit " + unit.name());
		}
	}

	private float valueInMeter() {
		switch (unit) {
		case m:
			return value;
		case km:
			return value * 1000.0f;
		default:
			throw new IllegalArgumentException("Unknown unit " + unit.name());
		}
	}

}
