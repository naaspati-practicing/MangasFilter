package samrock.filter.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;

public interface Counts {
    SimpleIntegerProperty thumbDownloaded = new SimpleIntegerProperty(); 
    SimpleIntegerProperty downloadHtml = new SimpleIntegerProperty(0);
    SimpleIntegerProperty fromCache = new SimpleIntegerProperty(0);
    
    SimpleIntegerProperty zeroNew = new SimpleIntegerProperty(0);
    SimpleIntegerProperty newChapters = new SimpleIntegerProperty();
    SimpleIntegerProperty selectedCount = new SimpleIntegerProperty();
    SimpleIntegerProperty errorCount = new SimpleIntegerProperty();
    SimpleIntegerProperty updatedRemoved = new SimpleIntegerProperty();
    SimpleIntegerProperty newRemoved = new SimpleIntegerProperty();
    
    static void increment(SimpleIntegerProperty field) {
        Platform.runLater(() ->  field.set(field.get() + 1));
    }
    static void add(SimpleIntegerProperty field, int delta) {
        if(delta == 0)
            return;
        Platform.runLater(() ->  field.set(field.get() + delta));
    }
    static void set(SimpleIntegerProperty field, int value) {
        Platform.runLater(() ->  field.set(value));
    }
}
