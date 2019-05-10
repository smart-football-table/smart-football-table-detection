package detection.detector;

import detection.data.position.AbsolutePosition;

public class GameStartDetector implements Detector {

	public static interface Listener {
		void gameStarted();
	}

	public static GameStartDetector onGameStart(Listener listener) {
		return new GameStartDetector(listener);
	}

	@Override
	public GameStartDetector newInstance() {
		return new GameStartDetector(listener);
	}

	private final GameStartDetector.Listener listener;

	private GameStartDetector(GameStartDetector.Listener listener) {
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