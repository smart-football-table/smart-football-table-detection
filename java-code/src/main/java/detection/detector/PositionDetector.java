package detection.detector;

import detection.data.position.AbsolutePosition;

public class PositionDetector implements Detector {

	public static PositionDetector onPositionChange(Listener listener) {
		return new PositionDetector(listener);
	}

	@Override
	public PositionDetector newInstance() {
		return new PositionDetector(listener);
	}

	private final PositionDetector.Listener listener;

	public static interface Listener {
		void position(AbsolutePosition pos);
	}

	private PositionDetector(PositionDetector.Listener listener) {
		this.listener = listener;
	}

	@Override
	public void detect(AbsolutePosition pos) {
		if (!pos.getRelativePosition().isNull()) {
			listener.position(pos);
		}
	}

}