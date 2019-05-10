package detection.main;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Before;
import org.junit.Test;

public class ControllerTestIT {

	private Controller game;
	private OpenCVHandler cv;

	@Before
	public void init() {

		game = new Controller();
		cv = new OpenCVHandler();
		cv.setPythonModule("../../../../alexeyab/darknet/darknet_video.py");
		ConfiguratorValues.setFramesPerSecond(10);

	}

	@Test
	public void startAndStopAGame() {

		Controller game = new Controller();

		game.startGameDetection();

		assertThat(game.isOngoing(), is(true));

		game.stopGameDetection();

		assertThat(game.isOngoing(), is(false));
	}

	@Test
	public void readRealExampleTestVideoWithNoBallAndGetBallPositions()
			throws MqttSecurityException, MqttException, IOException {

		String videoArgument = "src/main/resources/videos/testVid_noBall.avi";
		cv.setPythonArgumentVideoPath(videoArgument);

		game.setGameDetection(cv);
		game.startTheDetection();

		BallPosition positionOne = game.getGameDetection().getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(-1));
		assertThat(positionOne.getYCoordinate(), is(-1));

	}

	@Test
	public void readRealExampleTestVideoWithBall() throws MqttSecurityException, MqttException, IOException {

		String videoArgument = "src/main/resources/videos/testVid_ballUpperLeft.avi";
		cv.setPythonArgumentVideoPath(videoArgument);

		game.setGameDetection(cv);
		game.startTheDetection();

		BallPosition positionOne = game.getGameDetection().getManager().getBallPositions().get(0);

		boolean ballIsInUpperLeftCorner = positionOne.getXCoordinate() < 50 && positionOne.getYCoordinate() < 50;

		assertThat(ballIsInUpperLeftCorner, is(true));

	}

	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException, IOException {

		String videoArgument = "src/main/resources/videos/testVid_ballFromLeftToRight.avi";
		cv.setPythonArgumentVideoPath(videoArgument);

		game.setGameDetection(cv);
		game.startTheDetection();

		assertThat(cv.getManager().getScoreAsString(), is("1-0"));
	}

	@Test
	public void canDetectASpecificScoreFromTestVideo() throws MqttSecurityException, MqttException, IOException {

		String videoArgument = "src/main/resources/videos/full_someGameplay.avi";
		cv.setPythonArgumentVideoPath(videoArgument);

		ConfiguratorValues.setFramesPerSecond(25);

		game.setGameDetection(cv);
		game.startTheDetection();

		assertThat(cv.getManager().getScoreAsString(), is("3-0"));
	}

}
