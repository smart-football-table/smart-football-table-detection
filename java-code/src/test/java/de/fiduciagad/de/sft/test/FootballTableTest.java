package de.fiduciagad.de.sft.test;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class FootballTableTest {

	@Test
	public void configureFootballTableCorrect() {
		
		FootballTable.setGameFieldSize(1000,500);
		
		assertThat(FootballTable.getXMaxOfGameField(), CoreMatchers.is(1000));
		assertThat(FootballTable.getYMaxOfGameField(), CoreMatchers.is(500));
		
		FootballTable.setGameFieldSize(2000,1000);
		
		assertThat(FootballTable.getXMaxOfGameField(), CoreMatchers.is(2000));
		assertThat(FootballTable.getYMaxOfGameField(), CoreMatchers.is(1000));
		
		
	}

}
