package sample.tomcat;

import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

public class TickTickBean implements Lifecycle, InitializingBean {

	private Thready t;

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("Start");

		// new RuntimeException("running the ticktick").printStackTrace();

		AccessControlContext context = AccessController.getContext();
		Field field = AccessControlContext.class.getDeclaredField("context");
		field.setAccessible(true);
		ProtectionDomain[] d = (ProtectionDomain[]) field.get(context);
		for (ProtectionDomain protectionDomain : d) {
			// System.out.println(protectionDomain);
		}

		this.t = new Thready();
		this.t.start();
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		System.out.println("Bye");
		this.t.running = false;
		this.t.interrupt();
	}

	@Override
	public boolean isRunning() {
		return this.t.running;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		System.out.println("Tick Tick Final");
	}

	private static class Thready extends Thread {

		boolean running = true;

		@Override
		public void run() {
			while (this.running) {
				System.out.println("Tick");
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
				}
			}
		}
	}

}
