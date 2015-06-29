package io.github.avatarhurden.dayonewindows.components;

import io.github.avatarhurden.dayonewindows.models.JournalEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.controlsfx.control.textfield.TextFields;

public class TagEditor extends VBox {

	private JournalEntry entry;
	
	private ObservableList<String> addedTags;
	private ObservableList<Tag> possibleTags;
	private FilteredList<Tag> existingTags;
	
	private TextField textField;
	private ListView<String> addedTagsView;
	private ListView<Tag> existingTagsView;
	
	public TagEditor() {
		super(5);
		setAlignment(Pos.TOP_CENTER);
		setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		getStylesheets().add("style/TagEditor.css");
		
		addedTags = FXCollections.observableArrayList();
		possibleTags = FXCollections.observableArrayList();
		
		existingTags = possibleTags.filtered(tag -> !addedTags.contains(tag.getName()));
	
		initialize();
	}
	
	public TagEditor(JournalEntry entry) {
		this();
		
		setEntry(entry);
	}
	
	private void initialize() {
		textField = TextFields.createClearableTextField();
		VBox.setMargin(textField, new Insets(5));
		textField.setOnAction(event -> {
			entry.addTag(textField.getText());
			textField.clear();
		});

		textField.textProperty().addListener(event -> {
			existingTags.setPredicate(tag -> tag.getName().toLowerCase().contains(textField.getText().toLowerCase())
					&& !addedTags.contains(tag.getName()));
		});
		
		textField.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.DOWN) {
				existingTagsView.getSelectionModel().select(0);
				existingTagsView.requestFocus();
			} else if (event.getCode() == KeyCode.UP) {
				addedTagsView.getSelectionModel().select(addedTagsView.getItems().size() - 1);
				addedTagsView.requestFocus();
			}
		});
		
		addedTagsView = new ListView<String>(addedTags);
		addedTagsView.visibleProperty().bind(Bindings.size(addedTags).greaterThan(0));
		addedTagsView.setCellFactory(view -> new AddedTagCell());
		addedTagsView.prefHeightProperty().bind(Bindings.size(addedTagsView.getItems()).multiply(23).add(4));
		addedTagsView.maxHeightProperty().set(6 * 23 + 4);
		
		addedTagsView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.DOWN && 
					addedTagsView.getSelectionModel().getSelectedIndex() == addedTagsView.getItems().size() - 1)
				textField.requestFocus();
			else if (event.getCode() == KeyCode.DELETE) {
				entry.removeTag(addedTagsView.getSelectionModel().getSelectedItem());
				textField.clear();
			}
		});
		
		existingTagsView = new ListView<Tag>(existingTags);
		existingTagsView.visibleProperty().bind(Bindings.size(existingTags).greaterThan(0));
		existingTagsView.setCellFactory(view -> new PossibleTagCell());
		existingTagsView.prefHeightProperty().bind(Bindings.size(existingTagsView.getItems()).multiply(23).add(2));
		existingTagsView.maxHeightProperty().set(6 * 23 + 2);
		
		existingTagsView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)
				entry.addTag(existingTagsView.getSelectionModel().getSelectedItem().getName());
		});
		
		existingTagsView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				entry.addTag(existingTagsView.getSelectionModel().getSelectedItem().getName());
				textField.clear();
			} if (event.getCode() == KeyCode.UP && existingTagsView.getSelectionModel().getSelectedIndex() == 0)
				textField.requestFocus();
		});
		
		Label label = new Label("All tags");
		label.setAlignment(Pos.CENTER);
		
		getChildren().setAll(addedTagsView, textField, label, existingTagsView);
	}
	
	public void setPossibleTags(ObservableList<Tag> possible) {
		Bindings.bindContent(possibleTags, possible);
	}
	
	public void setEntry(JournalEntry entry) {
		this.entry = entry;
		Bindings.bindContent(addedTags, entry.getObservableTags());
	}
	
	private class AddedTagCell extends ListCell<String> {
		
		@Override public void updateItem(String item, boolean empty) {
	        super.updateItem(item, empty);
			if (empty || item == null) {
				setGraphic(null);
			} else {
				BorderPane pane = new BorderPane();
				
				Label label = new Label(item);
				BorderPane.setAlignment(label, Pos.CENTER_LEFT);
				pane.setCenter(label);
				
				CloseButton button = new CloseButton();
				button.setOnAction(() -> entry.removeTag(item));
				pane.setRight(button);
				setGraphic(pane);
			}
			
		}
	}
	
	private class PossibleTagCell extends ListCell<Tag> {
		
		@Override public void updateItem(Tag item, boolean empty) {
	        super.updateItem(item, empty);
	        
			if (empty || item == null) {
				setGraphic(null);
			} else {
				BorderPane pane = new BorderPane();
				
				pane.setRight(new Label(""+item.getEntries().size()));
				
				Label label = new Label(item.getName());
				BorderPane.setAlignment(label, Pos.CENTER_LEFT);
				pane.setCenter(label);
				
				setGraphic(pane);
			}
			
		}
	}
}
