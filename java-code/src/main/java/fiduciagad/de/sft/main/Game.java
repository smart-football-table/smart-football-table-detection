package fiduciagad.de.sft.main;

import java.util.ArrayList;
import java.util.List;

import de.fiduciagad.de.sft.main.test.Team;
import fiduciagad.de.sft.goaldetector.GoalDetector;

public class Game {

	private boolean gameIsAlive = false;
	private String pythonModule = "";
	private Team teamOne = new Team();
	private Team teamTwo = new Team();
	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();
	private GoalDetector goalDetector;

	public void startTheDetection() {

		OpenCVHandler opencv = new OpenCVHandler();

		opencv.startPythonModule(pythonModule);

		List<String> ballPositionsAsStrings = opencv.getOpenCVOutputAsList();

		for (String ballPositionAsString : ballPositionsAsStrings) {
			BallPositionHandler ballPositionHandler = new BallPositionHandler();

			BallPosition ballposition = ballPositionHandler.createBallPositionFrom(ballPositionAsString);

			ballPositions.add(ballposition);

		}

		BallPosition ballPosMinusTwo;
		BallPosition ballPosMinusOne;
		BallPosition ballPos;

		for (int i = 2; i < ballPositions.size(); i++) {

			ballPosMinusTwo = ballPositions.get(i - 2);
			ballPosMinusOne = ballPositions.get(i - 1);
			ballPos = ballPositions.get(i);

			goalDetector = new GoalDetector();

			if (goalDetector.isThereAGoal(ballPos, ballPosMinusOne, ballPosMinusTwo)) {

				String where = goalDetector.whereHappendTheGoal(ballPos, ballPosMinusOne, ballPosMinusTwo);
				if (where.equals("on the right")) {
					teamOne.increaseScore();
				} else {
					teamTwo.increaseScore();
				}

			}

		}

		stop();

	}

	public Boolean isOngoing() {
		return gameIsAlive;
	}

	public void stop() {
		gameIsAlive = false;
	}

	public void setPythonModule(String string) {
		pythonModule = string;
	}

	public void start() {
		gameIsAlive = true;
	}

	public String getScoreAsString() {
		return teamOne.getScore() + "-" + teamTwo.getScore();
	}

	public void setGoalForTeam(int i) {

		if (i == 1) {
			teamOne.increaseScore();
		} else {
			teamTwo.increaseScore();
		}

	}

}
