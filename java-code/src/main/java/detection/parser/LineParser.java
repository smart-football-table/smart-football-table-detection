package detection.parser;

import detection.data.position.RelativePosition;

@FunctionalInterface
public interface LineParser {
	RelativePosition parse(String line);
}