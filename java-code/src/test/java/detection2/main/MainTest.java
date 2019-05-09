package detection2.main;

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

import org.junit.Test;

public class MainTest {

	@Test
	public void test() throws IOException {
		File tmpFile = fileWithContent( //
				"import sys", //
				"print('Hello, world! ', str(sys.argv))" //
		);
		tmpFile.deleteOnExit();

		String module = tmpFile.getAbsolutePath();
		assertThat(Main.process(module, "-foo", "bar").collect(toList()),
				is(asList("('Hello, world! ', \"['" + module + "', '-foo', 'bar']\")")));
	}

	private static File fileWithContent(String... content) throws IOException {
		return write(createTempFile(MainTest.class.getName() + "tmp", ".py").toPath(),
				stream(content).collect(joining("\n")).getBytes()).toFile();
	}

}
