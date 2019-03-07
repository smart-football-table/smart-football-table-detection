package de.fiduciagad.de.sft.main.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.Game;

public class GameTest {

	@Test
	public void startAndStopAGame() {

		Game game = new Game();

		game.start();

		assertThat(game.isOngoing(), is(true));
		
		game.stop();
		
		assertThat(game.isOngoing(), is(false));
	}
	
	@Test
	public void gameStopsWhenDetectionEnds() throws MqttSecurityException, MqttException {

		Game game = new Game();

		game.setPythonModule("testCase_playedGameDigitizerWithoutBall.py");
		game.startTheDetection();
		
		assertThat(game.isOngoing(), is(false));
	}
	
	@Test
	public void canSetScore() {

		Game game = new Game();

		game.setGoalForTeam(1);
		game.setGoalForTeam(1);
		game.setGoalForTeam(2);
		
		assertThat(game.getScoreAsString(), is("2-1"));
	}
	
	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException {

		
		System.out.println("T");
		
		Game game = new Game();

		game.setPythonModule("testCase_playedGameDigitizerWithBallAndGoal.py");
		game.startTheDetection();
		
		assertThat(game.getScoreAsString(), is("1-0"));
	}
	
}
