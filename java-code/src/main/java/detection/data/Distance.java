package detection.data;

import detection.data.unit.DistanceUnit;

public class Distance {

	private final double value;
	private final DistanceUnit distanceUnit;

	public Distance(double value, DistanceUnit distanceUnit) {
		this.value = value;
		this.distanceUnit = distanceUnit;
	}

	public double value(DistanceUnit target) {
		return distanceUnit.convert(value, target);
	}

}