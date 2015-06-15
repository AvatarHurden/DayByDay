package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;

import java.util.Locale;
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

import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer.PointerType;
import com.mdimension.jchronic.utils.Span;

public class SearchTooltipController {

	@FXML private BorderPane textPane;
	@FXML private Text textSearchLabel;
	@FXML private TextFlow textBox;
	
	@FXML private BorderPane tagPane;
	@FXML private FlowPane tagBox;
	private ObservableList<Tag> tags;
	
	@FXML private BorderPane datePane;
	@FXML private VBox dateBox;
	@FXML private HBox endBox;
	@FXML private Label startDate, endDate;
	
	private Property<String> searchText;
	
	private BiConsumer<Predicate<DayOneEntry>, String> filterAction;
	
	@FXML
	public void initialize() {
		bindTextFilters();

		endBox.managedProperty().bind(endBox.visibleProperty());
		datePane.managedProperty().bind(datePane.visibleProperty());
		
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
	
	private void matchDates(String text) {
		Options p = new Options(false);
		p.setContext(PointerType.PAST);
		p.setNow(new DateTime().withMillisOfDay(8639999).toCalendar(Locale.getDefault()));
		p.setAmbiguousTimeRange(24);
		
		try {
			final Span t = Chronic.parse(text, p);
			
			startDate.setText(new DateTime(t.getBeginCalendar()).toString("EEEEEEEE - dd/MM/YYYY"));
			endDate.setText(new DateTime(t.getEndCalendar()).toString("EEEEEEEE - dd/MM/YYYY"));
			
			DateTime start = new DateTime(t.getBeginCalendar());
			DateTime end = new DateTime(t.getEndCalendar());
			
			String dateText;
			
			if (start.withMillisOfDay(0).isEqual(end.withMillisOfDay(0)) 
					|| (start.getMillisOfDay() == 0 && t.getEnd() - t.getBegin() <= 86400)) { // If the span is a single day 
				endBox.setVisible(false);
				dateText = start.toString("dd/MM/YYYY");
			} else {
				endBox.setVisible(true);
				dateText = start.toString("dd/MM/YYYY") + " - " + end.toString("dd/MM/YYYY");
			}
			dateBox.setOnMouseClicked(event -> filterAction.accept(entry -> {
				return entry.getCreationDate().isAfter(start.withMillisOfDay(0))
						&& entry.getCreationDate().isBefore(end.withMillisOfDay(86399999));
			}, dateText)); 
			
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
	}
	
}
