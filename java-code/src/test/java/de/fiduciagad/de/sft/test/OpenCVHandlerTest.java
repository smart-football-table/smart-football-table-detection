package de.fiduciagad.de.sft.test;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class OpenCVHandlerTest {

	@Test
	public void readSimpleConsole() {
		
		OpenCVHandler opencv = new OpenCVHandler();
		
		opencv.startPythonModule("test.py");
		
		String output = opencv.readConsole();
		
		assertThat(output, CoreMatchers.is("1"));
		
	}

}
