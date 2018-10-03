package possession_module;

import java.util.List;

import ui.LocationAtTimepoint;

public class PossessionController {

	PossessionMainPanel possessionMainPanel;
	List<LocationAtTimepoint> locationsOfBall;
	
	public PossessionController(List<LocationAtTimepoint> locationsOfBall) {

		this.locationsOfBall = locationsOfBall;
		possessionMainPanel = new PossessionMainPanel(this);
		
		double possessionLeftSide = calculatePossessionLeftSide();
		
		possessionMainPanel.getBarChartPanel().drawBar(possessionLeftSide);
	
	}

	private double calculatePossessionLeftSide() {

		int locationCountOnLeftSide = 0;
		
		for (LocationAtTimepoint currentLocation : locationsOfBall) {
			locationCountOnLeftSide += isLocationOnLeftSide(currentLocation) ? 1 : 0;
		}
		
		int locationCountOnRightSide = locationsOfBall.size() - locationCountOnLeftSide;
		
		double possessionLeftSide = (locationCountOnLeftSide / (locationCountOnLeftSide + locationCountOnRightSide)) * 100;
		return possessionLeftSide;
		
	}

	private boolean isLocationOnLeftSide(LocationAtTimepoint locationAtTimepoint) {
		int framesize = 800;
		return locationAtTimepoint.getxCoordinate() < (framesize/2);
	}

	public PossessionMainPanel getPossessionMainPanel() {
		return possessionMainPanel;
	}

	public void setPossessionMainPanel(PossessionMainPanel possessionMainPanel) {
		this.possessionMainPanel = possessionMainPanel;
	}
	
	
	
	
}
