package detection2.detector;

import detection2.data.position.AbsolutePosition;

public class PositionDetector implements Detector {

	private final PositionDetector.Listener listener;

	public static interface Listener {
		void position(AbsolutePosition pos);
	}

	public PositionDetector(PositionDetector.Listener listener) {
		this.listener = listener;
	}

	@Override
	public void detect(AbsolutePosition pos) {
		if (!pos.getRelativePosition().isNull()) {
			listener.position(pos);
		}
	}

}