package smartfootballtable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class BallPositionHandlerTest {

	@Test
	public void valueFromStringShouldBeTransferedCorrectlyIntoInteger() {

		String string = "100";

		BallPositionHandler positionHandler = new BallPositionHandler();

		assertThat(positionHandler.getValueFromString(string), is(100));
	}
	
	@Test
	public void positionAsStringShouldBeTransferedCorrectlyIntoModel() {

		String string = "1546885301.664638|100|200";

		BallPositionHandler positionHandler = new BallPositionHandler();
		
		BallPosition ballPosition = positionHandler.createBallPositionFrom(string);

		assertThat(ballPosition.getXCoordinate(), is(100));
		assertThat(ballPosition.getYCoordinate(), is(200));
		assertThat(ballPosition.getTimepoint(), is(1546885301.664638));
	}
	
	@Test
	public void anotherPositionAsStringShouldBeTransferedCorrectlyIntoModel() {

		String string = "123523.2348|999|111";

		BallPositionHandler positionHandler = new BallPositionHandler();
		
		BallPosition ballPosition = positionHandler.createBallPositionFrom(string);

		assertThat(ballPosition.getXCoordinate(), is(999));
		assertThat(ballPosition.getYCoordinate(), is(111));
		assertThat(ballPosition.getTimepoint(), is(123523.2348));
	}

}
