package fiduciagad.de.sft.main;

public class BallPositionHandler {

	public BallPosition createBallPositionFrom(String outputOne, String outputTwo) {

		String[] valuesPositionOne = outputOne.split("\\|"); // regex in java <3
		String[] valuesPositionTwo = outputTwo.split("\\|");

		int outputNumberOne = getValueFromString(valuesPositionOne[0]);
		int xCoordinateOne = getValueFromString(valuesPositionOne[2]);
		int yCoordinateOne = getValueFromString(valuesPositionOne[3]);
		String timepointOne = valuesPositionOne[1];

		int outputNumberTwo = getValueFromString(valuesPositionTwo[0]);
		int xCoordinateTwo = getValueFromString(valuesPositionTwo[2]);
		int yCoordinateTwo = getValueFromString(valuesPositionTwo[3]);
		String timepointTwo = valuesPositionTwo[1];

		int xCoordinateFinal = 0;
		int yCoordinateFinal = 0;
		String timepointFinal = timepointOne;

		if (xCoordinateOne == -1 && yCoordinateOne == -1) {

			xCoordinateFinal = xCoordinateTwo + ConfiguratorValues.getxOffsetCameraTwo();
			yCoordinateFinal = yCoordinateTwo + ConfiguratorValues.getyOffsetCameraTwo();

			if (xCoordinateTwo == -1 && yCoordinateTwo == -1) {
				xCoordinateFinal = -1;
				yCoordinateFinal = -1;
			}
		}

		else if (xCoordinateTwo == -1 && yCoordinateTwo == -1) {

			xCoordinateFinal = xCoordinateOne + ConfiguratorValues.getxOffsetCameraOne();
			yCoordinateFinal = yCoordinateOne + ConfiguratorValues.getyOffsetCameraOne();

			if (xCoordinateOne == -1 && yCoordinateOne == -1) {
				xCoordinateFinal = -1;
				yCoordinateFinal = -1;
			}
		} else {
			xCoordinateFinal = xCoordinateOne;
			yCoordinateFinal = yCoordinateOne;
		}

		BallPosition ballPosition = new BallPosition();

		ballPosition.setXCoordinate(xCoordinateFinal);
		ballPosition.setYCoordinate(yCoordinateFinal);

		String completeTimepoint = timepointFinal.replaceAll("\\.", "");
		ballPosition.setTimepoint(Long.parseLong(completeTimepoint));

		return ballPosition;
	}

	public int getValueFromString(String string) {
		return Integer.parseInt(string);
	}

}
