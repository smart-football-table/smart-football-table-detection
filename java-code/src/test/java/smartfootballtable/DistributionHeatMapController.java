package smartfootballtable;

import java.util.ArrayList;
import java.util.List;

public class DistributionHeatMapController {

	private List<Integer> listOfHeatMapTileIDsOfPositions = new ArrayList<Integer>();

	public void addPositionToHeatMapTile(int fieldNumber) {
		listOfHeatMapTileIDsOfPositions.add(fieldNumber);

	}

	public double getDistributionInPercentOfHeatMapTile(int heatMapTileID) {

		double sum = 0;

		for (Integer heatMapTileIDOfPosition : listOfHeatMapTileIDsOfPositions) {

			if (heatMapTileID == heatMapTileIDOfPosition) {
				sum++;
			}

		}

		return (sum / listOfHeatMapTileIDsOfPositions.size()) * 100;
	}

}
