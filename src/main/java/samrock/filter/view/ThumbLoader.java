package samrock.filter.view;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import sam.fx.helpers.FxText;

public class ThumbLoader implements Runnable {
	
	private final Unit unit;
	private final VBox thumbLoading;
	private final Scrapper scrapper;

	public ThumbLoader(Unit unit, VBox thumbLoading, Scrapper scrapper) {
		this.unit = unit;
		this.thumbLoading = thumbLoading;
		this.scrapper = scrapper;
	}

	@Override
	public void run() {
		Text t2 = FxText.text(unit.manga.mangaName, "text");
        t2.setWrappingWidth(Main.wrappingWidth);
        Platform.runLater(() -> thumbLoading.getChildren().add(t2));
        try {
            unit.setImage(scrapper.downloadThumb(unit.manga));
        } catch (IOException e) {
            unit.addError(e, "failed to extract thumb ");
        }
        Platform.runLater(() -> thumbLoading.getChildren().remove(t2));
        Counts.increment(Counts.thumbDownloaded);
	}
	

}
