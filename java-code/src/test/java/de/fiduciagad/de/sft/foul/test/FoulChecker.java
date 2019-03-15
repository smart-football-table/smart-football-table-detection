package de.fiduciagad.de.sft.foul.test;

import java.util.Date;
import java.util.List;

import fiduciagad.de.sft.main.BallPosition;

public class FoulChecker {

	public boolean isThereAFoal(List<BallPosition> ballPositions) {
		boolean thereIsAFoul = false;
		int counter = 0;
		BallPosition lastPosition = new BallPosition(-1, -1, new Date());

		for (int i = 0; i < 301; i++) {

			// iterate over the last 300 positions
			BallPosition position = ballPositions.get((ballPositions.size() - 301) + i);

			if (noVerticalPositionChangeDetected(lastPosition, position)) {
				counter++;
			} else {
				counter = 0;
			}

			if (counter == 300) {
				thereIsAFoul = true;
			}

			System.out.println(counter);

			lastPosition = position;

		}

		return thereIsAFoul;
	}

	private boolean noVerticalPositionChangeDetected(BallPosition lastPosition, BallPosition position) {

		boolean xInSameRange = position.getXCoordinate() >= lastPosition.getXCoordinate() - 10
				&& position.getXCoordinate() <= lastPosition.getXCoordinate() + 10;

		return xInSameRange;
	}

}
