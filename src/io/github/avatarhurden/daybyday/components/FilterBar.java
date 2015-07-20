package io.github.avatarhurden.daybyday.components;

import io.github.avatarhurden.daybyday.controllers.SearchTooltipController;
import io.github.avatarhurden.daybyday.managers.Config;
import io.github.avatarhurden.daybyday.models.Entry;
import io.github.avatarhurden.daybyday.models.JournalEntry;
import io.github.avatarhurden.daybyday.models.MonthEntry;
import io.github.avatarhurden.daybyday.models.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.util.Pair;
import jfxtras.scene.control.ListView;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

public class FilterBar extends HBox {

	private Runnable onSelected, onUnselected;
	private TextField text;
	
	private CloseButton clearAll;
	
	private ObservableList<Predicate<Entry>> predicates;
	private HashMap<HBox, Predicate<Entry>> boxMap;
	private ListView<Pair<String, Predicate<Entry>>> overflowPredicates;
	private Button overflowButton;
	
	private PopOver popOver;
	private SearchTooltipController tooltipController;
	
	public FilterBar() {
		getStylesheets().add("style/FilterBar.css");

		initialize();
		bindPredicates();
		createToolTip();
	}

	private void initialize() {
		predicates = FXCollections.observableArrayList();
		
		clearAll = new CloseButton();
		Tooltip.install(clearAll, new Tooltip("Remove all filters"));
		clearAll.visibleProperty().bind(Bindings.isNotEmpty(predicates));
		
		clearAll.setOnAction(() -> {
			overflowPredicates.getItems().clear();
			for (Iterator<HBox> it = boxMap.keySet().iterator(); it.hasNext(); ) {
				removeFilterInstance(it.next());
				it.remove();
			}
			predicates.clear();
		});
		
		HBox.setMargin(clearAll, new Insets(0, 10, 0, 0));
		getChildren().add(clearAll);
		
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
		boxMap = new HashMap<HBox, Predicate<Entry>>();
		
		overflowPredicates = new ListView<>();
		overflowPredicates.setCellFactory(list -> new OverflowFilterCell());

		overflowPredicates.prefHeightProperty().bind(Bindings.size(overflowPredicates.getItems()).multiply(23).add(2));
		overflowPredicates.maxHeightProperty().set(10 * 23 + 2);
		
		overflowButton = new Button("V");
		overflowButton.visibleProperty().bind(Bindings.size(overflowPredicates.getItems()).greaterThan(0));
		overflowButton.managedProperty().bind(overflowButton.visibleProperty());
		
		overflowButton.setOnAction(event -> {
			PopOver over = new PopOver(overflowPredicates);
			over.setArrowLocation(ArrowLocation.TOP_CENTER);
			over.setDetachable(false);
			
			over.show(overflowButton);
		});
		
		getChildren().add(1, overflowButton);
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
    		Predicate<Entry> pred = (entry -> entry instanceof MonthEntry ? true : predicate.test((JournalEntry) entry));
    		predicates.add(pred);
    		addFilterInstance(pred, string);
    		text.clear();
    		text.requestFocus();
    	});
    	
	}
	
	private void addFilterInstance(Predicate<Entry> predicate, String textValue) {
		HBox box = getFilterBox(textValue);
		
		if (getChildren().size() == 3) {
			text.getStyleClass().add("right");
			box.getStyleClass().add("left");
			text.getStyleClass().remove("unique");
		}
		
		double estimatedWidth = new Text(textValue).getLayoutBounds().getWidth() + 60;
		
		for (Node b : getChildren().filtered(node -> node instanceof HBox))
			estimatedWidth += ((HBox) b).getWidth();
		
		if (estimatedWidth > getPrefWidth() - 150)
			overflowPredicates.getItems().add(new Pair<String, Predicate<Entry>>(textValue, predicate));
		else {
			getChildren().add(getChildren().size() - 2, box);
			boxMap.put(box, predicate);
		}
	}
	
	private void removeFilterInstance(HBox box) {
		predicates.remove(boxMap.get(box));
		
		getChildren().remove(box);
		
		if (getChildren().size() > 3)
			getChildren().get(0).getStyleClass().add("left");
		else if (getChildren().size() == 3) {
			text.getStyleClass().add("unique");
			text.getStyleClass().remove("right");
		}
		
		double width = 0;
		for (Node b : getChildren().filtered(node -> node instanceof HBox))
			width += ((HBox) b).getWidth();
		
		if (width < getPrefWidth() - 150 && !overflowPredicates.getItems().isEmpty()) {
			Pair<String, Predicate<Entry>> pair = overflowPredicates.getItems().get(0);
			overflowPredicates.getItems().remove(pair);
			addFilterInstance(pair.getValue(), pair.getKey());
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
			boxMap.remove(itemBox);
			this.text.requestFocus();
		});

        itemBox.getChildren().add(deleteButton);

		return itemBox;
	}
	
	public ObservableList<Predicate<Entry>> getFilters() {
		return predicates;
	}
	
	public Predicate<Entry> getCombinedFilter() {
		boolean all = Config.get().getProperty("list_filter_system").equals("all");
		Predicate<Entry> filter = entry -> all;
		
		for (int i = 0; i < predicates.size(); i++)
			if (all)
				filter = filter.and(predicates.get(i));
			else
				filter = filter.or(predicates.get(i));
				
		if (predicates.size() == 0)
			filter = entry -> true;
		
		return filter;
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
	
	private class OverflowFilterCell extends ListCell<Pair<String, Predicate<Entry>>> {
		
		@Override public void updateItem(Pair<String, Predicate<Entry>> item, boolean empty) {
	        super.updateItem(item, empty);
			if (empty || item == null) {
				setGraphic(null);
			} else {
				BorderPane pane = new BorderPane();
				
				Label label = new Label(item.getKey());
				BorderPane.setAlignment(label, Pos.CENTER_LEFT);
				pane.setCenter(label);
				
				CloseButton button = new CloseButton();
				button.setOnAction(() -> {
					predicates.remove(item.getKey());
					overflowPredicates.getItems().remove(item);
				});
				pane.setRight(button);
				setGraphic(pane);
			}
			
		}
	}
	
}
