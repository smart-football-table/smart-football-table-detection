package smartfootballtable;

import java.util.ArrayList;
import java.util.List;

public class DistributionHeatMapController {

	private List<Integer> fieldIdOfPositions = new ArrayList<Integer>();

	public void addPositionToField(int fieldNumber) {
		fieldIdOfPositions.add(fieldNumber);

	}

	public double getDistributionInPercentOfField(int fieldNumber) {

		double sum = 0;

		for (Integer fieldIdOfPosition : fieldIdOfPositions) {

			if (fieldNumber == fieldIdOfPosition) {
				sum++;
			}

		}

		return (sum / fieldIdOfPositions.size()) * 100;
	}

}
