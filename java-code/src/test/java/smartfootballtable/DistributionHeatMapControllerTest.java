package smartfootballtable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DistributionHeatMapControllerTest {

	@Test
	public void definingFieldGranularityAsOne_shouldHaveOneField() {

		DistributionHeatMapController distController = new DistributionHeatMapController();
		
		distController.configureFieldGranularity(1);
		
		assertThat(distController.getFields().size(), is(1));
	}
	
	@Test
	public void definingFieldGranularityAsTwo_shouldHaveTwoField() {

		DistributionHeatMapController distController = new DistributionHeatMapController();
		
		distController.configureFieldGranularity(2);
		
		assertThat(distController.getFields().size(), is(2));
	}
	
	@Test
	public void settingOnePositionInField_shouldHaveDistributionOfOneHundredPercent() {

		DistributionHeatMapController distController = new DistributionHeatMapController();
		
		distController.configureFieldGranularity(1);
		
		distController.addPositionToField(1);
		
		assertThat(distController.getDistributionInPercentOfField(1), is(100.0));
	}
	
	@Test
	public void settingOnePositionInTwoFields_shouldHaveDistributionOfFiftyPercentInEachField() {

		DistributionHeatMapController distController = new DistributionHeatMapController();
		
		distController.configureFieldGranularity(2);
		
		distController.addPositionToField(1);
		distController.addPositionToField(2);
		
		assertThat(distController.getDistributionInPercentOfField(1), is(50.0));
		assertThat(distController.getDistributionInPercentOfField(2), is(50.0));
	}

}
