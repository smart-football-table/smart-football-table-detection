package de.fiduciagad.de.sft.test;

import fiduciagad.de.sft.main.BallPosition;

public class GoalDetector {

	public boolean isThereAGoal(BallPosition ballPosition_t, BallPosition ballPosition_tMinus1,
			BallPosition ballPosition_tMinus2) {

		boolean newestBallPostionNotInGameField = ballPosition_t.getXCoordinate() == -1
				&& ballPosition_t.getYCoordinate() == -1;

		if (newestBallPostionNotInGameField) {
			return true;
		}

		return false;
	}

	public String whereHappendTheGoal(BallPosition ballPosition_t, BallPosition ballPosition_tMinus1,
			BallPosition ballPosition_tMinus2) {

		boolean ballMovedLeft = ballPosition_tMinus2.getXCoordinate() > ballPosition_tMinus1.getXCoordinate();
		if (ballMovedLeft) {
			return "on the left";
		} else {
			return "on the right";
		}

	}

}
