package detection2;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RelativePositionTest {

	@Test
	public void normalizeLeftHandSide() {
		RelativePosition pos = new RelativePosition(anyTimestamp(), 0.1, 0.2);
		assertThat(pos.normalizeX(), is(new RelativePosition(pos.getTimestamp(), 0.9, 0.2)));
	}

	@Test
	public void normalizeRightHandSide() {
		RelativePosition pos = new RelativePosition(anyTimestamp(), 0.9, 0.2);
		assertThat(pos.normalizeX(), is(pos));
	}

	private long anyTimestamp() {
		return 123;
	}

}
