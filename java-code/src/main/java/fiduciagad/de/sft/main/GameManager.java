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
	private int positionsSinceLastVelocity = 0;
	private int positionsSinceLastFoul;
	private BallPosition ballPositionOne = null;
	private BallPosition ballPositionTwo = null;
	private boolean calculateVelocity = false;
	private double oldVelocity;

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

		sendPositionIfThereIsOne();

		positionsSinceLastVelocity++;
		positionsSinceLastFoul++;

		BallPosition actualBallPos = ballPositions.get(ballPositions.size() - 1);
		boolean ballPositionNotSet = actualBallPos.getXCoordinate() == -1 && actualBallPos.getYCoordinate() == -1;

		if (!ballPositionNotSet) {
			if (ballPositionOne != null && ballPositionTwo == null && positionsSinceLastVelocity > 5) {
				ballPositionTwo = actualBallPos;
				calculateVelocity = true;
			}
			if (ballPositionOne == null) {
				ballPositionOne = actualBallPos;
			}

		}
		if (calculateVelocity) {
			calculateVelocity = false;

			double velocity = velocityCalculator.getVelocityOfBallInKilometerPerHour(ballPositionOne, ballPositionTwo);
			velocityCalculator.getVelocityValues().add(Math.abs(velocity));
			velocity = Math.round(velocity * 100.0) / 100.0;

			if (velocity == 0.0 || velocity > 70) {
				velocity = oldVelocity;
			} else {
				oldVelocity = velocity;
			}

			mqtt.sendVelocity(Math.abs(velocity));

			ballPositionOne = null;
			ballPositionTwo = null;

		}

		if (ballPositions.size() > 50)

		{

			if (goalDetector.isThereAGoal(ballPositions)) {

				String atPostion = goalDetector.whereHappendTheGoal(ballPositions.get(ballPositions.size() - 51),
						ballPositions.get(ballPositions.size() - 52));
				setGoalForTeamWhenGoalHappend(atPostion);
				mqtt.sendScore(getScoreAsString());

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
			resetGame();
		}
		if (teamTwo.getScore() == 6) {
			mqtt.sendGameOver("1");
			resetGame();
		}
		if (teamOne.getScore() == 5 && teamTwo.getScore() == 5) {
			mqtt.sendGameOver("0,1");
			resetGame();
		}

	}

	private void sendPositionIfThereIsOne() throws MqttPersistenceException, MqttException {
		if (ballPositions.get(ballPositions.size() - 1).getXCoordinate() != -1) {
			mqtt.sendPostion(ballPositions.get(ballPositions.size() - 1));
		}
	}

	private void resetGame() throws MqttPersistenceException, MqttException {
		ballPositions = new ArrayList<BallPosition>();
		teamOne.setScore(0);
		teamTwo.setScore(0);
		mqtt.sendScore("0-0");
		mqtt.sendGameStart();
	}

	public void createBallPosition(String line) {
		ballPositionHandler.setGoalDetector(goalDetector);
		BallPosition ballposition = ballPositionHandler.createBallPositionFrom(line);
		ballPositions.add(ballposition);

	}

	public List<BallPosition> getBallPositions() {
		return ballPositions;
	}

	public void setBallPositions(List<BallPosition> ballPositions) {
		this.ballPositions = ballPositions;
	}

}
