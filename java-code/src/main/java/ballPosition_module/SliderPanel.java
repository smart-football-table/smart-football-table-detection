package ballPosition_module;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSlider;

import ui.FileRead;
import ui.LocationAtTimepoint;

public class SliderPanel extends JPanel{

	JSlider timepoint;
	
	BallPositionMainPanel mainPanel;
	
	public SliderPanel(BallPositionMainPanel panelHolder) throws IOException {
		this.mainPanel = panelHolder;
		init();
	}

	private void init() throws IOException {
		this.setLayout(new BorderLayout());
		
		timepoint = getTimepoint();

		this.add(timepoint, BorderLayout.CENTER);

		
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

			timepoint.addChangeListener(new SliderChangeListener(mainPanel.getFieldPanel(), timepoint));

		}

		return timepoint;

	}
	
}
