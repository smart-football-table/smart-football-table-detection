package detection.main;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.lang.annotation.Retention;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.NamedOptionDef;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;

public final class EnvVars {

	@Retention(RUNTIME)
	public static @interface EnvVar {
		String value();
	}

	private EnvVars() {
		super();
	}

	public static String[] envVarsAndArgs(CmdLineParser parser, String... args) {
		return mixinEnvVars(parser.getOptions(), args);
	}

	public static String[] mixinEnvVars(@SuppressWarnings("rawtypes") List<OptionHandler> handlers, String... args) {
		return concat(readEnvVars(handlers), stream(args)).toArray(String[]::new);
	}

	public static Stream<String> readEnvVars(@SuppressWarnings("rawtypes") List<OptionHandler> handlers) {
		return handlers.stream().map(EnvVars::toOptionString).flatMap(Collection::stream);
	}

	private static List<String> toOptionString(@SuppressWarnings("rawtypes") OptionHandler handler) {
		String value = envValue(handler);
		return value == null ? emptyList() : asList(handler.option.toString(), value);
	}

	private static String envValue(@SuppressWarnings("rawtypes") OptionHandler handler) {
		EnvVar envVar = handler.setter.asAnnotatedElement().getAnnotation(EnvVar.class);
		if (envVar != null) {
			return System.getenv(envVar.value());
		}
		OptionDef optionDef = handler.option;
		if (optionDef instanceof NamedOptionDef) {
			NamedOptionDef namedOptionDef = (NamedOptionDef) optionDef;
			return concat(of(namedOptionDef.name()), of(namedOptionDef.aliases())) //
					.map(EnvVars::upperAndStripped).map(System::getenv) //
					.filter(Objects::nonNull).findFirst().orElse(null);
		}
		return null;
	}

	private static String upperAndStripped(String envVar) {
		return stripLeading(envVar.toUpperCase(), '-');
	}

	private static String stripLeading(String name, char ch) {
		return name.substring( //
				range(0, name.length()).filter(i -> name.charAt(i) != ch).findFirst().getAsInt(), //
				name.length() //
		);
	}

}