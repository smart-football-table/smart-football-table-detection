package fiduciagad.de.sft.goaldetector;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class GoalDetector {

	public boolean isThereAGoal(BallPosition ballPosition_t, BallPosition ballPosition_tMinus1,
			BallPosition ballPosition_tMinus2) {

		if (ballWasNotInFrontOfAGoal(ballPosition_tMinus1)) {
			return false;
		}

		if (newestBallPositionIsNotInGameField(ballPosition_t)) {

			if (theOtherPositionsAreAlsoNotInGameField(ballPosition_tMinus1, ballPosition_tMinus2)) {
				return false;
			}
			return true;
		}

		return false;
	}

	private boolean theOtherPositionsAreAlsoNotInGameField(BallPosition ballPosition_tMinus1,
			BallPosition ballPosition_tMinus2) {

		boolean pos1NotInField = ballPosition_tMinus1.getXCoordinate() == -1
				&& ballPosition_tMinus1.getYCoordinate() == -1;
		boolean pos2NotInField = ballPosition_tMinus2.getXCoordinate() == -1
				&& ballPosition_tMinus2.getYCoordinate() == -1;
		return pos1NotInField && pos2NotInField;
	}

	private boolean newestBallPositionIsNotInGameField(BallPosition ballPosition_t) {
		return ballPosition_t.getXCoordinate() == -1 && ballPosition_t.getYCoordinate() == -1;
	}

	private boolean ballWasNotInFrontOfAGoal(BallPosition ballPosition_tMinus1) {

		boolean ballWasNotInFrontOfAGoal;

		double twentyPercentOfXMaxOfGameField = ConfiguratorValues.getXMaxOfGameField() * 0.2;
		double xStartOfAreaWhereBallIsntBeforeAGoal = twentyPercentOfXMaxOfGameField;
		double xEndOfAreaWhereBallIsntBeforeAGoal = ConfiguratorValues.getXMaxOfGameField()
				- twentyPercentOfXMaxOfGameField;

		ballWasNotInFrontOfAGoal = xStartOfAreaWhereBallIsntBeforeAGoal < ballPosition_tMinus1.getXCoordinate()
				&& ballPosition_tMinus1.getXCoordinate() < xEndOfAreaWhereBallIsntBeforeAGoal;

		boolean ballWasNotInField = ballPosition_tMinus1.getXCoordinate() == -1
				&& ballPosition_tMinus1.getYCoordinate() == -1;

		return ballWasNotInFrontOfAGoal || ballWasNotInField;
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
