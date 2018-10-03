package possession_module;

import java.awt.BorderLayout;

import javax.swing.JPanel;

public class PossessionMainPanel extends JPanel {

	PossessionController possessionController;
	
	BarChartPanel barChartPanel;

	public PossessionMainPanel(PossessionController possessionController) {
		this.possessionController = possessionController;
		init();
	}

	private void init() {

		barChartPanel = new BarChartPanel(this);

		this.setLayout(new BorderLayout());

		this.add(barChartPanel, BorderLayout.CENTER);

	}

	public BarChartPanel getBarChartPanel() {
		return barChartPanel;
	}

	public void setBarChartPanel(BarChartPanel barChartPanel) {
		this.barChartPanel = barChartPanel;
	}

}
