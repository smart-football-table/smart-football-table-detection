package detection.main;

import static java.io.File.createTempFile;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	void canCallPython() throws IOException {
		String module = tmpFileWithContent( //
				"import sys", //
				"print('Hello, world! ', str(sys.argv))" //
		).getAbsolutePath();
		assertThat(Main.process(module, "-foo", "bar").collect(toList()),
				is(asList("('Hello, world! ', \"['" + module + "', '-foo', 'bar']\")")));
	}

	private static File tmpFileWithContent(String... content) throws IOException {
		File file = writeContent(createTempFile("tmp-" + MainTest.class.getName(), ".py").toPath(), content);
		file.deleteOnExit();
		return file;
	}

	private static File writeContent(Path path, String... content) throws IOException {
		return write(path, stream(content).collect(joining("\n")).getBytes()).toFile();
	}

}
