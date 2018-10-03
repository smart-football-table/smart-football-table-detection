package ui;

import java.io.IOException;
import java.util.List;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderChangeListener implements ChangeListener {

	FieldPanel fieldPanel;
	JSlider jslider;
	FileRead fileRead = new FileRead();
	List<LocationAtTimepoint> locationsOfBall;

	public SliderChangeListener(FieldPanel fieldPanel, JSlider jslider) throws IOException {
		this.fieldPanel = fieldPanel;
		this.jslider = jslider;

		locationsOfBall = fileRead.readFile();

	}

	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub

		int index = jslider.getValue();
		
		System.out.println(locationsOfBall.get(index).getxCoordinate());
		
		fieldPanel.setCoordinates(locationsOfBall.get(index).getxCoordinate(),
				locationsOfBall.get(index).getyCoordinate());

	}

}
