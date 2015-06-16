package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
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

	@FXML private BorderPane textPane;
	
	// Text
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
	
	private Parser parser;
	
	private Property<String> searchText;
	
	private BiConsumer<Predicate<DayOneEntry>, String> filterAction;
	
	@FXML
	public void initialize() {
		bindTextFilters();
		
		createDateParser();
	}
	
	private void bindTextFilters() {
		searchText = new SimpleStringProperty();
		
		textSearchLabel.textProperty().bind(searchText);
		
		searchText.addListener((obs, oldValue, newValue) -> {
			matchDates(newValue);
			matchTags(newValue);
			matchText(newValue);
		});
	}
	
	private void createDateParser() {
		parser = new Parser();

		onDateLabel.textProperty().bindBidirectional(beforeDateLabel.textProperty());
		beforeDateLabel.textProperty().bindBidirectional(afterDateLabel.textProperty());
	}
	
	private void matchDates(String text) {
		
		try {
			List<DateGroup> matches = parser.parse(text);
			
			DateTime date = new DateTime(matches.get(0).getDates().get(0));
			
			onDateLabel.setText(date.toString("EEEEEEEE - dd/MM/YYYY"));
			
			String dateText = date.toString("dd/MM/YYYY");
			
			onDateBox.setOnMouseClicked(event -> filterAction.accept(entry -> {
				return entry.getCreationDate().isAfter(date.withMillisOfDay(0))
						&& entry.getCreationDate().isBefore(date.withMillisOfDay(86399999));
			}, "On: " + dateText)); 
			
			beforeDateBox.setOnMouseClicked(event -> filterAction.accept(entry -> {
				return entry.getCreationDate().isBefore(date.withMillisOfDay(86399999));
			}, "Before: " + dateText)); 
			
			afterDateBox.setOnMouseClicked(event -> filterAction.accept(entry -> {
				return entry.getCreationDate().isAfter(date.withMillisOfDay(0));
			}, "After: " + dateText)); 
			
			datePane.setCenter(dateBox);
		} catch (Exception e) {
			datePane.setCenter(new Label("Enter a date to filter your entries"));
		}
	}
	
	private void matchTags(String text) {
		if (tags == null)
			return;
		
		ObservableList<Node> children = tagBox.getChildren();
		children.clear();
		
		for (Tag t : tags)
			if (t.getName().toLowerCase().contains(text.toLowerCase())) {
				VBox box = new VBox(new Label(t.getName()));
				box.setPadding(new Insets(5));
				box.getStyleClass().add("clickable");
				
				box.setOnMouseClicked(event -> filterAction.accept(entry -> entry.getTags().contains(t.getName()), "Tag: " + t.getName()));
				
				children.add(box);
			}
	}
	
	private void matchText(String text) {
		if (text.equals(""))
			textPane.setCenter(new Label("Enter a search term to filter your entries"));
		else
			textPane.setCenter(textBox);
		
		textBox.setOnMouseClicked(event -> filterAction.accept(
				entry -> entry.getEntryText().toLowerCase().contains(text.toLowerCase()), text));
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
	
}
