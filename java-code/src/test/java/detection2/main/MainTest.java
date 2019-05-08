package detection2.main;

import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class MainTest {

	@Test
	public void test() throws IOException {
		String[] py = new String[] { //
				"import sys", //
				"print('Hello, world! ', str(sys.argv))" //
		};

		File tmpFile = fileWithContent(py);
		try {
			String module = tmpFile.getAbsolutePath();
			assertThat(Main.process(module, "-foo", "bar").collect(toList()),
					is(asList("('Hello, world! ', \"['" + module + "', '-foo', 'bar']\")")));
		} finally {
			tmpFile.delete();
		}
	}

	private File fileWithContent(String[] content) throws IOException {
		File file = File.createTempFile("test", ".py");
		file.deleteOnExit();
		write(file.toPath(), stream(content).collect(joining("\n")).getBytes());
		return file;
	}

}
