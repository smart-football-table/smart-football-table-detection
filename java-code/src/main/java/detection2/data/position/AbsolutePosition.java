package detection2.data.position;

public class AbsolutePosition implements Position {

	private final RelativePosition relativePosition;
	private final double x;
	private final double y;

	public AbsolutePosition(RelativePosition relativePosition, double x, double y) {
		this.relativePosition = relativePosition;
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean isNull() {
		return relativePosition.isNull();
	}

	public RelativePosition getRelativePosition() {
		return relativePosition;
	}

	@Override
	public long getTimestamp() {
		return relativePosition.getTimestamp();
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public String toString() {
		return "AbsolutePosition [relativePosition=" + relativePosition + ", x=" + x + ", y=" + y + "]";
	}

}