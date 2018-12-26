package samrock.filter.view;

import static sam.config.MyConfig.NEW_MANGAS_TSV_FILE;
import static sam.config.MyConfig.UPDATED_MANGAS_TSV_FILE;
import static sam.manga.samrock.mangas.MangasMeta.DIR_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.sql.querymaker.QueryMaker.qm;
import static samrock.filter.view.Unit.selected;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sam.console.ANSI;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxClassHelper;
import sam.fx.helpers.FxFxml;
import sam.fx.helpers.FxText;
import sam.fx.popup.FxPopupShop;
import sam.manga.samrock.SamrockDB;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.manga.scrapper.ChapterScrapListener;
import sam.manga.scrapper.ScrappedManga;
import sam.myutils.Checker;
import sam.tsv.Row;
import sam.tsv.Tsv;
public class Main extends Application implements Counts {
	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
			}
		});
		launch(args);
	}
	static Stage stg;
	static HostServices hs;
	final static int wrappingWidth = 180;

	public static Stage getStage() {
		return stg;
	}
	public static HostServices getHostService() {
		return hs;
	}

	@FXML private BorderPane root;
	@FXML private VBox status;
	@FXML private TabPane centerTabPane;
	@FXML private Tab updatedTab;
	@FXML private FlowPane updatedPane;
	@FXML private Tab newTab;
	@FXML private FlowPane newPane;

	Tsv updatedTsv, newTsv;
	int updatedTsvSize, newTsvSize;
	Set<Row> updateDelete, newDelete;
	final List<Manga> mangas = new ArrayList<>();
	final List<Unit> units = new ArrayList<>();
	ExecutorService primaryExecutor, secondaryExecutor;
	final VBox htmlLoading = new VBox(3);
	final VBox thumbLoading = new VBox(3);
	private final Scrapper scrapper = Scrapper.getInstance();

	static Main main;
	boolean redColor = true;
	private Mangarock mangarock;

	@Override
	public void start(Stage stage) throws Exception {
		Main.hs = getHostServices();
		Main.stg = stage;
		Main.main = this;
		FxAlert.setParent(stage);
		FxPopupShop.setParent(stage);

		FxFxml.load(this, stage, this);
		stage.getScene().getStylesheets().add("style.css");

		stage.show();
		stage.setMaximized(true);

		fill("Updated Mangas", UPDATED_MANGAS_TSV_FILE, Type.UPDATED);
		loadSamrock();
		fill("new Mangas", NEW_MANGAS_TSV_FILE, Type.NEW);

		this.mangarock = new Mangarock();
		mangarock.loadMangaRock(mangas);

		if(isEmpty(newTsv))
			centerTabPane.getTabs().remove(newTab);
		if(isEmpty(updatedTsv))
			centerTabPane.getTabs().remove(updatedTab);

		redColor = false;

		updatedTsvSize = size(updatedTsv);
		newTsvSize = size(newTsv);

		updateDelete = updatedTsvSize == 0 ? Collections.emptySet() : Collections.newSetFromMap(new IdentityHashMap<>());
		newDelete = newTsvSize == 0 ? Collections.emptySet() : Collections.newSetFromMap(new IdentityHashMap<>());

		status.getChildren()
		.addAll(FxText.text("TOTAL\nUpdated : "+updatedTsvSize+ "\nNew     : "+newTsvSize, "text"),
				new Separator(),
				text(thumbDownloaded, "Thumbs Downloaded"),
				text(fromCache, "From Cache"),
				text(downloadHtml, "html downloaded"),
				new Separator(),
				text(Bindings.size(updatedPane.getChildren()).asString(), "updated"),
				text(Bindings.size(newPane.getChildren()).asString(), "new"),
				text(zeroNew, "zero-new"),
				text(newChapters, "new-chapters"),
				text(selectedCount, "selected"),
				text(Counts.errorCount, "errors"),
				text(newRemoved, "new Removed"),
				text(updatedRemoved, "Updated Removed"),
				new Separator(),new Separator(),
				label("Html Loading"),
				htmlLoading,
				label("Thumb Loading"),
				thumbLoading,
				new Separator(),
				new VBox(3, removeButton(),
						selectZeros(),
						selectAllButton(true),
						selectAllButton(false),
						pane(),
						removeFromMangarock()
						)
				);

		loadData();
		stage.setOnCloseRequest(e -> {
			try {
				exit(e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
	}

	private int size(Tsv t) {
		return isEmpty(t) ? 0 : t.size();
	}
	private boolean isEmpty(Tsv t) {
		return t == null || t.isEmpty();
	}
	private Node selectZeros() {
		Button b = new Button("select Zero(s)");
		b.setOnAction(e -> units.forEach(u -> {
			if(u.isHtmlloaded())
				if(Checker.isEmpty(u.manga.getNew()))
					u.select();
		}));
		return b;
	}
	private Node pane() {
		Pane pane = new Pane();
		pane.setMinHeight(100);
		pane.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(pane, Priority.ALWAYS);
		return pane;
	}
	private Node selectAllButton(boolean select) {
		Button b = new Button(select ? "Select All" : "Unselect All");
		b.setOnAction(e -> selectedPane()
				.getChildren()
				.forEach(u -> { if(select) ((Unit)u).select(); else ((Unit)u).unselect();}));
		return b;
	}
	private Button removeFromMangarock() {
		Button b = new Button("Remove Mangarock");
		b.setTooltip(new Tooltip("Remove from mangarock Favorites"));
		b.disableProperty().bind(removeButton.disableProperty());

		b.setOnAction(e -> {
			int[] ids = removeAction();
			if(ids == null || ids.length == 0)
				return;
			try {
				mangarock.remove(ids);
			} catch (SQLException e2) {
				FxAlert.showErrorDialog(null, "failed to remove mangarock favorites", e);
			}
		});
		return b;
	}

	private Button removeButton;
	private Node removeButton() {
		removeButton = new Button("Remove Selected");
		FxClassHelper.addClass(removeButton, "remove-button");
		removeButton.disableProperty().bind(selectedCount.isEqualTo(0));
		removeButton.setOnAction(e -> removeAction());

		return removeButton;
	}
	private int[] removeAction() {
		if(!FxAlert.showConfirmDialog(null, "Confirm to remove"))
			return null;
		selectedPane()
		.getChildren()
		.removeAll(Unit.selected);

		selected.forEach(u -> {
			if(u.type == Type.UPDATED )
				updateDelete.add(u.manga.row0);
			else
				newDelete.add(u.manga.row0);
		});
		Map<Type, Long> map = selected.stream().collect(Collectors.groupingBy(u -> u.type, () -> new EnumMap<>(Type.class), Collectors.counting()));
		int[] ids = selected.stream().mapToInt(u -> u.manga.id).toArray();
		selected.clear();

		Platform.runLater(() -> {
			selectedCount.set(0);
			if(map.containsKey(Type.UPDATED))
				updatedRemoved.set((int) (updatedRemoved.get()+map.get(Type.UPDATED)));
			if(map.containsKey(Type.NEW))
				newRemoved.set((int) (newRemoved.get()+map.get(Type.NEW)));
		});

		return ids;

	}
	private FlowPane selectedPane() {
		return centerTabPane.getSelectionModel().getSelectedItem() == updatedTab ? updatedPane : newPane;
	}
	private Node label(String string) {
		Label l = new Label(string);
		l.setMaxWidth(Double.MAX_VALUE);
		l.setTextFill(Color.BLACK);
		FxClassHelper.addClass(l, "status-title");
		return l;
	}
	private Node text(Observable to, String string) {
		Text t = new Text();
		FxClassHelper.addClass(t, "text");
		t.textProperty().bind(Bindings.concat(string+" : ", to));
		return t;
	}
	private void loadData() throws IOException {
		primaryExecutor = Executors.newSingleThreadExecutor();
		secondaryExecutor = Executors.newFixedThreadPool(2);

		CompletableFuture.runAsync(() -> {
			for (Unit unit : units) {
				Path p = scrapper.findThumb(unit.manga.id);

				if(p != null && Files.exists(p))
					unit.setImage(p);
			}
		}, primaryExecutor).thenAccept(vd -> Platform.runLater(() -> {
			for (Unit u : units) {
				primaryExecutor.execute(() -> {
					Text t = FxText.text(u.manga.mangaName, "text");
					t.setWrappingWidth(wrappingWidth);
					try {
						Platform.runLater(() -> htmlLoading.getChildren().add(t));
						Manga m = u.manga;
						ScrappedManga sm = scrapper.scrapManga(m.url);

						m.setScrappedManga(sm);
						sm.getChapters(new ChapterScrapListener() {
							@Override
							public void onChapterSuccess(double number, String volume, String title, String url) {
								m.addChapter(new Chapter(number, title));
							}
							@Override
							public void onChapterFailed(String msg, Throwable e, String number, String volume, String title, String url) {
								System.out.printf(ANSI.red("chapter failed: msg: \"%s\" , number: \"%s\" , volume: \"%s\" , title: \"%s\" , url: \"%s\" "), msg, number, volume, title, url);
								e.printStackTrace();
							}
						});
						m.scrappingComplete();
						u.setHtmlLoaded();

						if(u.isThumbLoaded())
							return;

						secondaryExecutor.execute(new ThumbLoader(u, thumbLoading, scrapper));
					} catch (Exception e) {
						u.addError(e, "failed to extract chapters");
					} finally {
						Platform.runLater(() -> htmlLoading.getChildren().remove(t));
					}
				});
			}
		}));

		/** TODO
		 *         executor.execute(() -> {
        });
        units.forEach(u -> u.load(executor, u.type == Type.NEW ? newLatch : updatedLatch));

        catogerize(newLatch, newContainer);
        catogerize(updatedLatch, updatedContainer);
        executor.shutdown();
		 */
	}
	/* TODO
    private void catogerize(CountDownLatch latch, BorderPane pane) {
        if(latch != null) {
            primaryExecutor.execute(() -> {
                try {
                    latch.await();
                    Platform.runLater(() -> categorize(pane));
                } catch (InterruptedException e1) {
                    System.out.println("latch.await(): "+e1);
                }
            });
        }
    }

    private void categorize(BorderPane root) {
        if(root == null)
            return;

        FlowPane fpane = ((FlowPane)root.getCenter());

        Map<Integer, List<Unit>> map = fpane.getChildren()
                .stream()
                .map(Unit.class::cast)
                .collect(Collectors.groupingBy(u -> u.manga.getNew().size()));

        fpane.getChildren().clear();
        VBox box = new VBox(5);

        map.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> {
            UnitPane bp = new UnitPane("", false);
            bp.flowPane.getChildren().setAll(e.getValue());

            Button remove = new Button("select all");
            Button hide = new Button("hide");

            remove.setOnAction(e1 -> {
                List<Node> list = bp.flowPane.getChildren();
                while(!list.isEmpty())
                    ((Unit)list.get(0)).select();
            });

            String s = String.valueOf(e.getKey());
            bp.title.textProperty().bind(Bindings.concat(s, " (",Bindings.size(bp.flowPane.getChildren()), ")"));
            bp.getChildren().remove(bp.title);

            Pane pane = new Pane();
            pane.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(pane, Priority.ALWAYS);

            HBox buttons = new HBox(5, bp.title, pane, hide, remove);
            FxClassHelper.addClass(buttons, "title-label-box");
            bp.setTop(buttons);

            hide.setOnAction(e11 -> {
                if(hide.getText().equals("hide")) {
                    hide.setText("show");
                    bp.getChildren().remove(bp.flowPane);
                } else {
                    hide.setText("hide");
                    bp.setCenter(bp.flowPane);
                }
            });

            box.getChildren().addAll(bp);
        });
        root.setCenter(box);
    }
	 */
	private void loadSamrock() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
		if(mangas.isEmpty())
			return;

		String mangasSelect = qm()
				.select(MANGA_ID, DIR_NAME)
				.from(MANGAS_TABLE_NAME)
				.where(w -> w.in(MANGA_ID, mangas, m -> m.id,  false))
				.build();

		try(SamrockDB db = new SamrockDB()) {
			Map<Integer, ChapterFilter2> chapsMap = new ChapterUtils(db).getChapterFilters(mangas.stream().map(m -> m.id).collect(Collectors.toList()), null, ChapterFilter2::new);
			Map<Integer, String> dirMap = new HashMap<>();
			db.iterate(mangasSelect, rs -> dirMap.put(rs.getInt(MANGA_ID), rs.getString(DIR_NAME)));

			mangas.forEach(m -> m.setSamrock(chapsMap.get(m.id), dirMap.get(m.id)));
		}
	}
	public void exit(WindowEvent e2) throws Exception {
		if(mangarock != null)
			mangarock.close();

		if(updateDelete.isEmpty() && newDelete.isEmpty())
			System.exit(0);
		
		if(primaryExecutor != null) {
			primaryExecutor.shutdownNow();
			primaryExecutor.awaitTermination(3, TimeUnit.DAYS);
		}
		if(secondaryExecutor != null) {
			secondaryExecutor.shutdownNow();
			secondaryExecutor.awaitTermination(3, TimeUnit.DAYS);
		}
		
		String msg = "";
		if(!updateDelete.isEmpty())
			msg =  "updated : "+ updatedTsvSize +" -> "+ (updatedTsvSize - updateDelete.size())+"\n";
		if(!newDelete.isEmpty())
			msg += "new     : "+ newTsvSize +" -> "+ (newTsvSize - newDelete.size());

		ButtonType response =  
				FxAlert.alertBuilder(AlertType.CONFIRMATION)
				.header("Confirm changes")
				.content(msg)
				.buttons(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
				.showAndWait()
				.orElse(null);

		if(response == null || response == ButtonType.CANCEL) {
			e2.consume();
			return;
		}

		if(response == ButtonType.NO)
			System.exit(0);
		
		Thread t = new Thread(() -> {
			if(!updateDelete.isEmpty()) {
				try {
					updatedTsv.removesIf(updateDelete::contains);
					updatedTsv.save();
				} catch (IOException e) {
					FxAlert.showErrorDialog(UPDATED_MANGAS_TSV_FILE, "failed to save", e);
				}
			}
			if(!newDelete.isEmpty()) {
				try {
					newTsv.removesIf(newDelete::contains);
					newTsv.save(newTsv.getPath());
				} catch (IOException e) {
					FxAlert.showErrorDialog(NEW_MANGAS_TSV_FILE, "failed to save", e);
				}
			}
		});
		t.start();
		t.join();
	}


	
	void fill(String title, String pathString, Type type) {
		Path path = Paths.get(pathString);

		if(Files.notExists(path))
			return;
		try {
			final Tsv tsv = Tsv.parse(path);
			FlowPane root = type == Type.NEW ? newPane : updatedPane;

			tsv.stream()
			.map(Manga::new)
			.peek(mangas::add)
			.map(m -> new Unit(m, type))
			.peek(units::add)
			.forEach(root.getChildren()::add);

			if(type == Type.NEW) newTsv = tsv;
			else  updatedTsv = tsv;

		} catch (IOException e) {
			FxAlert.showErrorDialog(path, "failed to parse", e, false);
		}
	}
}
