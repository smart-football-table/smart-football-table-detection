package fiduciagad.de.sft.ballvelocity;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class BallVelocityCalculator {

	public double getVelocityOfBallInKilometerPerHour(BallPosition position1, BallPosition position2) {
		return getVelocityOfBallInMeterPerSecond(position1, position2) * 3.6; // (m/s) --> (km/h): with 3.6
	}

	public double getVelocityOfBallInMeterPerSecond(BallPosition oldPosition, BallPosition newPosition) {

		long timeDifference = getTimeDifferenceInSecondsBetween(oldPosition, newPosition);
		double distanceDifference = getDistanceInMeterBetween(oldPosition, newPosition);

		return calculateVelocity(timeDifference, distanceDifference);
	}

	private double calculateVelocity(long timeDifference, double distanceDifference) {
		double velocity = distanceDifference / timeDifference;// v = s/t
		double velocityRounded = (velocity * 100) / 100;
		return velocityRounded;
	}

	private long getTimeDifferenceInSecondsBetween(BallPosition oldPosition, BallPosition newPosition) {
		return newPosition.getTimepoint().getTime() - oldPosition.getTimepoint().getTime();
	}

	private double getDistanceInMeterBetween(BallPosition oldPosition, BallPosition newPosition) {

		double distanceInMilliMeter = getDistanceInPixelBetween(oldPosition, newPosition)
				* ConfiguratorValues.getMillimeterPerPixel();
		double distanceInMeter = distanceInMilliMeter / 1000;

		return distanceInMeter;
	}

	public double getDistanceInPixelBetween(BallPosition position1, BallPosition position2) {

		double xDistance = position2.getXCoordinate() - position1.getXCoordinate();
		double yDistance = position2.getYCoordinate() - position1.getYCoordinate();

		// distance between two points = root((x2 - x1)² + (y2 - y1)²)

		double temporaryResult = Math.pow(xDistance, 2) + Math.pow(yDistance, 2);

		double distance = Math.sqrt(temporaryResult);
		double distanceRounded = Math.floor(distance * 100) / 100;

		return distanceRounded;
	}

}
