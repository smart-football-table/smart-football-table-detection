package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.BallPositionHandler;
import fiduciagad.de.sft.main.OpenCVHandler;

public class OpenCVHandlerTest {

	@Test
	public void readSimpleConsole() {

		OpenCVHandler opencv = new OpenCVHandler();

		opencv.startPythonModule("test.py");

		List<String> output = opencv.getOpenCVOutputAsList();

		assertThat(output.size(), CoreMatchers.is(9));
		assertThat(output.get(0), CoreMatchers.is("1"));

	}

	@Test
	public void readRealExampleTestVideoWithNoBallAndGetBallPositions() {

		OpenCVHandler opencv = new OpenCVHandler();

		opencv.startPythonModule("testCase_playedGameDigitizerWithoutBall.py");

		List<String> output = opencv.getOpenCVOutputAsList();

		BallPositionHandler ballPositionHandler = new BallPositionHandler();

		BallPosition positionOne = ballPositionHandler.createBallPositionFrom(output.get(0));

		assertThat(positionOne.getXCoordinate(), is(-1));
		assertThat(positionOne.getYCoordinate(), is(-1));

	}

	@Test
	public void readRealExampleTestVideoWithBall() {

		OpenCVHandler opencv = new OpenCVHandler();

		opencv.startPythonModule("testCase_playedGameDigitizerWithBall.py");

		List<String> output = opencv.getOpenCVOutputAsList();

		BallPositionHandler ballPositionHandler = new BallPositionHandler();

		BallPosition positionOne = ballPositionHandler.createBallPositionFrom(output.get(0));

		assertThat(positionOne.getXCoordinate(), is(33));
		assertThat(positionOne.getYCoordinate(), is(33));

	}

}
