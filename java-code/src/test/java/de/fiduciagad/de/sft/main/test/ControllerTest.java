package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;
import fiduciagad.de.sft.main.Controller;
import fiduciagad.de.sft.main.OpenCVHandler;

public class ControllerTest {

	@Test
	public void startAndStopAGame() {

		Controller game = new Controller();

		game.startGameDetection();

		assertThat(game.isOngoing(), is(true));

		game.stopGameDetection();

		assertThat(game.isOngoing(), is(false));
	}

	@Test
	public void gameStopsWhenDetectionEnds() throws MqttSecurityException, MqttException, IOException {

		Controller game = new Controller();

		OpenCVHandler cv = new OpenCVHandler();

		ConfiguratorValues.setDefaultColorRangeYellow();

		cv.setPythonModule("src/main/resources/testCase_playedGameDigitizerWithoutBall.py");

		game.setGameDetection(cv);
		game.startTheDetection();

		assertThat(game.isOngoing(), is(false));

	}

	@Ignore
	@Test
	public void readRealExampleTestVideoWithNoBallAndGetBallPositions()
			throws MqttSecurityException, MqttException, IOException {

		OpenCVHandler opencv = new OpenCVHandler();

		Controller controller = new Controller();
		controller.startGameDetection();

		opencv.setPythonModule("testCase_playedGameDigitizerWithoutBall.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(controller);

		BallPosition positionOne = opencv.getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(-1));
		assertThat(positionOne.getYCoordinate(), is(-1));

	}

	@Ignore
	@Test
	public void readRealExampleTestVideoWithBall() throws MqttSecurityException, MqttException, IOException {

		OpenCVHandler opencv = new OpenCVHandler();

		Controller controller = new Controller();
		controller.startGameDetection();

		opencv.setPythonModule("testCase_playedGameDigitizerWithBall.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(controller);

		BallPosition positionOne = opencv.getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(33));
		assertThat(positionOne.getYCoordinate(), is(33));

	}

	@Ignore
	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException, IOException {

		Controller controller = new Controller();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithBallAndGoal.py");

		controller.setGameDetection(cv);
		controller.startTheDetection();

		cv.handleWithOpenCVOutput(controller);

		assertThat(cv.getManager().getScoreAsString(), is("1-0"));
	}

}
