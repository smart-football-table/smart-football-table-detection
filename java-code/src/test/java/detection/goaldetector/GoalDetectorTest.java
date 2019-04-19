package detection.goaldetector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import detection.goaldetector.GoalDetector;
import detection.main.BallPosition;
import detection.main.BallPositionHandler;
import detection.main.ConfiguratorValues;

public class GoalDetectorTest {

	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();
	private BallPosition ballPosition_t;
	private BallPosition ballPosition_tMinus1;
	private BallPosition ballPosition_tMinus2;
	private GoalDetector goalDetector;

	@Before
	public void initialize() {
		ConfiguratorValues.setGameFieldSize(200, 50);
		ConfiguratorValues.setFramesPerSecond(30);

		goalDetector = new GoalDetector();
		goalDetector.setBallWasInMidArea(true);
		createEmptyBallPositionsForLastTwoSeconds();

	}

	@Test
	public void gettingLessThanTwoSecondsNoPositionIsntAGoal() {

		addRealBallPositionAfterOneSecond();

		assertThat(goalDetector.isThereAGoal(ballPositions), is(false));

	}

	@Test
	public void gettingTwoSecondsNoPositionIsAGoalWhenLastBallWasInFrontOfAGoal() {

		ballPositions.add(0, getPositionInFrontOfGoal());

		assertThat(goalDetector.isThereAGoal(ballPositions), is(true));

	}

	@Test
	public void gettingTwoSecondsNoPositionIsNotAGoalWhenLastBallWasNotInFrontOfAGoal() {

		ballPositions.add(0, getPositionNotInFrontOfGoal());

		assertThat(goalDetector.isThereAGoal(ballPositions), is(false));

	}

	@Test
	public void ballWasNeverInMidAreaIsNotAGoal() {

		goalDetector.setBallWasInMidArea(false);

		ballPositions.add(0, getPositionInFrontOfGoal());

		assertThat(goalDetector.isThereAGoal(ballPositions), is(false));

	}

	@Test
	public void ifBallPositionIsInMiddleOfFieldBallWasInMidArea() {

		goalDetector.setBallWasInMidArea(false);

		String string = "1235232348.00|100|25";

		BallPositionHandler positionHandler = new BallPositionHandler();
		positionHandler.setGoalDetector(goalDetector);

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string);

		ballPositions.add(0, getPositionInFrontOfGoal());

		assertThat(goalDetector.isThereAGoal(ballPositions), is(true));

	}

	@Test
	public void getCorrectSiteOfGoal_Right_StraightShot() {

		String gameField = // field with 20x5, each one is 10 pixel (200*50)
				"----------------------," + //
						"|00000000000000000000|," + //
						"-00000000000000000000-," + //
						" 00000000000000001020 ," + //
						"-00000000000000000000-," + //
						"|00000000000000000000|," + //
						"----------------------";

		initalizeBallPositionsFrom(gameField);

		GoalDetector goalDetector = new GoalDetector();

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_tMinus1, ballPosition_tMinus2), is("on the right"));

	}

	@Test
	public void getCorrectSiteOfGoal_Left_StraightShot() {

		String gameField = // field with 20x5, each one is 10 pixel (200*50)
				"----------------------," + //
						"|00000000000000000000|," + //
						"-00000000000000000000-," + //
						" 02010000000000000000 ," + //
						"-00000000000000000000-," + //
						"|00000000000000000000|," + //
						"----------------------";

		initalizeBallPositionsFrom(gameField);

		GoalDetector goalDetector = new GoalDetector();

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_tMinus1, ballPosition_tMinus2), is("on the left"));

	}

	@Test
	public void getCorrectSiteOfGoal_Right_BlockShot() {

		String gameField = // field with 20x5, each one is 10 pixel (200*50)
				"----------------------," + //
						"|00000000000000000000|," + //
						"-00000000000000000010-," + //
						" 00000000000000000200 ," + //
						"-00000000000000000000-," + //
						"|00000000000000000000|," + //
						"----------------------";

		initalizeBallPositionsFrom(gameField);

		GoalDetector goalDetector = new GoalDetector();

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_tMinus1, ballPosition_tMinus2), is("on the right"));

	}

	private BallPosition getPositionNotInFrontOfGoal() {
		return new BallPosition(100, 25, new Date());
	}

	private BallPosition getPositionInFrontOfGoal() {
		return new BallPosition(10, 25, new Date());
	}

	private void initalizeBallPositionsFrom(String gameField) {

		String[] linesFromField = gameField.split(",");

		for (int i = 0; i < linesFromField.length; i++) {

			String actualLine = linesFromField[i];

			for (int j = 0; j < actualLine.toCharArray().length; j++) {

				char character = actualLine.charAt(j);

				int yCoordinate = (i - 1) * 10;
				int xCoordinate = (j - 1) * 10;

				if (character == '1') {
					ballPosition_tMinus2 = new BallPosition(xCoordinate, yCoordinate, new Date());
				}
				if (character == '2') {
					ballPosition_tMinus1 = new BallPosition(xCoordinate, yCoordinate, new Date());
				}
				if (character == '3') {
					ballPosition_t = new BallPosition(xCoordinate, yCoordinate, new Date());
				}

			}
		}

		// set default: no position

		if (ballPosition_tMinus2 == null) {
			ballPosition_tMinus2 = new BallPosition(-1, -1, new Date());
		}
		if (ballPosition_tMinus1 == null) {
			ballPosition_tMinus1 = new BallPosition(-1, -1, new Date());
		}
		if (ballPosition_t == null) {
			ballPosition_t = new BallPosition(-1, -1, new Date());
		}

	}

	private void createEmptyBallPositionsForLastTwoSeconds() {

		int countBallPositionsToBeCreated = ConfiguratorValues.getFramesPerSecond() * 2;

		for (int i = 0; i < countBallPositionsToBeCreated; i++) {
			BallPosition position = new BallPosition(-1, -1, new Date());
			ballPositions.add(position);
		}
	}

	private void addRealBallPositionAfterOneSecond() {
		ballPositions.add(new BallPosition(100, 100, new Date()));
	}

}
