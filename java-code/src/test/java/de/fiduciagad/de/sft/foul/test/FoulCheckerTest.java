package de.fiduciagad.de.sft.foul.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fiduciagad.de.sft.main.BallPosition;

public class FoulCheckerTest {

	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();

	@Before
	public void initialize() {

		for (int i = 0; i < 301; i++) {
			BallPosition position = new BallPosition(10, 10, new Date());
			ballPositions.add(position);
		}

	}

	@Test
	public void gettingMoreThan10SecondsNoVertcialPositionChangeTriggersAFoul() {

		FoulChecker checker = new FoulChecker();

		assertThat(checker.isThereAFoal(ballPositions), is(true));

	}

	@Test
	public void gettingOnly5SecondsNoVertcialPositionChangeDoesntTriggersAFoul() {

		FoulChecker checker = new FoulChecker();

		ballPositions.add(150, new BallPosition(1000, 1000, new Date()));

		assertThat(checker.isThereAFoal(ballPositions), is(false));

	}

}
