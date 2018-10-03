package ballPosition_module;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JSlider;

public class SliderPanel extends JPanel {

	JSlider timepointSlider;
	BallPositionMainPanel mainPanel;

	public SliderPanel(BallPositionMainPanel panelHolder) throws IOException {
		this.mainPanel = panelHolder;
		init();
	}

	private void init() throws IOException {
		this.setLayout(new BorderLayout());

		timepointSlider = getTimepointSlider();
		this.add(timepointSlider, BorderLayout.CENTER);

	}

	public JSlider getTimepointSlider() throws IOException {

		if (timepointSlider == null) {

			timepointSlider = new JSlider(JSlider.HORIZONTAL, 0,
					mainPanel.getBallPositionController().getLocationsOfBall().size(), 0);

			timepointSlider.setMajorTickSpacing(20);
			timepointSlider.setMinorTickSpacing(1);
			timepointSlider.setPaintTicks(true);
			timepointSlider.setPaintLabels(true);

			timepointSlider.addChangeListener(new SliderChangeListener(mainPanel.getFieldPanel(), timepointSlider));

		}

		return timepointSlider;

	}

}
