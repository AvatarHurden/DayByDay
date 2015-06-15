package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.ObjectListView;
import io.github.avatarhurden.dayonewindows.managers.ObjectListView.ObjectLayout;
import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

public class EntryCellController {

	@FXML
	private AnchorPane root;
	@FXML
	private Text timeLabel, dayOfWeekLabel, dayOfMonthLabel;
	@FXML
	private AnchorPane tagPane;
	private ObjectListView<String> tagView;
	@FXML
	private ImageView imageView;
	@FXML
	private Label textLabel;	
	@FXML private SVGPath favoriteIcon;
	
	private DoubleProperty width;
	
	@FXML
	private void initialize() {
		imageView.visibleProperty().bind(imageView.imageProperty().isNotNull());
		imageView.managedProperty().bind(imageView.visibleProperty());
		
		tagView = new ObjectListView<String>(s -> new SimpleStringProperty(s), false, ObjectLayout.HORIZONTAL);
		tagView.setItemHeight(0);
		tagPane.getChildren().add(tagView);
		
//		textLabel.setFont(new Font(16));

		Text text = new Text(" ");
		textLabel.setPrefHeight(text.getLayoutBounds().getHeight() * 3);
		
		width = new SimpleDoubleProperty();
		root.prefWidthProperty().bind(width);
	}
	
	public void setContent(DayOneEntry entry) {

		textLabel.textProperty().bind(entry.entryTextProperty());
		if (entry.getImage() == null) {
			AnchorPane.setLeftAnchor(textLabel, 90d);
			textLabel.prefWidthProperty().bind(width.subtract(90));
		} else {
			AnchorPane.setLeftAnchor(textLabel, 190d);
			textLabel.prefWidthProperty().bind(width.subtract(190));
		}
			
		favoriteIcon.visibleProperty().bind(Bindings.when(dayOfMonthLabel.visibleProperty().not()).then(entry.isStarred()).otherwise(false));
		
		tagView.setList(entry.getObservableTags());
	
		dayOfMonthLabel.fillProperty().bind(Bindings.when(
				BooleanBinding.booleanExpression(entry.starredProperty())).then(Color.GOLD.darker()).otherwise(Color.BLACK));
		dayOfMonthLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			entry.getCreationDate().toString("dd"), entry.creationDateProperty()));

		dayOfWeekLabel.fillProperty().bind(Bindings.when(
				BooleanBinding.booleanExpression(entry.starredProperty())).then(Color.GOLD.darker()).otherwise(Color.BLACK));
		dayOfWeekLabel.textProperty().bind(Bindings.createStringBinding(() ->
			entry.getCreationDate().toString("EEE"), entry.creationDateProperty()));
		
		timeLabel.fillProperty().bind(Bindings.when(
				BooleanBinding.booleanExpression(entry.starredProperty())).then(Color.GOLD.darker()).otherwise(Color.BLACK));
		timeLabel.textProperty().bind(Bindings.createStringBinding(() ->
			entry.getCreationDate().toString("HH:mm"), entry.creationDateProperty()));

		imageView.imageProperty().bind(Bindings.createObjectBinding(() -> entry.getImage(), entry.imageProperty()));
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
