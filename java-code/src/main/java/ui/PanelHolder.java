package ui;

import java.io.IOException;
import java.util.List;

import javax.swing.JTabbedPane;

import ballPosition_module.BallPositionController;
import ballPosition_module.BallPositionMainPanel;
import possession_module.PossessionController;

public class PanelHolder extends JTabbedPane {

	BallPositionMainPanel ballPositionMainPanel;

	public PanelHolder() throws IOException {
		init();
	}

	private void init() throws IOException {

		FileRead fileRead = new FileRead();
		List<LocationAtTimepoint> locationsOfBall = fileRead.readFile();
		
		BallPositionController ballPositionController = new BallPositionController(locationsOfBall);
		this.addTab("Ballposition", ballPositionController.getBallPositionMainPanel());
		
		PossessionController possessionController = new PossessionController(locationsOfBall);
		this.addTab("Ballbesitz", possessionController.getPossessionMainPanel());
		

	}


}
