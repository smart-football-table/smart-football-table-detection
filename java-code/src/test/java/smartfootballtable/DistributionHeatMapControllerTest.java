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

}
