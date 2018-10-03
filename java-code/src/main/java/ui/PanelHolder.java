package ui;

import java.io.IOException;
import java.util.List;

import javax.swing.JTabbedPane;

import ballPosition_module.BallPositionMainPanel;
import possession_module.PossessionController;
import possession_module.PossessionMainPanel;

public class PanelHolder extends JTabbedPane {

	BallPositionMainPanel ballPositionMainPanel;
	PossessionMainPanel possessionMainPanel;

	public PanelHolder() throws IOException {
		init();
	}

	private void init() throws IOException {

		FileRead fileRead = new FileRead();
		List<LocationAtTimepoint> locationsOfBall = fileRead.readFile();
		
		ballPositionMainPanel = new BallPositionMainPanel();
		this.addTab("Ballposition", ballPositionMainPanel);
		
		PossessionController possessionController = new PossessionController(locationsOfBall);
		this.addTab("Ballbesitz", possessionController.getPossessionMainPanel());
		

	}


}
