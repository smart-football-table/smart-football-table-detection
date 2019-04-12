package de.fiduciagad.de.sft.foul.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fiduciagad.de.sft.foul.FoulChecker;
import fiduciagad.de.sft.main.BallPosition;
import fiduciagad.de.sft.main.ConfiguratorValues;

public class FoulCheckerTest {

	private List<BallPosition> ballPositions = new ArrayList<BallPosition>();

	@Before
	public void initialize() {

		ConfiguratorValues.setFramesPerSecond(30);

		createRealBallPositionsForLastTenSeconds();

	}

	@Test
	public void gettingMoreThan10SecondsNoVertcialPositionChangeTriggersAFoul() {

		FoulChecker checker = new FoulChecker();

		assertThat(checker.isThereAFoul(ballPositions), is(true));

	}

	@Test
	public void gettingOnly5SecondsNoVertcialPositionChangeDoesntTriggersAFoul() {

		FoulChecker checker = new FoulChecker();

		addAnotherBallPositionAfterFiveSecondsOnAnOtherPlace();

		assertThat(checker.isThereAFoul(ballPositions), is(false));

	}

	@Test
	public void gettingMoreThan10SecondsNoVertcialPositionChangeTriggersNotAFoulWhenNoPositionOnField() {

		FoulChecker checker = new FoulChecker();

		createEmptyBallPositionsForTheLastTenSeconds();

		assertThat(checker.isThereAFoul(ballPositions), is(false));

	}

	private void createRealBallPositionsForLastTenSeconds() {

		int countBallPositionsToBeCreated = ConfiguratorValues.getFramesPerSecond() * 10;

		for (int i = 0; i <= countBallPositionsToBeCreated; i++) {
			BallPosition position = new BallPosition(10, 10, new Date());
			ballPositions.add(position);
		}
	}

	private void createEmptyBallPositionsForTheLastTenSeconds() {
		int countBallPositionsToBeCreated = ConfiguratorValues.getFramesPerSecond() * 10;

		for (int i = 0; i <= countBallPositionsToBeCreated; i++) {
			BallPosition position = new BallPosition(-1, -1, new Date());
			ballPositions.add(position);
		}
	}

	private void addAnotherBallPositionAfterFiveSecondsOnAnOtherPlace() {
		ballPositions.add(150, new BallPosition(1000, 1000, new Date()));
	}

}
