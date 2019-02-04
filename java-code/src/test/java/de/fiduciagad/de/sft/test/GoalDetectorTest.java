package de.fiduciagad.de.sft.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;

public class GoalDetectorTest {

	private int xMaxOfGame = 1000;
	private int yMaxOfGame = 500;
	
	
	@Test
	public void gettingThreePositionsMidfieldIsntAGoal() {
		
		BallPosition ballPosition_t = new BallPosition(300, 0, new Date(2));
		BallPosition ballPosition_tMinus1 = new BallPosition(300, 0, new Date(1));
		BallPosition ballPosition_tMinus2 = new BallPosition(300, 0, new Date(0));
		
		GoalDetector goalDetector = new GoalDetector();
		
		assertThat(goalDetector.isThereAGoal(), is(false));
		
		
	}

}
