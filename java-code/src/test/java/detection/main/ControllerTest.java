package detection.main;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import detection.main.BallPosition;
import detection.main.ConfiguratorValues;
import detection.main.Controller;
import detection.main.OpenCVHandler;

public class ControllerTest {

	private Controller game;
	private OpenCVHandler cv;

	@Before
	public void init() {

		game = new Controller();
		cv = new OpenCVHandler();
		cv.setPythonModule("src/main/resources/python-files/playedGameDigitizerOneCam.py");
	}

	@Test
	public void startAndStopAGame() {

		Controller game = new Controller();

		game.startGameDetection();

		assertThat(game.isOngoing(), is(true));

		game.stopGameDetection();

		assertThat(game.isOngoing(), is(false));
	}

	@Test
	public void readRealExampleTestVideoWithNoBallAndGetBallPositions()
			throws MqttSecurityException, MqttException, IOException {

		ConfiguratorValues.setDefaultColorRangeYellow();

		String videoArgument = "src/main/resources/videos/testVid_noBall.avi";
		cv.setPythonArgumentVideoPath(videoArgument);

		game.setGameDetection(cv);
		game.startTheDetection();

		BallPosition positionOne = game.getGameDetection().getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(-1));
		assertThat(positionOne.getYCoordinate(), is(-1));

	}

	@Ignore
	@Test
	public void readRealExampleTestVideoWithBall() throws MqttSecurityException, MqttException, IOException {

		OpenCVHandler opencv = new OpenCVHandler();

		Controller controller = new Controller();
		controller.startGameDetection();

		opencv.setPythonModule("testCase_playedGameDigitizerWithBall.py");
		opencv.startPythonModule();

		opencv.handleWithOpenCVOutput(controller);

		BallPosition positionOne = opencv.getManager().getBallPositions().get(0);

		assertThat(positionOne.getXCoordinate(), is(33));
		assertThat(positionOne.getYCoordinate(), is(33));

	}

	@Ignore
	@Test
	public void canDetectGoalFromTestVideo() throws MqttSecurityException, MqttException, IOException {

		Controller controller = new Controller();

		OpenCVHandler cv = new OpenCVHandler();

		cv.setPythonModule("testCase_playedGameDigitizerWithBallAndGoal.py");

		controller.setGameDetection(cv);
		controller.startTheDetection();

		cv.handleWithOpenCVOutput(controller);

		assertThat(cv.getManager().getScoreAsString(), is("1-0"));
	}

	private static String buildPythonArgumentForColor() {
		String pythonArgumentColor = ConfiguratorValues.getColorHSVMinH() + "," + ConfiguratorValues.getColorHSVMinS()
				+ "," + ConfiguratorValues.getColorHSVMinV() + "," + ConfiguratorValues.getColorHSVMaxH() + ","
				+ ConfiguratorValues.getColorHSVMaxS() + "," + ConfiguratorValues.getColorHSVMaxV();
		return pythonArgumentColor;
	}

}
