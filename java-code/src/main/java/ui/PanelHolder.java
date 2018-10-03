package ui;

import java.io.IOException;

import javax.swing.JTabbedPane;

import ballPosition_module.BallPositionMainPanel;
import possession_module.PossessionMainPanel;

public class PanelHolder extends JTabbedPane {

	BallPositionMainPanel ballPositionMainPanel;
	PossessionMainPanel possessionMainPanel;

	public PanelHolder() throws IOException {
		init();
	}

	private void init() throws IOException {

		ballPositionMainPanel = new BallPositionMainPanel();
		
		this.addTab("Ballposition", ballPositionMainPanel);
		this.addTab("Ballbesitz", possessionMainPanel);
		

	}


}
