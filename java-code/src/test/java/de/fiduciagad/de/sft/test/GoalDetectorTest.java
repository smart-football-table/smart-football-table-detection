package de.fiduciagad.de.sft.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;

public class GoalDetectorTest {

	private int xMaxOfGame = 1000;
	private int yMaxOfGame = 500;

	BallPosition ballPosition_t;
	BallPosition ballPosition_tMinus1;
	BallPosition ballPosition_tMinus2;

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

	}

}
