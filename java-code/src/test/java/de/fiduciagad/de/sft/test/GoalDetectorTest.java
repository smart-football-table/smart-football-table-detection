package de.fiduciagad.de.sft.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.goaldetector.GoalDetector;
import fiduciagad.de.sft.main.BallPosition;

public class GoalDetectorTest {

	BallPosition ballPosition_t;
	BallPosition ballPosition_tMinus1;
	BallPosition ballPosition_tMinus2;

	@Before
	public void initialize() {
		ConfiguratorValues.setGameFieldSize(200, 50);
	}

	@Test
	public void gettingThreePositionsMidfieldIsntAGoal() {

		String gameField = // field with 20x5, each one is 10 pixel (200*50)
				"----------------------," + //
						"|00000000000000000000|," + //
						"-00000000000000000000-," + //
						" 00000001020300000000 ," + //
						"-00000000000000000000-," + //
						"|00000000000000000000|," + //
						"----------------------";

		initalizeBallPositionsFrom(gameField);

		GoalDetector goalDetector = new GoalDetector();

		assertThat(goalDetector.isThereAGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2), is(false));

	}

	@Test
	public void gettingThreePositionsWhereLatestIsMissingOnGoalSideIsAGoalRight() {

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

		assertThat(goalDetector.isThereAGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2), is(true));

	}

	@Test
	public void gettingThreePositionsWhereLatestIsMissingOnGoalSideIsAGoalLeft() {

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

		assertThat(goalDetector.isThereAGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2), is(true));

	}

	@Test
	public void gettingThreePositionsWhereLatestIsMissingSomewhereElseIsntAGoal() {

		String gameField = // field with 20x5, each one is 10 pixel (200*50)
				"----------------------," + //
						"|00000000000000000000|," + //
						"-00000000000000000000-," + //
						" 00000000010000000000 ," + //
						"-00000000000000000000-," + //
						"|00000000020000000000|," + //
						"----------------------";

		initalizeBallPositionsFrom(gameField);

		GoalDetector goalDetector = new GoalDetector();

		assertThat(goalDetector.isThereAGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2), is(false));

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

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2),
				is("on the right"));

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

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2),
				is("on the left"));

	}

	@Ignore // TODO: try to figure out how to detect this kind of shots the correct way
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

		assertThat(goalDetector.whereHappendTheGoal(ballPosition_t, ballPosition_tMinus1, ballPosition_tMinus2),
				is("on the right"));

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

}
