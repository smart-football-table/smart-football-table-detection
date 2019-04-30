package detection2.detector;

import static java.util.concurrent.TimeUnit.MINUTES;

import detection2.data.position.AbsolutePosition;

public class IdleDetector implements Detector {

	private interface State {
		State update(AbsolutePosition pos);
	}

	private class GameInPlay implements IdleDetector.State {
		@Override
		public State update(AbsolutePosition pos) {
			return pos.isNull() ? new OffTableNotIdle(pos) : this;
		}
	}

	private class OffTableNotIdle implements IdleDetector.State {

		private long offTableSince;

		public OffTableNotIdle(AbsolutePosition pos) {
			this.offTableSince = pos.getTimestamp();
		}

		@Override
		public State update(AbsolutePosition pos) {
			return pos.isNull() //
					? isIdle(pos) //
							? new OffTableAndIdle() //
							: this //
					: new GameInPlay();
		}

		private boolean isIdle(AbsolutePosition pos) {
			return pos.getTimestamp() - offTableSince >= idleWhen;
		}

	}

	private class OffTableAndIdle implements State {
		@Override
		public State update(AbsolutePosition pos) {
			return pos.isNull() ? this : new GameInPlay();
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

	public static interface Listener {
		void idle(boolean state);
	}

	private final IdleDetector.Listener listener;
	private State state = new GameInPlay();

	private IdleDetector(IdleDetector.Listener listener) {
		this.listener = listener;
	}

	@Override
	public void detect(AbsolutePosition pos) {
		State oldState = state;
		state = state.update(pos);
		if (switchedToIdle(oldState)) {
			listener.idle(true);
		}
		if (switchedFromIdle(oldState)) {
			listener.idle(false);
		}
	}

	private boolean switchedToIdle(State oldState) {
		return !(oldState instanceof OffTableAndIdle) && state instanceof OffTableAndIdle;
	}

	private boolean switchedFromIdle(State oldState) {
		return (oldState instanceof OffTableAndIdle) && !(state instanceof OffTableAndIdle);
	}

}