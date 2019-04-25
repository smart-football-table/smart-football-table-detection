package detection2.input;

import java.io.IOException;

import detection2.data.position.RelativePosition;

// TODO PF use Interface Iterator instead
public interface PositionProvider {
	RelativePosition next() throws IOException;
}