package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.joda.time.DateTime;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

public class SearchTooltipController {

	@FXML private AnchorPane root;
	
	// Text
	@FXML private BorderPane textPane;
	@FXML private Text textSearchLabel;
	@FXML private TextFlow textBox;
	
	// Tag
	@FXML private BorderPane tagPane;
	@FXML private FlowPane tagBox;
	private ObservableList<Tag> tags;
	
	// Date
	@FXML private BorderPane datePane;
	@FXML private VBox dateBox;
	@FXML private HBox onDateBox, beforeDateBox, afterDateBox;
	@FXML private Label onDateLabel, beforeDateLabel, afterDateLabel;
	
	private ObservableList<Node> tabOrder;
	private int tabPosition;
	
	private Parser parser;
	
	private Property<String> searchText;
	private Property<DateTime> date;
	
	private BiConsumer<Predicate<DayOneEntry>, String> filterAction;
	
	@FXML
	public void initialize() {
		bindFilters();
		
		createDateParser();
		
		tabOrder = FXCollections.observableArrayList();
		root.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.DOWN) {
				if (tabPosition < tabOrder.size() - 1)
					tabPosition++;
				tabOrder.get(tabPosition).requestFocus();
				event.consume();
			} else if (event.getCode() == KeyCode.UP) {
				if (tabPosition > 0)
					tabPosition--;
				tabOrder.get(tabPosition).requestFocus();
				event.consume();
			}
		});
	}
	
	private void bindFilters() {
		searchText = new SimpleStringProperty();
		date = new SimpleObjectProperty<DateTime>();
		
		bindText();
		bindDates();
		
		searchText.addListener(event -> {
			matchText(searchText.getValue());
			matchDates(searchText.getValue());
			matchTags(searchText.getValue());
		});
	}
	
	private void bindText() {
		textBox.setOnMouseClicked(event -> filterAction.accept(
				entry -> entry.getEntryText().toLowerCase().contains(searchText.getValue().toLowerCase()), searchText.getValue()));
		textBox.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				filterAction.accept(entry -> entry.getEntryText().toLowerCase().contains(searchText.getValue().toLowerCase()), searchText.getValue());
				event.consume();
			}
		});
		
		textSearchLabel.textProperty().bind(searchText);
	}

	private void bindDates() {
		Runnable onDateAction = () -> filterAction.accept(entry -> {
			return entry.getCreationDate().isAfter(date.getValue().withMillisOfDay(0))
					&& entry.getCreationDate().isBefore(date.getValue().withMillisOfDay(86399999));
		}, "On: " + date.getValue().toString("dd/MM/YYYY"));
		
		onDateBox.setOnMouseClicked(event -> onDateAction.run()); 
		
		onDateBox.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER)
				onDateAction.run();
		});
		
		Runnable beforeDateAction = () -> filterAction.accept(entry -> {
			return entry.getCreationDate().isBefore(date.getValue().withMillisOfDay(86399999));
		}, "Before: " + date.getValue().toString("dd/MM/YYYY"));
		
		beforeDateBox.setOnMouseClicked(event -> beforeDateAction.run()); 
		
		beforeDateBox.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER)
				beforeDateAction.run();
		});
		
		Runnable afterDateAction = () -> filterAction.accept(entry -> {
			return entry.getCreationDate().isAfter(date.getValue().withMillisOfDay(0));
		}, "After: " + date.getValue().toString("dd/MM/YYYY"));
		
		afterDateBox.setOnMouseClicked(event -> afterDateAction.run()); 
		
		afterDateBox.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER)
				afterDateAction.run();
		});
	}
	
	private void createDateParser() {
		parser = new Parser();
		new Thread(() -> { // First parse on natty takes longer, so this will make the program smoother when first searching
			parser.parse("today");
		}).start();

		onDateLabel.textProperty().bindBidirectional(beforeDateLabel.textProperty());
		beforeDateLabel.textProperty().bindBidirectional(afterDateLabel.textProperty());
	}
	
	private void matchDates(String text) {
		
		try {
			List<DateGroup> matches = parser.parse(text);
			
			date.setValue(new DateTime(matches.get(0).getDates().get(0)));
			
			onDateLabel.setText(date.getValue().toString("dd/MM/YYYY"));
			
			datePane.setCenter(dateBox);
			
			if (tabOrder.size() == 1)
				tabOrder.addAll(onDateBox, beforeDateBox, afterDateBox);
			else {
				tabOrder.set(1, onDateBox);
				tabOrder.set(2, beforeDateBox);
				tabOrder.set(3, afterDateBox);
			}
		} catch (Exception e) {
			date.setValue(null);
			datePane.setCenter(new Label("Enter a date to filter your entries"));
			tabOrder.remove(onDateBox);
			tabOrder.remove(beforeDateBox);
			tabOrder.remove(afterDateBox);
		}
	}
	
	private void matchTags(String text) {
		if (tags == null)
			return;
		
		ObservableList<Node> children = tagBox.getChildren();
		tabOrder.removeAll(children);
		children.clear();
		
		for (Tag t : tags)
			if (t.getName().toLowerCase().contains(text.toLowerCase())) {
				VBox box = new VBox(new Label(t.getName()));
				box.setPadding(new Insets(5));
				box.getStyleClass().add("clickable");
				
				box.setOnMouseClicked(event -> filterAction.accept(entry -> entry.getTags().contains(t.getName()), "Tag: " + t.getName()));
				box.setOnKeyPressed(event -> {
					if (event.getCode() == KeyCode.ENTER) {
						filterAction.accept(entry -> entry.getTags().contains(t.getName()), "Tag: " + t.getName());
						event.consume();
					}
				});
				children.add(box);
				tabOrder.add(box);
			}
	}
	
	private void matchText(String text) {
		if (text.equals("")) {
			textPane.setCenter(new Label("Enter a search term to filter your entries"));
			tabOrder.remove(textBox);
		} else {
			textPane.setCenter(textBox);
			tabOrder.set(0, textBox);
		}
	}
	
	public void setOnFilterSelection(BiConsumer<Predicate<DayOneEntry>, String> consumer) {
		filterAction = consumer;
	}
	
	public Property<String> searchTextProperty() {
		return searchText;
	}
	
	public void setTags(ObservableList<Tag> tags) {
		this.tags = tags;
		matchTags(searchText.getValue());
	}

	public void requestFocus() {
		tabOrder.get(0).requestFocus();
		tabPosition = 0;
	}

	public boolean hasFocus() {
		for (Node n : tabOrder)
			if (n.isFocused())
				return true;
		return false;
	}
	
}
