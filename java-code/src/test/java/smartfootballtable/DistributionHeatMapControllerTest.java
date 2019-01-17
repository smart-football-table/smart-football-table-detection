package smartfootballtable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DistributionHeatMapControllerTest {

	@Test
	public void settingOnePositionInField_shouldHaveDistributionOfOneHundredPercent() {

		DistributionHeatMapController distController = new DistributionHeatMapController();

		distController.addPositionToField(1);

		assertThat(distController.getDistributionInPercentOfField(1), is(100.0));
	}

	@Test
	public void settingOnePositionInTwoFields_shouldHaveDistributionOfFiftyPercentInEachField() {

		DistributionHeatMapController distController = new DistributionHeatMapController();

		distController.addPositionToField(1);
		distController.addPositionToField(2);

		assertThat(distController.getDistributionInPercentOfField(1), is(50.0));
		assertThat(distController.getDistributionInPercentOfField(2), is(50.0));
	}

}
