package detection.data;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

import detection.data.position.Position;
import detection.data.unit.DistanceUnit;
import detection.data.unit.SpeedUnit;

public class Movement {

	private final long durationInMillis;
	private final Velocity velocity;
	private final Distance distance;

	public Movement(Position pos1, Position pos2, DistanceUnit distanceUnit) {
		this.distance = new Distance(sqrt(pow2(diffX(pos1, pos2)) + pow2(diffY(pos1, pos2))), distanceUnit);
		this.durationInMillis = pos2.getTimestamp() - pos1.getTimestamp();
		if (this.durationInMillis < 0) {
			throw new IllegalStateException("timestamp of pos2 (" + pos2 + ") before pos1 (" + pos1 + ")");
		}
		this.velocity = new Velocity(distance, this.durationInMillis, MILLISECONDS);
	}

	public double distance(DistanceUnit target) {
		return distance().value(target);
	}

	public Distance distance() {
		return distance;
	}

	public long duration(TimeUnit target) {
		return target.convert(durationInMillis, MILLISECONDS);
	}

	public double velocity(SpeedUnit speedUnit) {
		return velocity.value(speedUnit);
	}

	private double diffX(Position p1, Position p2) {
		return p1.getX() - p2.getX();
	}

	private double diffY(Position p1, Position p2) {
		return p1.getY() - p2.getY();
	}

	private double pow2(double d) {
		return pow(d, 2);
	}

}