package org.springframework.boot;

import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

public class ShutdownHook {

	private static final AtomicBoolean registered = new AtomicBoolean();

	private static final Set<ConfigurableApplicationContext> contexts = Collections.newSetFromMap(new WeakHashMap<>());

	private static final Set<Runnable> cleanupActions = new HashSet<>();

	private static final Object MONITOR = new Object();

	private static void registerIfNecessary() {
		if (registered.compareAndSet(false, true)) {
			try {
				Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHook::shutdown));
			}
			catch (AccessControlException ex) {
				// Not allowed in some environments
			}
		}
	}

	public static void register(Runnable cleanupAction) {
		registerIfNecessary();
		synchronized (MONITOR) {
			cleanupActions.add(cleanupAction);
		}
	}

	static void register(ConfigurableApplicationContext context) {
		registerIfNecessary();
		context.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
			@Override
			public void onApplicationEvent(ContextClosedEvent event) {
				synchronized (MONITOR) {
					contexts.remove(context);
				}
			}
		});
		synchronized (MONITOR) {
			contexts.add(context);
		}
	}

	private static void shutdown() {
		for (ConfigurableApplicationContext context : copy(contexts)) {
			context.close();
		}
		for (Runnable cleanupAction : copy(cleanupActions)) {
			cleanupAction.run();
		}
	}

	private static <T> Set<T> copy(Set<T> set) {
		synchronized (MONITOR) {
			return new HashSet<>(set);
		}
	}

}
