package fiduciagad.de.sft.main;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.ballvelocity.BallVelocityCalculator;
import fiduciagad.de.sft.foul.FoulChecker;
import fiduciagad.de.sft.goaldetector.GoalDetector;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class GameManager {
	private Team teamOne = new Team();
	private Team teamTwo = new Team();
	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();
	private GoalDetector goalDetector = new GoalDetector();
	private BallVelocityCalculator velocityCalculator = new BallVelocityCalculator();
	private FoulChecker foulChecker = new FoulChecker();

	private MqttSystem mqtt;
	private BallPositionHandler ballPositionHandler = new BallPositionHandler();
	private int positionsSinceLastVelocity = 0;
	private int positionsSinceLastFoul;

	public GameManager() throws MqttSecurityException, MqttException {
		mqtt = new MqttSystem("localhost", 1883);
	}

	public String getScoreAsString() {
		return teamOne.getScore() + "-" + teamTwo.getScore();
	}

	public void setGoalForTeamWhenGoalHappend(String atPostion) throws MqttPersistenceException, MqttException {

		if (atPostion.equals("on the right")) {
			teamOne.increaseScore();
			mqtt.sendTeamThatScored(0);
		} else {
			teamTwo.increaseScore();
			mqtt.sendTeamThatScored(1);
		}

	}

	public void doTheLogic() throws MqttPersistenceException, MqttException {

		if (ballPositions.get(ballPositions.size() - 1).getXCoordinate() != -1) {
			mqtt.sendPostion(ballPositions.get(ballPositions.size() - 1));
		}

		positionsSinceLastVelocity++;
		positionsSinceLastFoul++;

		if (positionsSinceLastVelocity > 15) {
			positionsSinceLastVelocity = 0;
			double velocity = velocityCalculator.getVelocityOfBallInKilometerPerHour(
					ballPositions.get(ballPositions.size() - 2), ballPositions.get(ballPositions.size() - 1));

			velocity = Math.round(velocity * 100.0) / 100.0;

			mqtt.sendVelocity(Math.abs(velocity));
		}

		if (ballPositions.size() > 50) {

			if (goalDetector.isThereAGoal(ballPositions)) {

				String atPostion = goalDetector.whereHappendTheGoal(ballPositions.get(ballPositions.size() - 51),
						ballPositions.get(ballPositions.size() - 52));
				setGoalForTeamWhenGoalHappend(atPostion);
				mqtt.sendPostion(getScoreAsString());
			}
		}

		if (positionsSinceLastFoul > 300) {
			positionsSinceLastFoul = 0;
			if (foulChecker.isThereAFoul(ballPositions)) {
				mqtt.sendFoul();
			}
		}

		if (teamOne.getScore() == 6) {
			mqtt.sendGameOver("0");
			ballPositions = new ArrayList<BallPosition>();
			teamOne.setScore(0);
			teamTwo.setScore(0);
			mqtt.sendGameStart();
		}
		if (teamTwo.getScore() == 6) {
			mqtt.sendGameOver("1");
			ballPositions = new ArrayList<BallPosition>();
			teamOne.setScore(0);
			teamTwo.setScore(0);
			mqtt.sendGameStart();
		}
		if (teamOne.getScore() == 5 && teamTwo.getScore() == 5) {
			mqtt.sendGameOver("1");
			ballPositions = new ArrayList<BallPosition>();
			teamOne.setScore(0);
			teamTwo.setScore(0);
			mqtt.sendGameStart();
		}

	}

	public void createBallPosition(String lineBefore, String line) {
		BallPosition ballposition = ballPositionHandler.createBallPositionFrom(lineBefore, line);
		ballPositions.add(ballposition);

	}

	public List<BallPosition> getBallPositions() {
		return ballPositions;
	}

	public void setBallPositions(List<BallPosition> ballPositions) {
		this.ballPositions = ballPositions;
	}

}
