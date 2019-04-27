package detection2;

public interface ScoreTracker {

	public static interface Listener {
		void teamScored(int teamid, int score);

		void won(int teamid);

		void draw(int[] teamids);
	}

	int teamScored(int teamid);

	int revertGoal(int teamid);

}