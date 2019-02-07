package samrock.filter.view;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

public class Utils {
	private static final ExecutorService main = Executors.newSingleThreadExecutor();
	
	static void shutDown() throws InterruptedException {
		main.shutdownNow();
		main.awaitTermination(3, TimeUnit.HOURS);
	}
	
	public static void fx(Runnable runnable) {
		Platform.runLater(runnable);
	}

	public static void execute(Runnable runnable) {
		main.execute(runnable);
	}
}
