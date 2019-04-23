package detection2.data;

import static detection2.data.unit.DistanceUnit.CENTIMETER;

import detection2.data.unit.SpeedUnit;

public class Velocity {

	private final double metersPerSecond;

	public Velocity(Distance distance, long millis) {
		this.metersPerSecond = mps(distance.value(CENTIMETER), millis);
	}

	private double mps(double cm, long millis) {
		return 10 * cm / millis;
	}

	public double value(SpeedUnit speedUnit) {
		return speedUnit.toMps(metersPerSecond);
	}

}