package detection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import detection.data.position.RelativePosition;

public class RelativePositionTest {

	@Test
	public void normalizeLeftHandSide() {
		double x = 0.1;
		double y = 0.2;
		RelativePosition pos = RelativePosition.create(anyTimestamp(), x, y);
		assertThat(pos.normalizeX(), is(RelativePosition.create(pos.getTimestamp(), 1.0 - x, y)));
	}

	@Test
	public void normalizeRightHandSide() {
		RelativePosition pos = RelativePosition.create(anyTimestamp(), 0.9, 0.2);
		assertThat(pos.normalizeX(), is(pos));
	}

	@Test
	public void normalizeTop() {
		double x = 0.2;
		double y = 0.1;
		RelativePosition pos = RelativePosition.create(anyTimestamp(), x, y);
		assertThat(pos.normalizeY(), is(RelativePosition.create(pos.getTimestamp(), x, 1.0 - y)));
	}

	@Test
	public void normalizeBottom() {
		RelativePosition pos = RelativePosition.create(anyTimestamp(), 0.2, 0.9);
		assertThat(pos.normalizeY(), is(pos));
	}

	private long anyTimestamp() {
		return 123;
	}

}
