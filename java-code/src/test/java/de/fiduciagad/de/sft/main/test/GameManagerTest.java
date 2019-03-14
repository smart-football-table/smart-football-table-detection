package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.GameManager;
import fiduciagad.de.sft.main.OpenCVHandler;

public class GameManagerTest {

	@Test
	public void canSetScore() throws MqttSecurityException, MqttException {

		GameManager game = new GameManager();

		game.setGoalForTeamWhenGoalHappend("on the right");
		game.setGoalForTeamWhenGoalHappend("on the right");
		game.setGoalForTeamWhenGoalHappend("on the left");

		assertThat(game.getScoreAsString(), is("2-1"));
	}

}
