package detection.data;

import detection.data.position.AbsolutePosition;
import detection.data.position.RelativePosition;

public class Table {

	private final int width;
	private final int height;

	public Table(int width, int height) {
		this.width = width;
		this.height = height;
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

	@Override
	public String toString() {
		return "Table [width=" + width + ", height=" + height + "]";
	}

}