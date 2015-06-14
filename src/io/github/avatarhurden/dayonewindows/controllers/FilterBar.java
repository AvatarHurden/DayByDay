package io.github.avatarhurden.dayonewindows.controllers;


import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;

import java.io.IOException;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

public class FilterBar extends HBox {

	private Runnable onSelected, onUnselected;
	private TextField text;
	
	private ObservableList<Predicate<Entry>> predicates;
	
	private PopOver popOver;
	private SearchTooltipController tooltipController;
	
	public FilterBar() {
		getStylesheets().add("style/FilterBar.css");

		setPrefWidth(230);

		bindPredicates();
		initialize();
		createToolTip();
	}

	private void initialize() {
		text = new TextField();
		text.getStyleClass().add("unique");
		text.setPromptText("Search");
		
		text.setOnMouseClicked(event -> {
    		if (event.getClickCount() == 2)
    			popOver.show(text);
    	});
		
    	text.focusedProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue)
    			onSelected.run();
    		else
				onUnselected.run();
			
		});
    	
		setHgrow(text, Priority.ALWAYS);
		
		getChildren().add(text);
	}
	
	private void bindPredicates() {
		predicates = FXCollections.observableArrayList();
	
		predicates.addListener((ListChangeListener.Change<? extends Predicate<Entry>> event) -> {
			while (event.next()) {
				if (event.wasAdded())
					for (Predicate<Entry> predicate : event.getAddedSubList())
						addFilterInstance(predicate);
				if (event.wasRemoved())
					for (Predicate<Entry> predicate : event.getRemoved())
						removeFilterInstance(predicate);
			}
		});
	}
	
	private void createToolTip() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SearchToolTip.fxml"));
		AnchorPane content = null;
    	try {
    		content = loader.<AnchorPane>load();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	popOver = new PopOver(content);
    	popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
    	popOver.setDetachable(false);
    	
    	tooltipController = loader.<SearchTooltipController>getController();
    	tooltipController.searchTextProperty().bind(text.textProperty());
    	
    	tooltipController.setOnFilterSelection(predicate -> {
    		predicates.add(entry -> entry instanceof MonthEntry ? true : predicate.test((DayOneEntry) entry));
    	});
    	
	}
	
	private void addFilterInstance(Predicate<Entry> predicate) {
		if (getChildren().size() == 1) {
			text.getStyleClass().add("right");
			text.getStyleClass().remove("unique");
		}
		
//		getChildren().add(predicates.size(), getFilterBox());
		
	}
	
	private void removeFilterInstance(Predicate<Entry> predicate) {
		
		
		if (getChildren().size() == 1) {
			text.getStyleClass().add("unique");
			text.getStyleClass().remove("right");
		}
	}
	
	private HBox getFilterBox(String text) {
		HBox itemBox = new HBox(0);
		itemBox.getStyleClass().add("filter-box");
		
		Label label = new Label(text);
        itemBox.getChildren().add(label);
        
		return itemBox;
	}
	
	public ObservableList<Predicate<Entry>> getFilters() {
		return predicates;
	}
	
	
	public void setAvailableTags(ObservableList<Tag> tags) {
		tooltipController.setTags(tags);
	}

	public void setOnSelected(Runnable event) {
		this.onSelected = event;
	}
	
	public void setOnUnselected(Runnable event) {
		this.onUnselected = event;
	}

	public void showPopup() {
		popOver.show(text);
	}

	public void hidePopup() {
		popOver.hide();
	}
	
}
