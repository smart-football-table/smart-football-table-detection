package detection.detector;

import static detection.data.unit.DistanceUnit.CENTIMETER;

import detection.data.Distance;
import detection.data.Movement;
import detection.data.position.AbsolutePosition;
import detection.data.position.RelativePosition;

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
		void movement(Movement movement, Distance overallDistance);
	}

	private MovementDetector(MovementDetector.Listener listener) {
		this.listener = listener;
	}

	private AbsolutePosition prevPos;
	private Distance overallDistance = new Distance(0, CENTIMETER);

	@Override
	public void detect(AbsolutePosition pos) {
		RelativePosition relPos = pos.getRelativePosition();
		if (!relPos.isNull()) {
			if (prevPos != null) {
				Movement movement = new Movement(prevPos, pos);
				listener.movement(movement, (overallDistance = overallDistance.add(movement.distance())));
			}
			prevPos = pos;
		}
	}

}