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

		BallPosition ballPosition = positionHandler.createBallPositionFrom(
				camera + "|" + timepoint + "|" + xCoordinate + "|" + yCoordinate,
				camera + "|" + timepoint + "|" + xCoordinate + "|" + yCoordinate);

		assertThat(ballPosition.getXCoordinate(), is(100));
		assertThat(ballPosition.getYCoordinate(), is(200));

		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("154166463822"));
	}

	private String anyTimepoint() {
		return "1541664638.22";
	}

	@Test
	public void onlyPositionOneDetectedAsStringShouldBeTransferedCorrectlyIntoBallPosition() {

		String string = "1|1235232348.00|999|111";
		String string2 = "2|1235232348.00|-1|-1";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(999));
		assertThat(ballPosition.getYCoordinate(), is(111));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

	@Test
	public void onlyPositionTwoDetectedAsStringShouldBeTransferedCorrectlyIntoBallPosition() {

		String string = "1|1235232348.00|-1|-1";
		String string2 = "2|1235232348.00|1000|1000";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(1000));
		assertThat(ballPosition.getYCoordinate(), is(1000));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

	@Test
	public void createCorrectBallPositionWithOffsetForCameraOne() {

		String string = "1|1235232348.00|10|10";
		String string2 = "2|1235232348.00|-1|-1";

		BallPositionHandler positionHandler = new BallPositionHandler();

		ConfiguratorValues.setxOffsetCameraOne(100);
		ConfiguratorValues.setyOffsetCameraOne(100);

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(110));
		assertThat(ballPosition.getYCoordinate(), is(110));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

	@Test
	public void createCorrectBallPositionWithNegativeOffsetForCameraOne() {

		String string = "1|1235232348.00|10|10";
		String string2 = "2|1235232348.00|-1|-1";

		BallPositionHandler positionHandler = new BallPositionHandler();

		ConfiguratorValues.setxOffsetCameraOne(-5);
		ConfiguratorValues.setyOffsetCameraOne(-5);

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(5));
		assertThat(ballPosition.getYCoordinate(), is(5));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

	@Test
	public void combineTwoGottenOutputsTheCorrectWay() {

		String string = "1|1235232348.00|-1|-1";
		String string2 = "2|1235232348.00|-1|-1";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(-1));
		assertThat(ballPosition.getYCoordinate(), is(-1));
		assertThat(String.valueOf(ballPosition.getTimepoint().getTime()), is("123523234800"));
	}

	@Test
	public void ballPositionValuesGetSetCorrectlyWithOffset() {

		ConfiguratorValues.setOffsetX(50);
		ConfiguratorValues.setOffsetY(100);

		String string = "1|1235232348.00|100|200";
		String string2 = "2|1235232348.00|100|200";

		BallPositionHandler positionHandler = new BallPositionHandler();

		BallPosition ballPosition = positionHandler.createBallPositionFrom(string, string2);

		assertThat(ballPosition.getXCoordinate(), is(50));
		assertThat(ballPosition.getYCoordinate(), is(100));

	}

}
