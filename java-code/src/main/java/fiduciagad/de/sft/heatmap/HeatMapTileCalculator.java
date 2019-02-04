package fiduciagad.de.sft.heatmap;

public class HeatMapTileCalculator {

	private int xHeatmapSize = 0;
	private int yHeatMapSize = 0;

	private int heatMapTilesPerColumn = 1;
	private int heatMapTilesPerRow = 1;

	public int calculateHeatMapId(double x, double y) {
		
		int sizePerHeatMapTile = xHeatmapSize / heatMapTilesPerColumn;

		return (int) (Math.ceil(x / sizePerHeatMapTile));
	}

	public void setHeatMapSize(int xMaxOfGame, int yMaxOfGame) {
		xHeatmapSize = xMaxOfGame;
		yHeatMapSize = yMaxOfGame;
	}

	public void setheatMapTilesPerColumn(int heatMapTilesPerColumn) {
		this.heatMapTilesPerColumn = heatMapTilesPerColumn;
	}

	public void setheatMapTilesPerRow(int heatMapTilesPerRow) {
		this.heatMapTilesPerRow = heatMapTilesPerRow;

	}

}
