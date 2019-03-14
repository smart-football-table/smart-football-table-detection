package de.fiduciagad.de.sft.adjustment.test;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import fiduciagad.de.sft.main.ConfiguratorValues;
import fiduciagad.de.sft.main.OpenCVHandler;

public class ColorAdjustmentTest {

	@Test
	public void canConfigureColor() {

		OpenCVHandler cv = new OpenCVHandler();

		int hsvminh = ConfiguratorValues.getColorHSVMinH();

		cv.setPythonModule("colorGrabber.py");
		cv.startPythonModule();
		cv.startTheAdjustment();

		assertThat(ConfiguratorValues.getColorHSVMinH(), not(hsvminh));
	}

}
