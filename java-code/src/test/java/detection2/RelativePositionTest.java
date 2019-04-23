package detection2;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RelativePositionTest {

	@Test
	public void normalizeLeftHandSide() {
		double x = 0.1;
		double y = 0.2;
		RelativePosition pos = new RelativePosition(anyTimestamp(), x, y);
		assertThat(pos.normalizeX(), is(new RelativePosition(pos.getTimestamp(), 1.0 - x, y)));
	}

	@Test
	public void normalizeRightHandSide() {
		RelativePosition pos = new RelativePosition(anyTimestamp(), 0.9, 0.2);
		assertThat(pos.normalizeX(), is(pos));
	}

	@Test
	public void normalizeTop() {
		double x = 0.2;
		double y = 0.1;
		RelativePosition pos = new RelativePosition(anyTimestamp(), x, y);
		assertThat(pos.normalizeY(), is(new RelativePosition(pos.getTimestamp(), x, 1.0 - y)));
	}

	@Test
	public void normalizeBottom() {
		RelativePosition pos = new RelativePosition(anyTimestamp(), 0.2, 0.9);
		assertThat(pos.normalizeY(), is(pos));
	}

	private long anyTimestamp() {
		return 123;
	}

}
