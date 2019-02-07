package samrock.filter.view;

import static samrock.filter.view.Utils.fx;

import java.util.Collection;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
	ObservableList<String> activity = FXCollections.observableArrayList();

    static void increment(SimpleIntegerProperty field) {
        fx(() ->  field.set(field.get() + 1));
    }
    static void add(SimpleIntegerProperty field, int delta) {
        if(delta == 0)
            return;
        fx(() ->  field.set(field.get() + delta));
    }
    static void set(SimpleIntegerProperty field, int value) {
        fx(() ->  field.set(value));
    }
    static <E> void add(Collection<E> collection, E value) {
    	fx(() -> collection.add(value));
    }
    static <E> void remove(Collection<E> collection, E value) {
    	fx(() -> collection.remove(value));
    }
}
