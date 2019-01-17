package smartfootballtable;

public class BallVelocityCalculator {

	public double getDistanceInMillimeterBetween(BallPosition position1, BallPosition position2) {
		return Math.floor(((Math.sqrt(Math.pow((position2.getXCoordinate() - position1.getXCoordinate()), 2)
				+ Math.pow((position2.getYCoordinate() - position1.getYCoordinate()), 2))) * 10) * 100) / 100;
	}

}
