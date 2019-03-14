package fiduciagad.de.sft.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.goaldetector.GoalDetector;

public class Game {

	private boolean gameIsAlive = false;

	private Team teamOne = new Team();
	private Team teamTwo = new Team();
	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();
	private GoalDetector goalDetector;
	private OpenCVHandler gameDetection = new OpenCVHandler();
	private OpenCVHandler colorHandler = new OpenCVHandler();

	public void startTheDetection() throws MqttSecurityException, MqttException {

		colorHandler.startPythonModule();
		colorHandler.startTheAdjustment();

		gameDetection.startPythonModule();
		List<String> ballPositionsAsStrings = gameDetection.getOpenCVOutputAsList();

		for (String ballPositionAsString : ballPositionsAsStrings) {
			BallPositionHandler ballPositionHandler = new BallPositionHandler();

			BallPosition ballposition = ballPositionHandler.createBallPositionFrom(ballPositionAsString);

			ballPositions.add(ballposition);

		}

		BallPosition ballPosMinusTwo;
		BallPosition ballPosMinusOne;
		BallPosition ballPos;
		// Setup setup = new Setup("localhost", 1883);
		// setup.sendScore("0-0");

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
				// setup.sendScore(getScoreAsString());
			}

		}

		stop();

	}

	public void setGameDetection(OpenCVHandler gameDetection) {
		this.gameDetection = gameDetection;
	}

	public void setColorHandler(OpenCVHandler colorHandler) {
		this.colorHandler = colorHandler;
	}

	public Boolean isOngoing() {
		return gameIsAlive;
	}

	public void stop() {
		gameIsAlive = false;
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
