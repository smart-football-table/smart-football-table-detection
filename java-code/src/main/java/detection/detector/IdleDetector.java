package detection.detector;

import static java.util.concurrent.TimeUnit.MINUTES;

import detection.data.position.AbsolutePosition;

public class IdleDetector implements Detector {

	private interface State {
		State update(AbsolutePosition pos);

		default boolean isIdle() {
			return false;
		}
	}

	private class Movement implements IdleDetector.State {

		private AbsolutePosition lastMovement;

		public Movement(AbsolutePosition lastMovement) {
			this.lastMovement = lastMovement;
		}

		public Movement lastMovement(AbsolutePosition lastMovement) {
			this.lastMovement = lastMovement;
			return this;
		}

		@Override
		public State update(AbsolutePosition pos) {
			return pos.isNull() || pos.getRelativePosition().equalsPosition(lastMovement.getRelativePosition())
					? new NoMovement(pos)
					: lastMovement(pos);
		}

	}

	private class NoMovement implements IdleDetector.State {

		private final AbsolutePosition noMovementSince;

		public NoMovement(AbsolutePosition pos) {
			this.noMovementSince = pos;
		}

		@Override
		public State update(AbsolutePosition pos) {
			return pos.getRelativePosition().equalsPosition(noMovementSince.getRelativePosition()) //
					? timeoutReached(pos, noMovementSince.getTimestamp()) //
							? new Idle(pos) //
							: this //
					: new Movement(pos);
		}

	}

	private class Idle implements State {

		private final AbsolutePosition pos;

		public Idle(AbsolutePosition pos) {
			this.pos = pos;
		}

		@Override
		public State update(AbsolutePosition pos) {
			return pos.getRelativePosition().equalsPosition(this.pos.getRelativePosition()) //
					? this //
					: new Movement(pos);
		}

		@Override
		public boolean isIdle() {
			return true;
		}
	}

	public static IdleDetector onIdle(Listener listener) {
		return new IdleDetector(listener);
	}

	@Override
	public IdleDetector newInstance() {
		return new IdleDetector(listener);
	}

	private final long idleWhen = MINUTES.toMillis(1);

	private boolean timeoutReached(AbsolutePosition pos, long offTableSince) {
		return timestampDiff(pos, offTableSince) >= idleWhen;
	}

	private long timestampDiff(AbsolutePosition pos, long offTableSince) {
		return pos.getTimestamp() - offTableSince;
	}

	public static interface Listener {
		void idle(boolean state);
	}

	private final IdleDetector.Listener listener;
	private State state = p -> new Movement(p);

	private IdleDetector(IdleDetector.Listener listener) {
		this.listener = listener;
	}

	@Override
	public void detect(AbsolutePosition pos) {
		boolean oldIdleState = state.isIdle();
		state = state.update(pos);
		if (oldIdleState != state.isIdle()) {
			listener.idle(state.isIdle());
		}
	}

}