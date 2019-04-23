package detection2.data;

import static detection2.data.unit.DistanceUnit.CENTIMETER;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

import detection2.data.position.Position;
import detection2.data.unit.DistanceUnit;
import detection2.data.unit.SpeedUnit;

public class Movement {

	private final long durationInMillis;
	private final Velocity velocity;
	private final Distance distance;

	public Movement(Position pos1, Position pos2) {
		this.distance = new Distance(sqrt(pow2(absDiffX(pos1, pos2)) + pow2(absDiffY(pos1, pos2))), CENTIMETER);
		this.durationInMillis = pos2.getTimestamp() - pos1.getTimestamp();
		this.velocity = new Velocity(distance, this.durationInMillis);
	}

	public double distance(DistanceUnit target) {
		return distance.value(target);
	}

	public long duration(TimeUnit target) {
		return target.convert(durationInMillis, MILLISECONDS);
	}

	public double velocity(SpeedUnit speedUnit) {
		return velocity.value(speedUnit);
	}

	private double absDiffX(Position p1, Position p2) {
		return abs(p1.getX() - p2.getX());
	}

	private double absDiffY(Position p1, Position p2) {
		return abs(p1.getY() - p2.getY());
	}

	private double pow2(double d) {
		return pow(d, 2);
	}

}