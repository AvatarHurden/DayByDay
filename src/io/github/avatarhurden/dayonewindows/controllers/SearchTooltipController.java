package io.github.avatarhurden.dayonewindows.controllers;

import java.util.Locale;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
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

	@FXML private BorderPane datePane;
	@FXML private VBox dateBox;
	@FXML private HBox endBox;
	@FXML private Label startDate, endDate;
	
	private Property<String> searchText;
	
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
		try {
			Options p = new Options(false);
			p.setContext(PointerType.PAST);
			p.setNow(new DateTime().withMillisOfDay(8639999).toCalendar(Locale.getDefault()));
			p.setAmbiguousTimeRange(24);
			
			final Span t = Chronic.parse(text, p);
			
			startDate.setText(new DateTime(t.getBeginCalendar()).toString("EEEEEEEE - dd/MM/YYYY"));
			endDate.setText(new DateTime(t.getEndCalendar()).toString("EEEEEEEE - dd/MM/YYYY"));
			if (new DateTime(t.getBeginCalendar()).withMillisOfDay(0).isEqual(new DateTime(t.getEndCalendar()).withMillisOfDay(0))) {
				endBox.setVisible(false);
			} else {
				endBox.setVisible(true);
				System.out.println("hi");
			}

			datePane.setCenter(dateBox);
			
	//		setPredicate(s -> s.getCreationDate().isAfter(new DateTime(t.getBeginCalendar())) 
	//			&& s.getCreationDate().isBefore(new DateTime(endT.getEndCalendar())));
		} catch (NullPointerException e) {
			datePane.setCenter(new Label("Enter a date to filter your entries"));
//			datePane.setVisible(false);
		}
	}
	
	private void matchTags(String text) {
		
	}
	
	private void matchText(String text) {
		if (text.equals(""))
			textPane.setCenter(new Label("Enter a search term to filter your entries"));
		else
			textPane.setCenter(textBox);
	}
	
	public Property<String> searchTextProperty() {
		return searchText;
	}
	
}
