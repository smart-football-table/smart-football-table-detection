package possession_module;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class BarChartPanel extends JPanel {

	PossessionMainPanel possessionMainPanel;

	int lenghtBarLeftSideWidth = 250;
	int lenghtBarRightSideWidth = 250;

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
		g.fillRect(100, 100, lenghtBarLeftSideWidth, 50);
		g.setColor(Color.BLUE);
		g.fillRect(100 + lenghtBarLeftSideWidth, 100, lenghtBarRightSideWidth, 50);

	}

	public void drawBar(double possessionLeftSide) {

		System.out.println(possessionLeftSide);
		
		lenghtBarLeftSideWidth = (int) (500*possessionLeftSide);
		lenghtBarRightSideWidth = 500 - lenghtBarLeftSideWidth;
		
		repaint();
	}

}
