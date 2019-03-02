package de.fiduciagad.de.sft.ballvelocity.test;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import fiduciagad.de.sft.ballvelocity.BallVelocityCalculator;
import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class BallVelocityCalculatorTest {

	private BallVelocityCalculator ballVelocityCaluclator;
	private BallPosition position1;
	private BallPosition position2;

	@Before
	public void initialize() {
		ballVelocityCaluclator = new BallVelocityCalculator();
		position1 = new BallPosition();
		position2 = new BallPosition();

		ConfiguratorValues.setMillimeterPerPixel(10);
	}

	@Test
	public void getCorrectVerticalDistanceBetweenTwoPositions() {

		position1.setXCoordinate(0);
		position1.setYCoordinate(0);

		position2.setXCoordinate(10);
		position2.setYCoordinate(0);

		assertThat(ballVelocityCaluclator.getDistanceInPixelBetween(position1, position2), CoreMatchers.is(10.0));

	}

	@Test
	public void getCorrectDiagonalDistanceBetweenTwoPositions() {

		position1.setXCoordinate(0);
		position1.setYCoordinate(0);

		position2.setXCoordinate(10);
		position2.setYCoordinate(10);

		// d = root((x2 - x1)² + (y2 - y1)²)
		assertThat(ballVelocityCaluclator.getDistanceInPixelBetween(position1, position2), CoreMatchers.is(14.14));

	}

	@Test
	public void calculateCorrectVelocityWithSimpleData() {

		position1.setXCoordinate(0);
		position1.setYCoordinate(0);
		position1.setTimepoint(1546300800);

		position2.setXCoordinate(0);
		position2.setYCoordinate(100);
		position2.setTimepoint(1546300801);

		// v = s/t
		assertThat(ballVelocityCaluclator.getVelocityOfBallInMeterPerSecond(position1, position2),
				CoreMatchers.is(1.0));

		assertThat(ballVelocityCaluclator.getVelocityOfBallInKilometerPerHour(position1, position2),
				CoreMatchers.is(3.6));

	}

	@Test
	public void calculateCorrectVelocityWithSimpleDataAndAnotherValueForMillimeterPerPixel() {

		ConfiguratorValues.setMillimeterPerPixel(1);

		position1.setXCoordinate(0);
		position1.setYCoordinate(0);
		position1.setTimepoint(1546300800);

		position2.setXCoordinate(0);
		position2.setYCoordinate(1000);
		position2.setTimepoint(1546300801);

		// v = s/t
		assertThat(ballVelocityCaluclator.getVelocityOfBallInMeterPerSecond(position1, position2),
				CoreMatchers.is(1.0));

		assertThat(ballVelocityCaluclator.getVelocityOfBallInKilometerPerHour(position1, position2),
				CoreMatchers.is(3.6));

	}

}
