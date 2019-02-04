package de.fiduciagad.de.sft.test;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import fiduciagad.de.sft.ballvelocity.BallVelocityCalculator;
import fiduciagad.de.sft.main.BallPosition;

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

		assertThat(ballVelocityCaluclator.getDistanceInCentimeterBetween(position1, position2), CoreMatchers.is(10.0));

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

		
		// d = root( (Bx - Ax)² + (By - Ay)² ) 
		assertThat(ballVelocityCaluclator.getDistanceInCentimeterBetween(position1, position2),
				CoreMatchers.is(14.14));
		
	}
	
	@Test
	public void calculateCorrectVelocityWithSimpleData() {

		BallVelocityCalculator ballVelocityCaluclator = new BallVelocityCalculator();

		BallPosition position1 = new BallPosition();
		position1.setXCoordinate(0);
		position1.setYCoordinate(0);
		position1.setTimepoint(1546300800);

		BallPosition position2 = new BallPosition();
		position2.setXCoordinate(0);
		position2.setYCoordinate(100);
		position2.setTimepoint(1546300801);

		//v = s/t
		assertThat(ballVelocityCaluclator.calculateVelocityInMeterPerSecond(position1, position2),
				CoreMatchers.is(1.0));
		
		assertThat(ballVelocityCaluclator.calculateVelocityInKilometerPerHour(position1, position2),
				CoreMatchers.is(3.6));

	}
	
	

}
