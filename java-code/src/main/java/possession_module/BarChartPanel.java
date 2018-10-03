package possession_module;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class BarChartPanel extends JPanel {

	PossessionMainPanel possessionMainPanel;

	int lenghtBarLeftSidePossession = 250;
	int lenghtBarRightSidePossession = 250;

	public BarChartPanel(PossessionMainPanel possessionMainPanel) {
		this.possessionMainPanel = possessionMainPanel;
		init();
	}

	private void init() {
		// TODO Auto-generated method stub

	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.RED);
		g.fillRect(100, 100, lenghtBarLeftSidePossession, 50);
		g.setColor(Color.BLUE);
		g.fillRect(100 + lenghtBarLeftSidePossession, 100, lenghtBarRightSidePossession, 50);

	}

	public void drawBar(double possessionLeftSide) {

		lenghtBarLeftSidePossession = (int) (lenghtBarLeftSidePossession*possessionLeftSide);
		lenghtBarRightSidePossession = 500 - lenghtBarLeftSidePossession;
		
		repaint();
	}

}
