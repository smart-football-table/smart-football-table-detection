package detection2.detector;

import static java.util.concurrent.TimeUnit.MINUTES;

import detection2.data.position.AbsolutePosition;

public class IdleDetector implements Detector {

	public static IdleDetector onIdle(Listener listener) {
		return new IdleDetector(listener);
	}

	private final long idleWhen = MINUTES.toMillis(1);

	public static interface Listener {
		void idle(boolean state);
	}

	private final IdleDetector.Listener listener;
	private AbsolutePosition offTableSince;
	private boolean idle;

	private IdleDetector(IdleDetector.Listener listener) {
		this.listener = listener;
	}

	@Override
	public void detect(AbsolutePosition pos) {
		if (offTable(pos)) {
			if (offTableSince == null) {
				offTableSince = pos;
			} else {
				long diff = pos.getTimestamp() - offTableSince.getTimestamp();
				if (diff >= idleWhen) {
					if (!idle) {
						listener.idle(true);
						idle = true;
					}
				}
			}
		} else {
			if (idle) {
				listener.idle(false);
				idle = false;
			}
		}
	}

	private boolean offTable(AbsolutePosition pos) {
		boolean offTable = pos.isNull();
		return offTable;
	}

}