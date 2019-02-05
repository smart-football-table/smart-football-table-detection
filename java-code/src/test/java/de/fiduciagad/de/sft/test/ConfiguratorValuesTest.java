package de.fiduciagad.de.sft.test;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ConfiguratorValuesTest {

	@Test
	public void configureFootballTableCorrect() {
		
		ConfiguratorValues.setGameFieldSize(1000,500);
		
		assertThat(ConfiguratorValues.getXMaxOfGameField(), CoreMatchers.is(1000));
		assertThat(ConfiguratorValues.getYMaxOfGameField(), CoreMatchers.is(500));
		
		ConfiguratorValues.setGameFieldSize(2000,1000);
		
		assertThat(ConfiguratorValues.getXMaxOfGameField(), CoreMatchers.is(2000));
		assertThat(ConfiguratorValues.getYMaxOfGameField(), CoreMatchers.is(1000));
		
		
	}

}
