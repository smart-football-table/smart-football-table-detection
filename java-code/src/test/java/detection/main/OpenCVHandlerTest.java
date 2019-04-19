package detection.main;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import detection.main.OpenCVHandler;

public class OpenCVHandlerTest {

	@Test
	public void readSimpleConsoleAndEndTheProgram() {

		OpenCVHandler opencv = new OpenCVHandler();

		assertThat(opencv.isProcessAlive(), is(false));

		opencv.setPythonModule("test.py");
		opencv.startPythonModule();

		assertThat(opencv.isProcessAlive(), is(true));

	}

}
