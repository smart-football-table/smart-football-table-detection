package ballPosition_module;

import java.io.IOException;
import java.util.List;

import ui.LocationAtTimepoint;

public class BallPositionController {

	BallPositionMainPanel ballPositionMainPanel;
	List<LocationAtTimepoint> locationsOfBall;

	public BallPositionController(List<LocationAtTimepoint> locationsOfBall) throws IOException {

		this.locationsOfBall = locationsOfBall;
		ballPositionMainPanel = new BallPositionMainPanel(this);

	}

	public List<LocationAtTimepoint> getLocationsOfBall() {
		return locationsOfBall;
	}

	public void setLocationsOfBall(List<LocationAtTimepoint> locationsOfBall) {
		this.locationsOfBall = locationsOfBall;
	}

	public BallPositionMainPanel getBallPositionMainPanel() {
		return ballPositionMainPanel;
	}

	public void setBallPositionMainPanel(BallPositionMainPanel ballPositionMainPanel) {
		this.ballPositionMainPanel = ballPositionMainPanel;
	}

	public void drawBallWithTimepoint(int index) {

		int xPosition = locationsOfBall.get(index).getxCoordinate();
		int yPosition = locationsOfBall.get(index).getyCoordinate();
		ballPositionMainPanel.getFieldPanel().setCoordinates(xPosition, yPosition);

	}

}
