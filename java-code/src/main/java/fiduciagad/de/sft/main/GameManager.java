package fiduciagad.de.sft.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.goaldetector.GoalDetector;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class GameManager {
	private Team teamOne = new Team();
	private Team teamTwo = new Team();
	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();
	private GoalDetector goalDetector = new GoalDetector();

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

		if (ballPositions.size() > 50) {

			if (goalDetector.isThereAGoal(ballPositions)) {

				String atPostion = goalDetector.whereHappendTheGoal(ballPositions.get(ballPositions.size() - 51),
						ballPositions.get(ballPositions.size() - 52));
				setGoalForTeamWhenGoalHappend(atPostion);
				System.out.println(getScoreAsString());
				mqtt.sendScore(getScoreAsString());
			}
		}

	}

	public void createBallPosition(String line) {
		BallPosition ballposition = ballPositionHandler.createBallPositionFrom(line);
		ballPositions.add(ballposition);

		System.out.println(" x " + ballposition.getXCoordinate() + " y " + ballposition.getYCoordinate() + " size "
				+ ballPositions.size());

	}

	public List<BallPosition> getBallPositions() {
		return ballPositions;
	}

	public void setBallPositions(List<BallPosition> ballPositions) {
		this.ballPositions = ballPositions;
	}

}
