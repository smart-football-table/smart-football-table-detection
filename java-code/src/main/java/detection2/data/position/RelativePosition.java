package detection2.data.position;

import static java.lang.Math.abs;

public class RelativePosition implements Position {

	private final long timestamp;
	private final double x;
	private final double y;

	public static RelativePosition noPosition(long timestamp) {
		return create(timestamp, -1, -1);
	}

	// TODO create two subclasses absent/pressent
	public static RelativePosition create(long timestamp, double x, double y) {
		return new RelativePosition(timestamp, x, y);
	}

	private RelativePosition(long timestamp, double x, double y) {
		this.timestamp = timestamp;
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean isNull() {
		return x < 0 || y < 0;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	public RelativePosition normalizeX() {
		return create(timestamp, centerX() + abs(centerX() - x), y);
	}

	public RelativePosition normalizeY() {
		return create(timestamp, x, centerY() + abs(centerY() - y));
	}

	public boolean isRightHandSide() {
		return x >= centerX();
	}

	private double centerX() {
		return 0.5;
	}

	private double centerY() {
		return 0.5;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelativePosition other = (RelativePosition) obj;
		if (timestamp != other.timestamp)
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RelativePosition [timestamp=" + timestamp + ", x=" + x + ", y=" + y + "]";
	}

}