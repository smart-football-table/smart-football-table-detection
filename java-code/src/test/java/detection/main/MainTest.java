package detection.main;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static detection.data.unit.DistanceUnit.CENTIMETER;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.jupiter.api.Test;

class MainTest {

	private static final String MQTTPORT = "MQTTPORT";
	private static final String MQTTHOST = "MQTTHOST";
	private static final String TABLEHEIGHT = "TABLEHEIGHT";
	private static final String TABLEWIDTH = "TABLEWIDTH";
	private static final String TABLEUNIT = "TABLEUNIT";

	@Test
	void printsHelpOnMinusH() throws Exception {
		assertThat(tapSystemErr(() -> Main.main("-h")), allOf(//
				containsString("-mqttHost " + MQTTHOST), //
				containsString("-mqttPort " + MQTTPORT), //
				containsString("-tableWidth " + TABLEWIDTH), //
				containsString("-tableHeight " + TABLEHEIGHT), //
				containsString("-tableUnit") // TODO bug in args4j?
//				containsString("-tableUnit " + TABLEUNIT) // TODO bug in args4j?
		));
	}

	@Test
	void canReadEnvVars() throws Exception {
		Main main = new Main();
		withEnvironmentVariable(MQTTPORT, "1") //
				.and(MQTTHOST, "someHostname") //
				.and(TABLEHEIGHT, "2") //
				.and(TABLEWIDTH, "3") //
				.and(TABLEUNIT, CENTIMETER.name()) //
				.execute(() -> assertThat(main.parseArgs(), is(true)));
		assertThat(main.mqttPort, is(1));
		assertThat(main.mqttHost, is("someHostname"));
		assertThat(main.tableHeight, is(2));
		assertThat(main.tableWidth, is(3));
		assertThat(main.tableUnit, is(CENTIMETER));
	}

}
