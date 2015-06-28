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

		textField.textProperty().addListener((obs, oldValue, newValue) -> {
			existingTags.setPredicate(tag -> tag.getName().toLowerCase().contains(newValue.toLowerCase())
					&& !addedTags.contains(tag.getName()));
		});
		
		addedTagsView = new ListView<String>(addedTags);
		addedTagsView.visibleProperty().bind(Bindings.size(addedTags).greaterThan(0));
		addedTagsView.setCellFactory(view -> new TagCell());
		addedTagsView.prefHeightProperty().bind(Bindings.size(addedTagsView.getItems()).multiply(23).add(4));
		addedTagsView.maxHeightProperty().set(6 * 23 + 4);
		
		existingTagsView = new ListView<Tag>(existingTags);
		existingTagsView.visibleProperty().bind(Bindings.size(existingTags).greaterThan(0));
		existingTagsView.setCellFactory(view -> new TagCell2());
		existingTagsView.prefHeightProperty().bind(Bindings.size(existingTagsView.getItems()).multiply(23).add(2));
		existingTagsView.maxHeightProperty().set(6 * 23 + 2);
		
		existingTagsView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				entry.addTag(existingTagsView.getSelectionModel().getSelectedItem().getName());
				System.out.println(addedTags.size());
			}
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
	
	private class TagCell extends ListCell<String> {
		
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
	
	private class TagCell2 extends ListCell<Tag> {
		
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
				
//				CloseButton button = new CloseButton();
//				button.setOnAction(() -> addedTags.remove(item));
//				pane.setRight(button);
				setGraphic(pane);
			}
			
		}
	}
}
