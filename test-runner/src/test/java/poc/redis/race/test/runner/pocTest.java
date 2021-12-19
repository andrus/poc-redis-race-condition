package poc.redis.race.test.runner;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import poc.redis.race.test.PoC;

public class pocTest {

	@Before
	public void init() {
		final PoC poc = PoC.getInstance();
		boolean ok = poc.clearTable();
		if (ok) {
			ok = poc.prefillTable();
		}
		if (ok) {
			poc.runUpdater();
		}
	}

	@After
	public void teardown() {
		PoC.getInstance().stopUpdater();
	}

	@Test
	public void reproduceTest() {
		try {
			Thread.sleep(1000);
			int aa = 0;
			aa = 99 / 9;
			assertEquals(9, aa);
		} catch (InterruptedException ex) {
			Logger.getLogger(pocTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Test
	public void fixTest() {
		int aa = 0;
		aa = 99 / 9;
		assertEquals(11, aa);
	}

}
