package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.ObjectListView;
import io.github.avatarhurden.dayonewindows.managers.ObjectListView.ObjectLayout;
import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import org.joda.time.DateTime;

public class EntryCellController {

	@FXML private AnchorPane root;
	
	@FXML private Text timeLabel, dayOfWeekLabel, dayOfMonthLabel;
	private Property<DateTime> date;
	
	@FXML private AnchorPane tagPane;
	private ObjectListView<String> tagView;
	
	@FXML private ImageView imageView;
	
	@FXML private Label textLabel;	
	
	@FXML private SVGPath favoriteIcon;
	private BooleanProperty isStarred;
	
	private DoubleProperty width;
	
	@FXML
	private void initialize() {
		bindImagePane();

		bindDateTexts();
		
		bindStarredColors();

		bindTagPane();
		
		textLabel.setWrapText(true);
		
		width = new SimpleDoubleProperty();
		root.prefWidthProperty().bind(width);
	}
	
	public void setContent(DayOneEntry entry) {
		textLabel.textProperty().bind(entry.entryTextProperty());
		
		tagView.setList(entry.getObservableTags());
		
		isStarred.bind(entry.starredProperty());
		
		date.bind(entry.creationDateProperty());
		
		imageView.imageProperty().bind(entry.imageProperty());
	}
	
	private void bindDateTexts() {
		date = new SimpleObjectProperty<DateTime>(new DateTime());
		
		dayOfMonthLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			date.getValue().toString("dd"), date));

		dayOfWeekLabel.textProperty().bind(Bindings.createStringBinding(() ->
			date.getValue().toString("EEE"), date));
		
		timeLabel.textProperty().bind(Bindings.createStringBinding(() ->
			date.getValue().toString("HH:mm"), date));

	}
	
	private void bindStarredColors() {
		isStarred = new SimpleBooleanProperty();
		
		dayOfMonthLabel.fillProperty().bind(Bindings.when(
				BooleanBinding.booleanExpression(isStarred)).then(Color.GOLD.darker()).otherwise(Color.BLACK));
		dayOfWeekLabel.fillProperty().bind(dayOfMonthLabel.fillProperty());
		timeLabel.fillProperty().bind(dayOfMonthLabel.fillProperty());
		
		favoriteIcon.visibleProperty().bind(Bindings.when(dayOfMonthLabel.visibleProperty().not()).then(isStarred).otherwise(false));
	}
	
	private void bindTagPane() {
		tagView = new ObjectListView<String>(s -> new SimpleStringProperty(s), false, ObjectLayout.HORIZONTAL);
		tagView.setItemHeight(0);
		tagView.getList().addListener((ListChangeListener.Change<? extends String> event) -> {
			tagPane.setPrefHeight(tagView.getList().isEmpty() ? 0 : 31);
			AnchorPane.setBottomAnchor(textLabel, tagView.getList().isEmpty() ? 10d : 31d);
		});
		tagPane.getChildren().add(tagView);
		
		tagPane.prefWidthProperty().bindBidirectional(textLabel.prefWidthProperty());
		
	}
	
	private void bindImagePane() {
		imageView.visibleProperty().bind(imageView.imageProperty().isNotNull());
		
		imageView.managedProperty().bind(imageView.visibleProperty());
		
		imageView.imageProperty().addListener((obs, oldValue, newValue) -> {
			AnchorPane.setLeftAnchor(textLabel, newValue == null ? 90d : 190d);
			AnchorPane.setLeftAnchor(tagPane, newValue == null ? 90d : 190d);
			textLabel.prefWidthProperty().bind(width.subtract(newValue == null ? 90 : 190));
		});
	}
	
	public void setDateEnabled(boolean enabled) {
		dayOfWeekLabel.setVisible(enabled);
		dayOfMonthLabel.setVisible(enabled);
	}

	public DoubleProperty widthProperty() {
		return width;
	}
	
	public void setWidth(double width) {
		this.width.setValue(width);
	}
	
}
