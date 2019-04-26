package detection.main;

import java.io.IOException;

public class Main {

	public Main() throws IOException {
		new OpenCVHandler().startPythonModule();
	}

}
