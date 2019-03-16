package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.Controller;
import fiduciagad.de.sft.main.GameManager;
import fiduciagad.de.sft.main.OpenCVHandler;

public class ControllerTest {

	@Test
	public void startAndStopAGame() {

		Controller game = new Controller();

		game.start();

		assertThat(game.isOngoing(), is(true));

		game.stop();

		assertThat(game.isOngoing(), is(false));
	}

	@Test
	public void gameStopsWhenDetectionEnds() throws MqttSecurityException, MqttException {

		Controller game = new Controller();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithoutBall.py");

		game.setGameDetection(cv);
		game.startTheDetection();

		assertThat(game.isOngoing(), is(false));

	}

	@Ignore
	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException {

		Controller controller = new Controller();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithBallAndGoal.py");

		controller.setGameDetection(cv);
		controller.startTheDetection();

		// assertThat(controller.getScoreAsString(), is("1-0"));
	}

}
