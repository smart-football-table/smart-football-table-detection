package fiduciagad.de.sft.main;

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

	public GameManager() throws MqttSecurityException, MqttException {
		mqtt = new MqttSystem("localhost", 1883);
	}

	public String getScoreAsString() {
		return teamOne.getScore() + "-" + teamTwo.getScore();
	}

	public void setGoalForTeamWhenGoalHappend(String atPostion) {

		if (atPostion.equals("on the right")) {
			teamOne.increaseScore();
		} else {
			teamTwo.increaseScore();
		}

	}

	public void doTheLogic() throws MqttPersistenceException, MqttException {

		mqtt.sendPostion(ballPositions.get(ballPositions.size()-1));

		if (ballPositions.size() > 2) {
			double velocity = velocityCalculator.getVelocityOfBallInKilometerPerHour(
					ballPositions.get(ballPositions.size() - 2), ballPositions.get(ballPositions.size() - 1));
			// here send velocity
		}

		if (ballPositions.size() > 50) {

			if (goalDetector.isThereAGoal(ballPositions)) {

				String atPostion = goalDetector.whereHappendTheGoal(ballPositions.get(ballPositions.size() - 51),
						ballPositions.get(ballPositions.size() - 52));
				setGoalForTeamWhenGoalHappend(atPostion);
				mqtt.sendPostion(getScoreAsString());
			}
		}

		if (ballPositions.size() > 300) {

			if (foulChecker.isThereAFoul(ballPositions)) {
				mqtt.sendFoul();
			}
		}

		if (teamOne.getScore() == 6) {
			mqtt.sendGameOver("0");
			ballPositions = new ArrayList<BallPosition>();
			teamOne.setScore(0);
			teamTwo.setScore(0);
		}
		if (teamTwo.getScore() == 6) {
			mqtt.sendGameOver("1");
			ballPositions = new ArrayList<BallPosition>();
			teamOne.setScore(0);
			teamTwo.setScore(0);
		}

	}

	public void createBallPosition(String line) {
		BallPosition ballposition = ballPositionHandler.createBallPositionFrom(line);
		ballPositions.add(ballposition);

		// System.out.println(" x " + ballposition.getXCoordinate() + " y " +
		// ballposition.getYCoordinate() + " time "
		// + ballposition.getTimepoint().getTime() + " size " + ballPositions.size());

	}

	public List<BallPosition> getBallPositions() {
		return ballPositions;
	}

	public void setBallPositions(List<BallPosition> ballPositions) {
		this.ballPositions = ballPositions;
	}

}
