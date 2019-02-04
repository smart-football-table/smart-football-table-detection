package fiduciagad.de.sft.ballvelocity;

import fiduciagad.de.sft.main.BallPosition;

public class BallVelocityCalculator {

	public double getDistanceInCentimeterBetween(BallPosition position1, BallPosition position2) {
		
		double pos1xMinusPos2x = position2.getXCoordinate() - position1.getXCoordinate();
		double pos1yMinusPos2y = position2.getYCoordinate() - position1.getYCoordinate();
		
		double result = Math.pow(pos1xMinusPos2x, 2)
				+ Math.pow(pos1yMinusPos2y, 2);
		
		double resultAfterRoot = Math.sqrt(result);
		
		double resultRounded = Math.floor(resultAfterRoot * 100) / 100;
		
		return resultRounded;
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
