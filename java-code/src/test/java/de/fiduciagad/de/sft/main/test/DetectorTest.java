package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.Detector;
import fiduciagad.de.sft.main.GameManager;
import fiduciagad.de.sft.main.OpenCVHandler;

public class DetectorTest {

	@Test
	public void startAndStopAGame() {

		Detector game = new Detector();

		game.start();

		assertThat(game.isOngoing(), is(true));

		game.stop();

		assertThat(game.isOngoing(), is(false));
	}

	@Ignore
	@Test
	public void gameStopsWhenDetectionEnds() throws MqttSecurityException, MqttException {

		Detector game = new Detector();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithoutBall.py");

		game.setGameDetection(cv);
		game.startTheDetection();

		assertThat(game.isOngoing(), is(false));
	}

	@Ignore
	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException {

		Detector detector = new Detector();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithBallAndGoal.py");

		detector.setGameDetection(cv);
		detector.startTheDetection();

		// assertThat(detector.getScoreAsString(), is("1-0"));
	}

}
