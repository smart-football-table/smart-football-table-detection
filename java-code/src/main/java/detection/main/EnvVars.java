package detection.main;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.kohsuke.args4j.spi.OptionHandler;

public final class EnvVars {

	private EnvVars() {
		super();
	}

	public static Stream<String> readEnvVars(@SuppressWarnings("rawtypes") List<OptionHandler> handlers) {
		return handlers.stream().map(EnvVars::readEnvVar).flatMap(Collection::stream);
	}

	private static List<String> readEnvVar(@SuppressWarnings("rawtypes") OptionHandler handler) {
		String envVar = System.getenv(handler.option.metaVar());
		return envVar == null ? emptyList() : Arrays.asList(handler.option.toString(), envVar);
	}

}