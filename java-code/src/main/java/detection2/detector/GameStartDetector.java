package detection2.detector;

import detection2.data.position.AbsolutePosition;

public class GameStartDetector implements Detector {

	public static interface Listener {
		void gameStarted();
	}

	private final GameStartDetector.Listener listener;

	public GameStartDetector(GameStartDetector.Listener listener) {
		this.listener = listener;
	}

	private boolean gameStartSend;

	@Override
	public void detect(AbsolutePosition pos) {
		if (!gameStartSend && !pos.getRelativePosition().isNull()) {
			gameStartSend = true;
			listener.gameStarted();
		}
	}

}