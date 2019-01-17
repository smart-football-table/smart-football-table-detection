package smartfootballtable;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class BallVelocityCalculatorTest {

	private int millimeterPerPixel = 10;

	@Test
	public void getCorrectVerticalDistanceBetweenTwoPositions() {

		BallVelocityCalculator ballVelocityCaluclator = new BallVelocityCalculator();

		BallPosition position1 = new BallPosition();
		position1.setXCoordinate(0);
		position1.setYCoordinate(0);

		BallPosition position2 = new BallPosition();
		position2.setXCoordinate(10);
		position2.setYCoordinate(0);

		assertThat(ballVelocityCaluclator.getDistanceInMillimeterBetween(position1, position2), CoreMatchers.is(100.0));

	}

	@Test
	public void getCorrectDiagonalDistanceBetweenTwoPositions() {

		BallVelocityCalculator ballVelocityCaluclator = new BallVelocityCalculator();

		BallPosition position1 = new BallPosition();
		position1.setXCoordinate(0);
		position1.setYCoordinate(0);

		BallPosition position2 = new BallPosition();
		position2.setXCoordinate(10);
		position2.setYCoordinate(10);

		assertThat(ballVelocityCaluclator.getDistanceInMillimeterBetween(position1, position2),
				CoreMatchers.is(141.42));

	}

}
