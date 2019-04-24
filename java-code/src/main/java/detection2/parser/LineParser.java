package detection2.parser;

import detection2.data.position.RelativePosition;

public interface LineParser {
	RelativePosition parse(String line);
}