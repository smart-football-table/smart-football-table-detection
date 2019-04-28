package detection2.data.position;

import static java.lang.Math.abs;

public abstract class RelativePosition implements Position {

	private static class Absent extends RelativePosition {

		public Absent(long timestamp) {
			super(timestamp);
		}

		@Override
		public boolean isNull() {
			return true;
		}

		@Override
		public double getX() {
			return -1;
		}

		@Override
		public double getY() {
			return -1;
		}

	}

	private static class Present extends RelativePosition {

		private final double x;
		private final double y;

		public Present(long timestamp, double x, double y) {
			super(timestamp);
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean isNull() {
			return false;
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

	}

	private final long timestamp;

	public static RelativePosition noPosition(long timestamp) {
		return new Absent(timestamp);
	}

	public static RelativePosition create(long timestamp, double x, double y) {
		return x == -1 && y == -1 ? noPosition(timestamp) : new Present(timestamp, x, y);
	}

	private RelativePosition(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	public RelativePosition normalizeX() {
		return create(timestamp, centerX() + abs(centerX() - getX()), getY());
	}

	public RelativePosition normalizeY() {
		return create(timestamp, getX(), centerY() + abs(centerY() - getY()));
	}

	public boolean isRightHandSide() {
		return getX() >= centerX();
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
		temp = Double.doubleToLongBits(getX());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(getY());
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
		if (Double.doubleToLongBits(getX()) != Double.doubleToLongBits(other.getX()))
			return false;
		if (Double.doubleToLongBits(getY()) != Double.doubleToLongBits(other.getY()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RelativePosition [timestamp=" + timestamp + ", x=" + getX() + ", y=" + getY() + "]";
	}

}