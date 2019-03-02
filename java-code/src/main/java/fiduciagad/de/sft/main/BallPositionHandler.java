package fiduciagad.de.sft.main;

public class BallPositionHandler {

	public BallPosition createBallPositionFrom(String string) {

		String[] values = string.split("\\|"); // regex in java <3

		BallPosition ballPosition = new BallPosition();

		ballPosition.setXCoordinate(getValueFromString(values[1]));
		ballPosition.setYCoordinate(getValueFromString(values[2]));

		String completeTimepoint = values[0].replaceAll("\\.", "");
		ballPosition.setTimepoint(Long.parseLong(completeTimepoint));

		return ballPosition;
	}

	public int getValueFromString(String string) {
		return Integer.parseInt(string);
	}

}
