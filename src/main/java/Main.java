import java.lang.Thread.UncaughtExceptionHandler;

import javafx.application.Application;
import samrock.filter.view.App;

public class Main {
	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
			}
		});
		Application.launch(App.class, args);
	}
}
