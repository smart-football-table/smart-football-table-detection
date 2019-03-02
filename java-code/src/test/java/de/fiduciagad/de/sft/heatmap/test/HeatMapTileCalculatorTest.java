package de.fiduciagad.de.sft.heatmap.test;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.heatmap.HeatMapTileCalculator;

public class HeatMapTileCalculatorTest {

	private int xMaxOfGame = 200;
	private int yMaxOfGame = 200;

	public HeatMapTileCalculatorTest() {

	}

	@Test
	public void definingOneHoricontalTileAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 100;
		int y = 0;

		int heatMapTilesPerRow = 1;

		HeatMapTileCalculator h = new HeatMapTileCalculator();

		h.setHeatMapSize(xMaxOfGame, yMaxOfGame);
		h.setheatMapTilesPerColumn(heatMapTilesPerRow);

		assertThat(h.calculateHeatMapId(x, y), CoreMatchers.is(1));

	}

	@Test
	public void definingTwoHoricontalTilesAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 150;
		int y = 0;

		int heatMapTilesPerColumn = 2;

		HeatMapTileCalculator h = new HeatMapTileCalculator();

		h.setHeatMapSize(xMaxOfGame, yMaxOfGame);
		h.setheatMapTilesPerColumn(heatMapTilesPerColumn);

		assertThat(h.calculateHeatMapId(x, y), CoreMatchers.is(2));

	}

	@Test
	public void definingThreeHoricontalTilesAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 190;
		int y = 0;

		int heatMapTilesPerColumn = 3;

		HeatMapTileCalculator h = new HeatMapTileCalculator();

		h.setHeatMapSize(xMaxOfGame, yMaxOfGame);
		h.setheatMapTilesPerColumn(heatMapTilesPerColumn);

		assertThat(h.calculateHeatMapId(x, y), CoreMatchers.is(3));

	}

	//TODO: add vertical support for heatmap
	
	@Ignore
	@Test
	public void definingTwoVerticalTilesAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 0;
		int y = 150;

		int heatMapTilesPerRow = 2;

		HeatMapTileCalculator h = new HeatMapTileCalculator();

		h.setHeatMapSize(xMaxOfGame, yMaxOfGame);
		h.setheatMapTilesPerRow(heatMapTilesPerRow);

		assertThat(h.calculateHeatMapId(x, y), CoreMatchers.is(2));

	}

	@Ignore
	@Test
	public void definingTwoVerticalAndTwoHoricontalTilesAndAddingPosition_shouldHaveCorrectHeatMapId() {

		int x = 110;
		int y = 110;

		int heatMapTilesPerRow = 4;

		HeatMapTileCalculator h = new HeatMapTileCalculator();

		h.setHeatMapSize(xMaxOfGame, yMaxOfGame);
		h.setheatMapTilesPerColumn(heatMapTilesPerRow);

		assertThat(h.calculateHeatMapId(x, y), CoreMatchers.is(4));

	}

}
