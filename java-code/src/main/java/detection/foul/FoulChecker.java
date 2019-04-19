package detection.foul;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import detection.main.BallPosition;

public class FoulChecker {

	public boolean isThereAFoul(List<BallPosition> ballPositions) {
		if (ballPositions.isEmpty()) {
			return false;
		}
		BallPosition lastPos = ballPositions.get(ballPositions.size() - 1);
		return isEmpty(lastN(ballPositions, 300).filter(diffMore(lastPos.getXCoordinate(), 50)));
	}

	private <T> Stream<T> lastN(List<T> list, int size) {
		return list.subList(max(0, list.size() - size), list.size()).stream();
	}

	private boolean isEmpty(Stream<?> stream) {
		return !stream.findAny().isPresent();
	}

	private Predicate<BallPosition> diffMore(int lastXcoord, int max) {
		return p -> p.getXCoordinate() == -1 || abs(p.getXCoordinate() - lastXcoord) > max;
	}

}
