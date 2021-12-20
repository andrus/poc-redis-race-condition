package poc.redis.race.test;

import java.net.http.HttpClient;
import java.util.concurrent.locks.ReentrantLock;

public class CacheState {

	public short sheduled;
	public short current;
	public short previous;
	private final String id;
	private final HttpClient[] clients;
	private ValueState state;

	public CacheState(String id, short value, int parallelism) {
		this.id = id;
		this.sheduled = value;
		this.previous = -1001;
		this.current = -1000;
		this.state = ValueState.SHEDULED;
		this.clients = new HttpClient[parallelism];
		for (int i = 0; i < parallelism; i++) {
			this.clients[i] = HttpClient.newBuilder().build();
		}
	}

	public short value() {
		return current;
	}

	public synchronized void validate(short value) {

		switch (this.state) {
			case ACTUAL:
				if (previous == value) {
					this.state = ValueState.DEGRADED;
					System.err.println("Cache for '" + id + "' degraded old value returned: " + value + " (" + this.previous + " | " + this.current + " | " + this.sheduled + ")");
				}
				break;
			case SHEDULED:
				if (sheduled == value) {
					this.previous = this.current;
					this.current = value;
					this.sheduled = -1000;
					this.state = ValueState.ACTUAL;
					System.out.println("Cache for '" + id + "' accepted new value: " + value);
				}
				break;
		}

	}

	public synchronized void shedule(short value) {
		if (this.current != value) {
			this.sheduled = value;
			this.state = ValueState.SHEDULED;
			System.out.println("Cache for '" + id + "' sheduled new value: " + value);
		}
	}

	public ValueState getState() {
		return this.state;
	}

	public HttpClient[] getClients() {
		return clients;
	}

}
