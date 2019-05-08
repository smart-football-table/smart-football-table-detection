package detection2.main;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.Test;

public class MainTest {

	@Test
	public void test() throws IOException {
		String module = "src/test/resources/helloworld.py";
		Stream<String> stream = Main.process(module, "-foo", "bar");
		assertThat(stream.collect(toList()), is(asList("('Hello, world! ', \"['" + module + "', '-foo', 'bar']\")")));
	}

}
