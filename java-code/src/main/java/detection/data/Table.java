package detection.data;

import static detection.data.unit.DistanceUnit.CENTIMETER;

import detection.data.position.AbsolutePosition;
import detection.data.position.RelativePosition;

public class Table {

	private final double widthInCm, heightInCm;

	private Table(double widthInCm, double heightInCm) {
		this.widthInCm = widthInCm;
		this.heightInCm = heightInCm;
	}

	public Table(Distance width, Distance height) {
		this(width.value(CENTIMETER), height.value(CENTIMETER));
	}

	public AbsolutePosition toAbsolute(RelativePosition pos) {
		return new AbsolutePosition(pos, convertX(pos.getX()), convertY(pos.getY()));
	}

	private double convertY(double y) {
		return heightInCm * y;
	}

	private double convertX(double x) {
		return widthInCm * x;
	}

	public Distance getHeight() {
		return new Distance(heightInCm, CENTIMETER);
	}

	public Distance getWidth() {
		return new Distance(widthInCm, CENTIMETER);
	}

	@Override
	public String toString() {
		return "Table [width=" + widthInCm + ", height=" + heightInCm + "]";
	}

}
