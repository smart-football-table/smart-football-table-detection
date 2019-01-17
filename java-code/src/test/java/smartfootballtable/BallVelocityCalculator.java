package smartfootballtable;

public class BallVelocityCalculator {

	public double getDistanceInCentimeterBetween(BallPosition position1, BallPosition position2) {
		return (Math.floor(((Math.sqrt(Math.pow((position2.getXCoordinate() - position1.getXCoordinate()), 2)
				+ Math.pow((position2.getYCoordinate() - position1.getYCoordinate()), 2))) * 10) * 100) / 100) /10;
	}

	public double calculateVelocityInMeterPerSecond(BallPosition position1, BallPosition position2) {

		long time = position2.getTimepoint().getTime() - position1.getTimepoint().getTime();
		
		double distance = getDistanceInCentimeterBetween(position1, position2) / 100;
		
		return ((distance/time) * 100) / 100;
	}

	public double calculateVelocityInKilometerPerHour(BallPosition position1, BallPosition position2) {
		return calculateVelocityInMeterPerSecond(position1, position2) * 3.6;
	}

}
