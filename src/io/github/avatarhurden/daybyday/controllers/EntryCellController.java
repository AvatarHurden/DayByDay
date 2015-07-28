package io.github.avatarhurden.daybyday.controllers;

import io.github.avatarhurden.daybyday.managers.Config;
import io.github.avatarhurden.daybyday.models.JournalEntry;

import java.util.Arrays;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import org.joda.time.DateTime;

public class EntryCellController {

	@FXML private AnchorPane root;
	
	@FXML private Text timeLabel, dayOfWeekLabel, dayOfMonthLabel;
	private Property<DateTime> date;
	
	@FXML private HBox tagPane;
	private ObservableList<String> tags;
	
	@FXML private ImageView imageView;

	@FXML private VBox textBox;
	@FXML private Label titleLabel, bodyLabel;
	private StringProperty text;
	
	@FXML private SVGPath favoriteIcon;
	private BooleanProperty isStarred;
	
	private DoubleProperty width;
	
	private JournalEntry current;
	
	@FXML
	private void initialize() {
		text = new SimpleStringProperty();
		text.addListener((obs, oldValue, newValue) -> {
			String[] lines = newValue.split("\n");
			String body;
			if (Config.get().getBoolean("bold_titles", true) 
					&& lines.length > 0
					&& lines[0].length() > 0 && lines[0].length() <= 140 
					&& Character.isAlphabetic(lines[0].charAt(0))) {
				titleLabel.setManaged(true);
				titleLabel.setText(lines[0]);
				
				int start = 1;
				while (start < lines.length && lines[start].isEmpty())
					start++;
				
				body = String.join("\n", Arrays.asList(lines).subList(start, lines.length));
				bodyLabel.setText(body);
			} else {
				titleLabel.setText("");
				titleLabel.setManaged(false);
				bodyLabel.setText(newValue);
			}
		});

		titleLabel.setWrapText(true);
		bodyLabel.setWrapText(true);
		
		bindImagePane();

		bindDateTexts();
		
		bindStarredColors();

		bindTagPane();
		
		width = new SimpleDoubleProperty();
		root.prefWidthProperty().bind(width);
	}
	
	public void setContent(JournalEntry entry) {
		text.bind(entry.entryTextProperty());
		
		if (current != null)
			Bindings.unbindContent(tags, current.getObservableTags());
		Bindings.bindContent(tags, entry.getObservableTags());
		
		isStarred.bind(entry.starredProperty());
		
		date.bind(entry.creationDateProperty());
		
		if (current != null)
			current.decrementImageReferences();
		imageView.imageProperty().bind(entry.imageProperty());
		entry.incrementImageReferences();
		
		current = entry;
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
		tags = FXCollections.observableArrayList();
		
		tags.addListener((ListChangeListener.Change<? extends String> event) -> {
			ObservableList<Node> panes = tagPane.getChildren();
			panes.clear();
			for (String t : tags) {
				HBox tagBox = new HBox(new Label(t));
				tagBox.setPadding(new Insets(3, 6, 3, 6));
				tagBox.getStyleClass().add("tag");
				panes.add(tagBox);
			}

			tagPane.setPrefHeight(tags.isEmpty() ? 0 : 31);
			AnchorPane.setBottomAnchor(textBox, tags.isEmpty() ? 10d : 31d);
		});
		
		tagPane.prefWidthProperty().bindBidirectional(textBox.prefWidthProperty());
		
	}
	
	private void bindImagePane() {
		imageView.visibleProperty().bind(imageView.imageProperty().isNotNull());
		
		imageView.managedProperty().bind(imageView.visibleProperty());
		
		imageView.imageProperty().addListener((obs, oldValue, newValue) -> {
			AnchorPane.setLeftAnchor(textBox, newValue == null ? 90d : 190d);
			AnchorPane.setLeftAnchor(tagPane, newValue == null ? 90d : 190d);
			textBox.prefWidthProperty().bind(width.subtract(newValue == null ? 90 : 190));
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
