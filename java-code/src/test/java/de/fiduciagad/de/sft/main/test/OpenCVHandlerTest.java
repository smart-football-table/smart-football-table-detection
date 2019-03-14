package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.BallPositionHandler;
import fiduciagad.de.sft.main.Detector;
import fiduciagad.de.sft.main.OpenCVHandler;

public class OpenCVHandlerTest {

	@Ignore
	@Test
	public void readSimpleConsoleAndEndTheProgram() throws MqttSecurityException, MqttException {

		OpenCVHandler opencv = new OpenCVHandler();

		Detector detector = new Detector();
		detector.start();

		opencv.setPythonModule("test.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(detector);

		assertThat(detector.isOngoing(), CoreMatchers.is(false));

	}

	@Ignore
	@Test
	public void readRealExampleTestVideoWithNoBallAndGetBallPositions() throws MqttSecurityException, MqttException {

		OpenCVHandler opencv = new OpenCVHandler();

		Detector detector = new Detector();
		detector.start();

		opencv.setPythonModule("testCase_playedGameDigitizerWithoutBall.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(detector);

		BallPosition positionOne = opencv.getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(-1));
		assertThat(positionOne.getYCoordinate(), is(-1));

	}

	@Ignore
	@Test
	public void readRealExampleTestVideoWithBall() throws MqttSecurityException, MqttException {

		OpenCVHandler opencv = new OpenCVHandler();

		Detector detector = new Detector();
		detector.start();

		opencv.setPythonModule("testCase_playedGameDigitizerWithBall.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(detector);

		BallPosition positionOne = opencv.getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(33));
		assertThat(positionOne.getYCoordinate(), is(33));

	}

}
