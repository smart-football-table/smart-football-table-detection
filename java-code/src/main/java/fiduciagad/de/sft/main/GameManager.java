package fiduciagad.de.sft.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import fiduciagad.de.sft.ballvelocity.BallVelocityCalculator;
import fiduciagad.de.sft.foul.FoulChecker;
import fiduciagad.de.sft.goaldetector.GoalDetector;
import fiduciagad.de.sft.mqtt.MqttSystem;

public class GameManager {
	
	private static class Message {
		private String topic;
		private String payload;

		private Message(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}

	}

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
	private boolean gameOver;
	private Queue<Message> receivedMessages = new ConcurrentLinkedQueue<>();

	public GameManager() throws MqttSecurityException, MqttException {
		mqtt = new MqttSystem("localhost", 1883, new MqttCallback() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				receivedMessages.add(new Message(topic, new String(message.getPayload())));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub

			}
		});
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
		
		Message receivedMessage = receivedMessages.poll();
		if (receivedMessage != null) {
			if (receivedMessage.topic.equals("game/reset")) {
				resetGame();
			}
		}

		if (ballPositions.get(ballPositions.size() - 1).getXCoordinate() != -1) {
			mqtt.sendPostion(ballPositions.get(ballPositions.size() - 1));
		}

		positionsSinceLastVelocity++;
		positionsSinceLastFoul++;

		BallPosition actualBallPOs = ballPositions.get(ballPositions.size() - 1);
		boolean ballPositionNotSet = actualBallPOs.getXCoordinate() == -1 && actualBallPOs.getYCoordinate() == -1;

		if (!ballPositionNotSet) {
			if (ballPositionOne != null && ballPositionTwo == null && positionsSinceLastVelocity > 5) {
				ballPositionTwo = actualBallPOs;
				calculateVelocity = true;
				System.out.println("" + ballPositionTwo.getXCoordinate() + "|" + ballPositionTwo.getYCoordinate());
			}
			if (ballPositionOne == null) {
				ballPositionOne = actualBallPOs;
				System.out.println("" + ballPositionOne.getXCoordinate() + "|" + ballPositionOne.getYCoordinate());
			}

		}

		if (calculateVelocity) {
			calculateVelocity = false;
			// positionsSinceLastVelocity = 0;

			double velocity = velocityCalculator.getVelocityOfBallInKilometerPerHour(ballPositionOne, ballPositionTwo);
			velocityCalculator.getVelocityValues().add(Math.abs(velocity));
			System.out.println(velocity + " ungerundet");
			velocity = Math.round(velocity * 100.0) / 100.0;
			System.out.println(velocity + " gerundet");

			if (velocity == 0.0) {
				velocity = oldVelocity;
			} else {
				oldVelocity = velocity;
			}
			if (velocity < 70) {

				mqtt.sendVelocity(Math.abs(velocity));
			}
			//
			// double velocityms =
			// velocityCalculator.getVelocityOfBallInMeterPerSecond(ballPositionOne,
			// ballPositionTwo);
			// velocityms = Math.round(velocityms * 100.0) / 100.0;
			// mqtt.sendVelocityMS(Math.abs(velocityms));

			// double velocityAverage = velocityCalculator.getVelocityAverage();
			// velocityAverage = Math.round(velocityAverage * 100.0) / 100.0;
			// // mqtt.sendVelocity(velocityAverage);
			// // mqtt.sendVelocity(velocityms);
			// velocityCalculator.setVelocityList(new ArrayList<Double>());

			ballPositionOne = null;
			ballPositionTwo = null;

		}

		// if (positionsSinceLastVelocity > 15) {
		// positionsSinceLastVelocity = 0;
		// double velocity = velocityCalculator.getVelocityOfBallInKilometerPerHour(
		// ballPositions.get(ballPositions.size() - 2),
		// ballPositions.get(ballPositions.size() - 1));
		//
		// velocity = Math.round(velocity * 100.0) / 100.0;
		// System.out.println(velocity);
		// mqtt.sendVelocity(Math.abs(velocity));
		// }

		if (ballPositions.size() > 50) {

			if (goalDetector.isThereAGoal(ballPositions)) {

				String atPostion = goalDetector.whereHappendTheGoal(ballPositions.get(ballPositions.size() - 51),
						ballPositions.get(ballPositions.size() - 52));
				setGoalForTeamWhenGoalHappend(atPostion);
				if (!gameOver) {
					mqtt.sendScore(getScoreAsString());
				}

			}
		}

		if (gameOver && goalDetector.isBallWasInMidArea()) {
			resetGame();
			mqtt.sendScore("0-0");
			mqtt.sendGameStart();
			gameOver = false;
		}

		if (positionsSinceLastFoul > 300) {
			positionsSinceLastFoul = 0;
			if (foulChecker.isThereAFoul(ballPositions)) {
				mqtt.sendFoul();
			}
		}

		if (!gameOver) {
			if (teamOne.getScore() == 6) {
				mqtt.sendGameOver("0");
				gameOver = true;
			}
			if (teamTwo.getScore() == 6) {
				mqtt.sendGameOver("1");
				gameOver = true;
			}
			if (teamOne.getScore() == 5 && teamTwo.getScore() == 5) {
				mqtt.sendGameOver("1");
				gameOver = true;
			}
		}

	}

	private void resetGame() throws MqttPersistenceException, MqttException {
		ballPositions = new ArrayList<BallPosition>();
		teamOne.setScore(0);
		teamTwo.setScore(0);
	}

	public void createBallPosition(String lineBefore, String line) {
		ballPositionHandler.setGoalDetector(goalDetector);
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
