package de.fiduciagad.de.sft.test;

public class FootballTable {

	private static int xSize = 0;
	private static int ySize = 0;

	public static void setGameFieldSize(int xSizeGet, int ySizeGet) {
		xSize = xSizeGet;
		ySize = ySizeGet;
	}

	public static int getXMaxOfGameField() {
		return xSize;
	}

	public static int getYMaxOfGameField() {
		return ySize;
	}

}
