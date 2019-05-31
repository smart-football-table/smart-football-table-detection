package detection.main;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.kohsuke.args4j.spi.OptionHandler;

public final class EnvVars {

	private EnvVars() {
		super();
	}

	public static Stream<String> readEnvVars(@SuppressWarnings("rawtypes") List<OptionHandler> options) {
		@SuppressWarnings("rawtypes")
		Function<OptionHandler, List<String>> mapper = EnvVars::readEnvVar;
		return options.stream().map(mapper).flatMap(Collection::stream);
	}

	private static List<String> readEnvVar(@SuppressWarnings("rawtypes") OptionHandler h) {
		String envVar = System.getenv(h.option.metaVar());
		return envVar == null ? emptyList() : Arrays.asList(h.option.toString(), envVar);
	}

}