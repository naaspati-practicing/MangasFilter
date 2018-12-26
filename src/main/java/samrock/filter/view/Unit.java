package samrock.filter.view;

import static sam.fx.helpers.FxClassHelper.addClass;
import static sam.fx.helpers.FxClassHelper.toggleClass;
import static samrock.filter.view.Counts.newChapters;
import static samrock.filter.view.Counts.selectedCount;
import static samrock.filter.view.Counts.zeroNew;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

class Unit extends StackPane {
    static final int MAX_WIDTH = Optional.ofNullable(System.getenv("MAX_WIDTH")).map(Integer::parseInt).orElse(120);
    final static Set<Unit> selected = Collections.newSetFromMap(new IdentityHashMap<>());

    final Manga manga;
    private Label data = label("");
    private transient boolean thumbLoaded;
    private transient boolean htmlloaded;
    final Type type;
    private StringBuilder logs;
    private Label title;

    public Unit(Manga manga, Type type) {
        this.type = type;

        addClass(this, "unit");
        this.manga = manga;
        title = label(manga.mangaName);

        addClass(title, "title");
        addClass(this, "layer");
        addClass(data, "data");

        BorderPane titlebox = new BorderPane(data);
        data.setAlignment(Pos.TOP_LEFT);
        BorderPane.setAlignment(data, Pos.TOP_LEFT);
        titlebox.setBottom(title);
        title.setTextAlignment(TextAlignment.CENTER);
        titlebox.setId("titlebox");
        getChildren().addAll(new Text(), titlebox);
        
        
        setMaxWidth(MAX_WIDTH);
        setOnContextMenuRequested(e -> ContextMenu2.getInstance().showContext(this, e));

        setOnMouseClicked(e -> {
            if(e.getButton() == MouseButton.SECONDARY)
                return;
            if (selected.contains(this)) unselect();
            else select();
        });
    }
    private Label label(String text) {
    	Label l = new Label(text);
    	l.setWrapText(true);
		return l;
	}
	public StringBuilder getLogs() {
        return logs;
    }
    public boolean isThumbLoaded() {
        return thumbLoaded;
    }
    public boolean isHtmlloaded() {
        return htmlloaded;
    }
   
    public void unselect() {
    	if(!selected.contains(this))
    		return;
        selected.remove(this);
        toggleClass(this, "selected", false);
        Counts.set(selectedCount, selected.size());
    }

    public void select() {
    	if(selected.contains(this))
    		return;
        selected.add(this);
        toggleClass(this, "selected", true);
        Counts.set(selectedCount, selected.size());
    }
    public void setImage(Path p) {
        if(p == null) {
            addError(null, "thumb not found");
            return;
        }
        try(InputStream is = Files.newInputStream(p)) {
            Image image = new Image(is, MAX_WIDTH, -1, true, true);
            Platform.runLater(() -> {
            	getChildren().set(0, new ImageView(image));
                if(!htmlloaded)
                    data.setText("loading html");
                thumbLoaded = true;
            });
        } catch (IOException e) {
            addError(e, "image loading error: ", e);
        }
    }
    public void setHtmlLoaded() {
        htmlloaded = true;
        
        if (manga.getNew().isEmpty()) Counts.increment(zeroNew);
        else Counts.add(newChapters, manga.getNew().size());
        
        Platform.runLater(() -> {
        	data.setText(String.format("%s/%s\n%s\n%s", 
        			manga.getNew().size(),manga.chaptersCount(),
        			manga.getStatus(),
        			manga.getRank()
        			));
        });
    }
    private static final Border red = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 0, 2, 0), Insets.EMPTY));

    public synchronized void addError(Throwable t, Object...msgs) {
        if(logs == null)
            logs = new StringBuilder().append(manga);

        if(msgs != null && msgs.length != 0)
            for (Object o : msgs) logs.append(o).append("\n");

        if(t != null) {
            StringWriter sb = new StringWriter();
            t.printStackTrace(new PrintWriter(sb));

            logs.append(sb).append("\n-------------------------------------------\n");
        }

        if(title.getBorder() != red) 
            Platform.runLater(() -> title.setBorder(red));
    }
}