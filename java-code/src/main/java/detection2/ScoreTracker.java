package detection2;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import detection2.ScoreTracker.Listener;

public class ScoreTracker {

	public static interface Listener {
		void teamScored(int teamid, int score);

		void won(int teamid);

		void draw(int[] teamids);
	}

	private static final int MAX_BALLS = 10;
	public static ScoreTracker onScoreChange(Listener listener) {
		return new ScoreTracker(listener);
	}

	private final ScoreTracker.Listener listener;

	private ScoreTracker(ScoreTracker.Listener listener) {
		this.listener = listener;
	}

	private final Map<Integer, Integer> scores = new HashMap<>();

	public int teamScored(int teamid) {
		return changeScore(teamid, +1);
	}

	public int revertGoal(int teamid) {
		return changeScore(teamid, -1);
	}

	private int changeScore(int teamid, int d) {
		Integer newScore = score(teamid) + d;
		scores.put(teamid, newScore);
		listener.teamScored(teamid, newScore);
		checkState(teamid, newScore);
		return newScore;
	}

	private void checkState(int teamid, Integer newScore) {
		if (isWinningGoal(newScore)) {
			listener.won(teamid);
		} else if (isDraw()) {
			listener.draw(teamids());
		}
	}

	private Integer score(int teamid) {
		return scores.getOrDefault(teamid, 0);
	}

	private boolean isWinningGoal(int score) {
		return score > ((double) MAX_BALLS) / 2;
	}

	private boolean isDraw() {
		return scores().sum() == MAX_BALLS;
	}

	private IntStream scores() {
		return scores.values().stream().mapToInt(Integer::intValue);
	}

	private int[] teamids() {
		return scores.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
	}

}