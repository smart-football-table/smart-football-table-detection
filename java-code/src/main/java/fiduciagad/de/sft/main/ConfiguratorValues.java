package fiduciagad.de.sft.main;

public class ConfiguratorValues {

	private static int millimeterPerPixel = 0;
	
	private static int xOffsetCameraOne = 0;
	private static int yOffsetCameraOne = 0;
	
	private static int xOffsetCameraTwo = 0;
	private static int yOffsetCameraTwo = 0;

	private static int xSizeGameField = 0;
	private static int ySizeGameField = 0;

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

	public static int getxOffsetCameraOne() {
		return xOffsetCameraOne;
	}

	public static void setxOffsetCameraOne(int xOffsetCameraOne) {
		ConfiguratorValues.xOffsetCameraOne = xOffsetCameraOne;
	}

	public static int getyOffsetCameraOne() {
		return yOffsetCameraOne;
	}

	public static void setyOffsetCameraOne(int yOffsetCameraOne) {
		ConfiguratorValues.yOffsetCameraOne = yOffsetCameraOne;
	}

	public static int getxOffsetCameraTwo() {
		return xOffsetCameraTwo;
	}

	public static void setxOffsetCameraTwo(int xOffsetCameraTwo) {
		ConfiguratorValues.xOffsetCameraTwo = xOffsetCameraTwo;
	}

	public static int getyOffsetCameraTwo() {
		return yOffsetCameraTwo;
	}

	public static void setyOffsetCameraTwo(int yOffsetCameraTwo) {
		ConfiguratorValues.yOffsetCameraTwo = yOffsetCameraTwo;
	}
	
	

}
