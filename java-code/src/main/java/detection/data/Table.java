package detection.data;

import static detection.data.unit.DistanceUnit.CENTIMETER;

import detection.data.position.AbsolutePosition;
import detection.data.position.RelativePosition;

public class Table {

	private final int width;
	private final int height;

	public Table(int widthInCm, int heightInCm) {
		this.width = widthInCm;
		this.height = heightInCm;
	}

	public Table(Distance width, Distance height) {
		this.width = (int) width.value(CENTIMETER);
		this.height = (int) height.value(CENTIMETER);
	}

	public AbsolutePosition toAbsolute(RelativePosition pos) {
		return new AbsolutePosition(pos, convertX(pos.getX()), convertY(pos.getY()));
	}

	private double convertY(double y) {
		return height * y;
	}

	private double convertX(double x) {
		return width * x;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public String toString() {
		return "Table [width=" + width + ", height=" + height + "]";
	}

}
