package fiduciagad.de.sft.goaldetector;

import de.fiduciagad.de.sft.test.FootballTable;
import fiduciagad.de.sft.main.BallPosition;

public class GoalDetector {

	public boolean isThereAGoal(BallPosition ballPosition_t, BallPosition ballPosition_tMinus1,
			BallPosition ballPosition_tMinus2) {

		if (ballWasNotInFrontOfAGoal(ballPosition_tMinus1)) {
			return false;
		}

		if (newestBallPositionIsNotInGameField(ballPosition_t)) {
			return true;
		}

		return false;
	}

	private boolean newestBallPositionIsNotInGameField(BallPosition ballPosition_t) {
		return ballPosition_t.getXCoordinate() == -1 && ballPosition_t.getYCoordinate() == -1;
	}

	private boolean ballWasNotInFrontOfAGoal(BallPosition ballPosition_tMinus1) {

		boolean ballWasNotInFrontOfAGoal;

		double twentyPercentOfXMaxOfGameField = FootballTable.getXMaxOfGameField() * 0.2;
		double xStartOfAreaWhereBallIsntBeforeAGoal = twentyPercentOfXMaxOfGameField;
		double xEndOfAreaWhereBallIsntBeforeAGoal = FootballTable.getXMaxOfGameField() - twentyPercentOfXMaxOfGameField;

		ballWasNotInFrontOfAGoal = xStartOfAreaWhereBallIsntBeforeAGoal < ballPosition_tMinus1.getXCoordinate()
				&& ballPosition_tMinus1.getXCoordinate() < xEndOfAreaWhereBallIsntBeforeAGoal;

		return ballWasNotInFrontOfAGoal;
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
