package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.BallPositionHandler;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class BallPositionHandlerTest {

	@Before
	public void init() {
		ConfiguratorValues.setxOffsetCameraOne(0);
		ConfiguratorValues.setyOffsetCameraOne(0);

		ConfiguratorValues.setxOffsetCameraTwo(0);
		ConfiguratorValues.setyOffsetCameraTwo(0);

		ConfiguratorValues.setOffsetX(0);
		ConfiguratorValues.setOffsetY(0);
	}

	@Test
	public void valueFromStringShouldBeTransferedCorrectlyIntoInteger() {

		String string = "100";

		BallPositionHandler positionHandler = new BallPositionHandler();

		assertThat(positionHandler.getValueFromString(string), is(100));
	}

	@Test
	public void ballPositionValuesGetNormedCorrectly() {

		BallPosition ballPosition = new BallPosition();

		ballPosition.setXCoordinate(100);
		ballPosition.setYCoordinate(500);
		ballPosition.setTimepoint(1541664638);

		ConfiguratorValues.setGameFieldSize(1000, 1000);

		assertThat(ballPosition.normedXPosition(), is(0.1));
		assertThat(ballPosition.normedYPosition(), is(0.5));
	}

	@Test
	public void twoIdenticalpositionsAsStringShouldBeTransferedCorrectlyIntoBallPosition() {

		int camera = 1;
		int xCoordinate = 100;
		int yCoordinate = 200;
		String timepoint = anyTimepoint();

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler
				.createBallPositionFrom(camera + "|" + timepoint + "|" + xCoordinate + "|" + yCoordinate);

		assertThat(ballPosition.getXCoordinate(), is(100));
		assertThat(ballPosition.getYCoordinate(), is(200));

		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("154166463822"));
	}

	private String anyTimepoint() {
		return "1541664638.22";
	}

	@Test
	public void ballPositionValuesGetSetCorrectlyWithOffset() {

		ConfiguratorValues.setOffsetX(50);
		ConfiguratorValues.setOffsetY(100);

		String string = "1|1235232348.00|100|200";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string);

		assertThat(ballPosition.getXCoordinate(), is(50));
		assertThat(ballPosition.getYCoordinate(), is(100));

	}

}
