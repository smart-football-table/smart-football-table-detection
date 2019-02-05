package de.fiduciagad.de.sft.test;

public class ConfiguratorValues {

	private static int xSizeGameField = 0;
	private static int ySizeGameField = 0;

	private static int millimeterPerPixel = 0;

	public static void setGameFieldSize(int xSize, int ySize) {
		xSizeGameField = xSize;
		ySizeGameField = ySize;
	}

	public static void setMillimeterPerPixel(int millimeterPerPixel) {
		ConfiguratorValues.millimeterPerPixel = millimeterPerPixel;
	}

	public static int getMillimeterPerPixel() {
		return millimeterPerPixel;
	}

	public static int getXMaxOfGameField() {
		return xSizeGameField;
	}

	public static int getYMaxOfGameField() {
		return ySizeGameField;
	}

}
