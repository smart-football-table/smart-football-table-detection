package fiduciagad.de.sft.main;

public class ConfiguratorValues {

	private static int millimeterPerPixel = 0;

	private static int framesPerSecond = 0;

	private static int xSizeGameField = 0;
	private static int ySizeGameField = 0;

	private static int offsetX = 0;
	private static int offsetY = 0;

	private static int colorHSVMinH = 0;
	private static int colorHSVMinS = 0;
	private static int colorHSVMinV = 0;
	private static int colorHSVMaxH = 0;
	private static int colorHSVMaxS = 0;
	private static int colorHSVMaxV = 0;

	public static int getColorHSVMinH() {
		return colorHSVMinH;
	}

	public static void setColorHSVMinH(int colorHSVMinH) {
		ConfiguratorValues.colorHSVMinH = colorHSVMinH;
	}

	public static int getFramesPerSecond() {
		return framesPerSecond;
	}

	public static void setFramesPerSecond(int framesPerSecond) {
		ConfiguratorValues.framesPerSecond = framesPerSecond;
	}

	public static int getOffsetX() {
		return offsetX;
	}

	public static int getOffsetY() {
		return offsetY;
	}

	public static int getColorHSVMinS() {
		return colorHSVMinS;
	}

	public static void setColorHSVMinS(int colorHSVMinS) {
		ConfiguratorValues.colorHSVMinS = colorHSVMinS;
	}

	public static int getColorHSVMinV() {
		return colorHSVMinV;
	}

	public static void setColorHSVMinV(int colorHSVMinV) {
		ConfiguratorValues.colorHSVMinV = colorHSVMinV;
	}

	public static int getColorHSVMaxH() {
		return colorHSVMaxH;
	}

	public static void setColorHSVMaxH(int colorHSVMaxH) {
		ConfiguratorValues.colorHSVMaxH = colorHSVMaxH;
	}

	public static int getColorHSVMaxS() {
		return colorHSVMaxS;
	}

	public static void setColorHSVMaxS(int colorHSVMaxS) {
		ConfiguratorValues.colorHSVMaxS = colorHSVMaxS;
	}

	public static int getColorHSVMaxV() {
		return colorHSVMaxV;
	}

	public static void setColorHSVMaxV(int colorHSVMaxV) {
		ConfiguratorValues.colorHSVMaxV = colorHSVMaxV;
	}

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

	public static void setDefaultColorRangeYellow() {
		colorHSVMinH = 20;
		colorHSVMinS = 100;
		colorHSVMinV = 100;
		colorHSVMaxH = 30;
		colorHSVMaxS = 255;
		colorHSVMaxV = 255;
	}

	public static void setOffsetX(int offsetX) {
		ConfiguratorValues.offsetX = offsetX;
	}

	public static void setOffsetY(int offsetY) {
		ConfiguratorValues.offsetY = offsetY;
	}

}
