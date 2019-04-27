package detection2.detector;

import detection2.data.Movement;
import detection2.data.position.AbsolutePosition;
import detection2.data.position.RelativePosition;

public class MovementDetector implements Detector {

	public static MovementDetector onMovement(Listener listener) {
		return new MovementDetector(listener);
	}

	@Override
	public MovementDetector newInstance() {
		return new MovementDetector(listener);
	}

	private final MovementDetector.Listener listener;

	public static interface Listener {
		void movement(Movement movement);
	}

	private MovementDetector(MovementDetector.Listener listener) {
		this.listener = listener;
	}

	private AbsolutePosition prevPos;

	@Override
	public void detect(AbsolutePosition pos) {
		RelativePosition relPos = pos.getRelativePosition();
		if (!relPos.isNull()) {
			if (prevPos != null) {
				listener.movement(new Movement(prevPos, pos));
			}
			prevPos = pos;
		}
	}

}