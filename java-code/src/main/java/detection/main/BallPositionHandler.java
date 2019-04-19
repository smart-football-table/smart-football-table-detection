package detection.main;

import detection.goaldetector.GoalDetector;

public class BallPositionHandler {

	private GoalDetector goalDetector = new GoalDetector();

	public GoalDetector getGoalDetector() {
		return goalDetector;
	}

	public void setGoalDetector(GoalDetector goalDetector) {
		this.goalDetector = goalDetector;
	}

	public BallPosition createBallPositionFrom(String output) {

		String[] valuesPositionOne = output.split("\\|"); // regex in java <3

		int xCoordinateFinal = getValueFromString(valuesPositionOne[1]);
		int yCoordinateFinal = getValueFromString(valuesPositionOne[2]);
		String timepointFinal = valuesPositionOne[0];

		BallPosition ballPosition = new BallPosition();

		if (xCoordinateFinal != -1 || yCoordinateFinal != 1) {

			xCoordinateFinal = xCoordinateFinal - ConfiguratorValues.getOffsetX();
			yCoordinateFinal = yCoordinateFinal - ConfiguratorValues.getOffsetY();

		}

		ballPosition.setXCoordinate(xCoordinateFinal);
		ballPosition.setYCoordinate(yCoordinateFinal);

		String completeTimepoint = timepointFinal.replaceAll("\\.", "");
		ballPosition.setTimepoint(Long.parseLong(completeTimepoint));

		boolean ballWasNotInMidAreaYet = !goalDetector.isBallWasInMidArea();
		if (ballWasNotInMidAreaYet) {
			checkIfBallWasInMidAreaForGoalDetector(ballPosition);
		}

		return ballPosition;
	}

	public int getValueFromString(String string) {
		return Integer.parseInt(string);
	}

	private void checkIfBallWasInMidAreaForGoalDetector(BallPosition ballPosition) {

		boolean ballWasNotInMidArea;

		double middleOfGameField = ConfiguratorValues.getXMaxOfGameField() / 2;
		double xStartOfAreaWhereBallIsntInMidArea = middleOfGameField - 50;
		double xEndOfAreaWhereBallIsntBeforeAMidArea = middleOfGameField + 50;

		ballWasNotInMidArea = xStartOfAreaWhereBallIsntInMidArea < ballPosition.getXCoordinate()
				&& ballPosition.getXCoordinate() < xEndOfAreaWhereBallIsntBeforeAMidArea;

		goalDetector.setBallWasInMidArea(ballWasNotInMidArea);
	}

}
