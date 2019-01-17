package smartfootballtable;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class HeatMapTileCalculatorTest {

	@Test
	public void definingOneTileAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 100;
		int y = 100;
		
		HeatMapTileCalculator h = new HeatMapTileCalculator();
		
		assertThat(h.calculateHeatMapId(x,y), CoreMatchers.is(1));
		
	}

}
