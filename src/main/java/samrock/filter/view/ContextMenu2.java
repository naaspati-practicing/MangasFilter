package samrock.filter.view;
import static sam.fx.helpers.FxMenu.menuitem;
import static sam.myutils.Checker.isNotEmpty;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import sam.config.MyConfig;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.manga.scrapper.ScrappedChapter;
import sam.manga.scrapper.ScrappedManga;
import sam.myutils.MyUtilsExtra;
import sam.reference.WeakAndLazy;
import sam.string.StringUtils;

public class ContextMenu2 extends ContextMenu {
	private static volatile ContextMenu2 instance;

	public static ContextMenu2 getInstance() {
		if (instance == null) {
			synchronized (ContextMenu2.class) {
				if (instance == null)
					instance = new ContextMenu2();
			}
		}
		return instance;
	}

	private ContextMenu2() {
		getItems()
		.addAll(copyId,
				browserMenuItem,
				chapterMenuItem,
				showDescription,
				showLogs);
	}

	private Manga manga;
	private Unit unit;
	private final MenuItem copyId = menuitem("copy ID", e1 -> FxClipboard.setString(String.valueOf(manga.id)));
	private final MenuItem browserMenuItem = menuitem("open in browser", e1 -> Main.getHostService().showDocument(manga.url));
	private final WeakAndLazy<About> about = new WeakAndLazy<>(About::new);
	private final MenuItem chapterMenuItem = menuitem("chapters", e1 -> about.get().show(manga));
	private final WeakAndLazy<Logs> logStage = new WeakAndLazy<>(Logs::new);
	private final MenuItem showLogs = menuitem("Show Logs", e -> logStage.get().show(unit.getLogs()));
	private final MenuItem showDescription = menuitem("Show Description", e -> showDescription(unit));

	public void showContext(Unit unit, ContextMenuEvent e) {
		manga = unit.manga;
		this.unit = unit;

		browserMenuItem.setDisable(manga.url == null);
		chapterMenuItem.setDisable(!unit.isHtmlloaded());
		showLogs.setDisable(unit.getLogs() == null || unit.getLogs().length() == 0);
		Stage s = Main.getStage();
		show(s, e.getScreenX(), e.getScreenY());
	}

	private void showDescription(Unit unit) {
		ScrappedManga sm = unit.manga.getScrappedManga();
		Manga m = unit.manga;
		

		if(sm == null) {
			FxPopupShop.showHidePopup("no scrapperManga", 1500);
			return;
		}

		FxAlert.showMessageDialog(
				sm.getTitle()+"\n"+
				sm.getAuthor()+"\n"+
				m.url+"\n"+
				sm.getTags()+"\n"+
				sm.getDescription() , unit.manga.mangaName+"\n"+unit.manga.id);
	}
}

class Logs extends Stage {
	final TextArea t = new TextArea();

	public Logs() {
		super(StageStyle.UNIFIED);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(Main.getStage());

		t.setEditable(false);
		setScene(new Scene(t, 300, 300));
	}
	public void show(StringBuilder logs) {
		t.setText(logs.toString());
		show();
	}

}

class About extends Stage {
	final ListView<ScrappedChapter> nnew = new ListView<>();
	final ListView<ScrappedChapter> old = new ListView<>();
	final ListView<Double> numbers = new ListView<>();
	final HBox pane = new HBox(bpane("New", nnew), bpane("Remaining", old), bpane("samrock", numbers));
	final Hyperlink title = new Hyperlink();
	final Text id = new Text();
	final Hyperlink url = new Hyperlink();
	final VBox box = new VBox(5, title, id, url, pane);

	private final Path mangafolder = Paths.get(MyConfig.MANGA_DIR);

	public About() {
		initModality(Modality.WINDOW_MODAL);
		initOwner(Main.getStage());
		setWidth(400);
		setWidth(400);

		setScene(new Scene(box));
		for (Node b : new Node[] { title, id, url })
			VBox.setMargin(b, new Insets(0, 0, 0, 5));

		Callback<ListView<ScrappedChapter>, ListCell<ScrappedChapter>> rendre = cb -> new ListCell<ScrappedChapter>() {

			@Override
			protected void updateItem(ScrappedChapter item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty)
					setText(null);
				else
					setText(StringUtils.doubleToString(item.getNumber()) + " " + MyUtilsExtra.nullSafe(item.getTitle(), ""));
			}
		};

		nnew.setCellFactory(rendre);
		old.setCellFactory(rendre);
		numbers.setCellFactory(s -> new ListCell<Double>() {
			@Override
			protected void updateItem(Double item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty)
					setText(null);
				else
					setText(StringUtils.doubleToString(item));
			}
		});

		title.setOnMouseClicked(e -> Main.getHostService()
				.showDocument(mangafolder.resolve(title.getUserData().toString()).toUri().toString()));
		id.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1) {
				FxClipboard.setString(id.getText());
				FxPopupShop.showHidePopup("copied: " + id.getText(), 1500);
			}
		});
		url.setOnAction(e -> Main.getHostService().showDocument(url.getText()));
		setTitle("summery");
	}

	public void show(Manga manga) {
		reset(manga);
		centerOnScreen();
		show();
	}

	private Node bpane(String string, Node list) {
		return new BorderPane(list, new Text(string), null, null, null);
	}

	public void reset(Manga manga) {
		nnew.getItems().clear();
		old.getItems().clear();
		numbers.getItems().clear();

		id.setText(String.valueOf(manga.id));
		title.setText(manga.mangaName);
		title.setDisable(manga.getDirName() == null);
		title.setUserData(manga.getDirName());
		url.setText(manga.url == null ? "--" : manga.url);
		url.setDisable(manga.url == null);

		if (manga.getFilter() != null)
			numbers.getItems().setAll(manga.getFilter().all());

		if (isNotEmpty(manga.getNew())) {
			nnew.getItems().addAll(manga.getNew());
			FXCollections.reverse(nnew.getItems());
		}
		if (isNotEmpty(manga.getExisting())) {
			old.getItems().addAll(manga.getExisting());
			FXCollections.reverse(old.getItems());	
		}
	}
}