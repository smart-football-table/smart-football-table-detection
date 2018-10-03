package ballPosition_module;

import java.io.IOException;
import java.util.List;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ui.FileRead;
import ui.LocationAtTimepoint;

public class SliderChangeListener implements ChangeListener {

	FieldPanel fieldPanel;
	JSlider jslider;

	public SliderChangeListener(FieldPanel fieldPanel, JSlider jslider) throws IOException {
		this.fieldPanel = fieldPanel;
		this.jslider = jslider;
	}

	public void stateChanged(ChangeEvent e) {

		// TODO ist der aufwaendige Weg zurück zum Controller sinnvoll, nur weil so die
		// Einheitlichkeit bewahrt wird?
		int index = jslider.getValue();

		fieldPanel.getBallPositionMainPanel().getBallPositionController().drawBallWithTimepoint(index);

	}

}
