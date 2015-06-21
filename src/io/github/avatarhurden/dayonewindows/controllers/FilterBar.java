package io.github.avatarhurden.dayonewindows.controllers;


import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;


import java.io.IOException;
import java.util.HashMap;
import java.util.function.Predicate;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

public class FilterBar extends HBox {

	private Runnable onSelected, onUnselected;
	private TextField text;
	
	private ObservableList<Predicate<Entry>> predicates;
	private HashMap<HBox, Predicate<Entry>> boxMap;
	
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
		
		text.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.DOWN && !tooltipController.hasFocus())
				tooltipController.requestFocus();
		});
		
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
		boxMap = new HashMap<HBox, Predicate<Entry>>();
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
    	
    	tooltipController.setOnFilterSelection((predicate, string) -> {
    		Predicate<Entry> pred = (entry -> entry instanceof MonthEntry ? true : predicate.test((DayOneEntry) entry));
    		predicates.add(pred);
    		addFilterInstance(pred, string);
    		text.clear();
    		text.requestFocus();
    	});
    	
	}
	
	private void addFilterInstance(Predicate<Entry> predicate, String textValue) {
		HBox box = getFilterBox(textValue);
		
		if (getChildren().size() == 1) {
			text.getStyleClass().add("right");
			box.getStyleClass().add("left");
			text.getStyleClass().remove("unique");
		}
		
		boxMap.put(box, predicate);
		
		getChildren().add(predicates.size() - 1, box);
	}
	
	private void removeFilterInstance(HBox box) {
		predicates.remove(boxMap.get(box));
		boxMap.remove(box);
		
		getChildren().remove(box);
		
		if (getChildren().size() > 1)
			getChildren().get(0).getStyleClass().add("left");
		else if (getChildren().size() == 1) {
			text.getStyleClass().add("unique");
			text.getStyleClass().remove("right");
		}
	}
	
	private HBox getFilterBox(String text) {
		HBox itemBox = new HBox(0);
		itemBox.getStyleClass().add("filter-box");
		
		Label label = new Label(text);
		itemBox.getChildren().add(label);
        
        HBox.setMargin(label, new Insets(4, 5, 4, 0));
		
		CloseButton deleteButton = new CloseButton();
		deleteButton.visibleProperty().bind(itemBox.hoverProperty());
		
		deleteButton.setOnAction(() -> {
			removeFilterInstance(itemBox);
			this.text.requestFocus();
		});

        itemBox.getChildren().add(deleteButton);
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
		text.setText("");
		text.setOnMouseClicked(event -> {
			if (!popOver.isShowing())
				popOver.show(text);
		});
	}
	
	public void showPopup(int millis) {
		new Thread(() -> {
			try {
				Thread.sleep(millis);
			} catch (Exception e) {}
			Platform.runLater(() -> popOver.show(text));
		}).start();
	}

	public void hidePopup() {
		popOver.hide();
		text.setOnMouseClicked(null);
	}
	
}
