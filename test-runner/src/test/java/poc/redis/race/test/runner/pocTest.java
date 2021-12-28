package poc.redis.race.test.runner;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import poc.redis.race.test.CacheState;
import poc.redis.race.test.CacheState.ValueState;
import poc.redis.race.test.PoC;

public class pocTest {

	private static final String endpoint = "http://localhost:8080";
	private ExecutorService exec;

	@Before
	public void init() {
		final PoC poc = PoC.getInstance();
		if (poc.clearTable()) {
			poc.prefillTable();
		}
		final int executorsInPool = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
		exec = Executors.newFixedThreadPool(executorsInPool);
	}

	@After
	public void teardown() {
		PoC.getInstance().stopUpdater();
	}

	public static boolean allAlive;

	@Test
	public void reproduceSparadical() {
		demo();
	}

	@Test
	public void reproducePermanent() {
		CacheState.singleDegradeIsEnought = false;
		demo();
	}

	@Test
	public void warm() {
		for (String id : PoC.getInstance().getIds()) {
			CacheState cs = PoC.getInstance().getCacheState(id);
			for (HttpClient client : cs.getClients()) {
				exec.execute(() -> {
					final URI target = URI.create(new StringBuilder(endpoint).append("/").append(id).toString());
					HttpRequest request = HttpRequest.newBuilder(target)
							.header("Content-Type", "application/json")
							.GET()
							.timeout(Duration.ofMinutes(1))
							.build();
					int counter = 50000;
					while (counter > 0) {
						short response = request(request, client);
						if (response != -1) {
							counter--;
						}
					}
				});
			}
		}
		PoC.getInstance().runUpdater();
		exec.shutdown();
		try {
			exec.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException ex) {
			Logger.getLogger(pocTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void demo() {
		allAlive = true;
		try {
			for (String id : PoC.getInstance().getIds()) {
				CacheState cs = PoC.getInstance().getCacheState(id);
				for (HttpClient client : cs.getClients()) {
					exec.execute(() -> {
						final URI target = URI.create(new StringBuilder(endpoint).append("/").append(id).toString());
						HttpRequest request = HttpRequest.newBuilder(target)
								.header("Content-Type", "application/json")
								.GET()
								.timeout(Duration.ofMinutes(1))
								.build();
						while (cs.getState() != ValueState.DEGRADED && allAlive) {
							short response = request(request, client);
							if (response != -1) {
								cs.validate(response);
							}
						}
						allAlive = false;
					});
				}
			}
			PoC.getInstance().runUpdater();
			exec.shutdown();
			exec.awaitTermination(1, TimeUnit.HOURS);
			Assert.assertFalse(allAlive);
		} catch (InterruptedException ex) {
			Logger.getLogger(pocTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private short request(HttpRequest request, HttpClient client) {
		try {
			String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
			try {
				return Short.parseShort(response.substring(response.lastIndexOf("\"score\":") + 8, response.lastIndexOf("}")));
			} catch (Exception ex) {
			}
		} catch (IOException ex) {
			Logger.getLogger(pocTest.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(pocTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		return -1;
	}

}
