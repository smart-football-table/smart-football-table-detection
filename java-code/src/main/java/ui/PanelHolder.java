package ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class PanelHolder extends JFrame {

	FieldPanel fieldPanel;
	JPanel sliderPanel;

	JSlider timepoint;

	public PanelHolder() throws IOException {
		init();
	}

	private void init() throws IOException {

		fieldPanel = new FieldPanel();
		sliderPanel = getSliderPanel();

		timepoint = getTimepoint();

		sliderPanel.add(timepoint, BorderLayout.CENTER);

		this.setLayout(new BorderLayout());

		this.add(fieldPanel, BorderLayout.CENTER);
		this.add(sliderPanel, BorderLayout.SOUTH);

		this.setSize(1200, 900);
		this.setVisible(true);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);

	}

	public JSlider getTimepoint() throws IOException {

		if (timepoint == null) {

			FileRead fileRead = new FileRead();
			List<LocationAtTimepoint> locationsOfBall = fileRead.readFile();

			timepoint = new JSlider(JSlider.HORIZONTAL, 0, locationsOfBall.size(), 0);

			timepoint.setMajorTickSpacing(20);
			timepoint.setMinorTickSpacing(1);
			timepoint.setPaintTicks(true);
			timepoint.setPaintLabels(true);

			timepoint.addChangeListener(new SliderChangeListener(fieldPanel, timepoint));

		}

		return timepoint;

	}

	public void setTimepoint(JSlider timepoint) {
		this.timepoint = timepoint;
	}

	public JPanel getSliderPanel() {
		if (sliderPanel == null) {
			sliderPanel = new JPanel();
			sliderPanel.setLayout(new BorderLayout());
		}

		return sliderPanel;
	}

	public void setSliderPanel(JPanel sliderPanel) {
		this.sliderPanel = sliderPanel;
	}

}
