package io.github.avatarhurden.dayonewindows.components;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.animation.FadeTransition;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;

import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

public class ObjectListView<T> extends StackPane {

	public enum ObjectLayout {
		HORIZONTAL, VERTICAL;
	}
	
	private Pane box;
	
	private Callback<String, T> creationPolicy;
	private Consumer<T> deletionPolicy;
	private StringPropertyGetter<T> property;
	
	private AutoCompletionBinding<?> autoCompletion;
	
	private ObservableList<T> objectList;
	private HashMap<T, Node> objectNodes;
	
	private TextField textField;

	private boolean editable;
	private ObjectLayout layout;
	
	private Property<Number> itemHeight;
	
	public ObjectListView(StringPropertyGetter<T> property, boolean editable, ObjectLayout layout) {
		this.editable = editable;
		this.property = property;
		this.layout = layout;
		itemHeight = new SimpleIntegerProperty(25);
		
		objectList = FXCollections.observableArrayList();
		
		getStylesheets().add("/style/objectListView.css");
	
		if (layout == ObjectLayout.VERTICAL) {
			box = new VBox();
			((VBox) box).setFillWidth(false);
		} else {
			box = new HBox();
			((HBox) box).setFillHeight(false);
		}
		
		objectNodes = new HashMap<T, Node>();
		
		getChildren().setAll(box);
		
		if (editable)
			addTextField();
	}
	
	public Integer getItemHeight() {
		return itemHeight.getValue().intValue();
	}
	
	public void setItemHeight(int itemHeight) {
		this.itemHeight.setValue(itemHeight);
	}
	
	public void setPromptText(String text) {
		textField.setPromptText(text);
	}
	
	public ObservableList<T> getList() {
		return objectList;
	}
	
	public void setList(ObservableList<T> objects) {
		// Clears the list of labels. If editable, maintains the textField. If not, remove everything
		ObservableList<Node> children = box.getChildren();
		if (children.size() > (editable ? 1 : 0))
			children.remove(0, editable ? children.size() - 1 : children.size());
		
		ListChangeListener<T> listener = (ListChangeListener.Change<? extends T> event) -> {
			while (event.next()) {
				if (event.wasRemoved())
					for (T removed : event.getRemoved()) {
						children.remove(objectNodes.get(removed));
						objectNodes.remove(removed);
					}
				if (event.wasAdded())
					for (T added : event.getAddedSubList())
						addLabel(added);
			}
		};
		
		if (objectList != null)
			objectList.removeListener(listener);
		objects.addListener(listener);
		
		for (T object : objects)
			addLabel(object);
		
		this.objectList.clear();
		this.objectList.addAll(objects);
	}

	public void setSuggestions(ObservableList<T> suggestions) {
		if (autoCompletion != null)
			autoCompletion.dispose();
		autoCompletion = TextFields.bindAutoCompletion(textField,
				request -> suggestions.filtered(t -> request.getUserText().length() > 0 
								&& t.toString().contains(request.getUserText().toLowerCase())));
	}
	
	public <S> void setSuggestions(ObservableList<S> suggestions, Predicate<Pair<S, String>> tester) {
		if (autoCompletion != null)
			autoCompletion.dispose();
		autoCompletion = TextFields.bindAutoCompletion(textField,
				request -> suggestions.filtered(t -> tester.test(new Pair<S, String>(t, request.getUserText()))));
	}
	
	
	/**
	 * Set the action to perform when trying to create a new object. This method should
	 * verify if the value is valid and, if it is invalid and should not be accepted, 
	 * the policy should return null.
	 * 
	 * @param policy
	 */
	public void setCreationPolicy(Callback<String, T> policy) {
		creationPolicy = policy;
	}

	/**
	 * Set the action to perform when deleting an object.
	 * 
	 * @param policy
	 */
	public void setDeletionPolicy(Consumer<T> policy) {
		deletionPolicy = policy;
	}
	
	private void addTextField() {
		textField = TextFields.createClearableTextField();
		textField.getStyleClass().add("creation-text");
		
		if (layout == ObjectLayout.VERTICAL)
			VBox.setMargin(textField, new Insets(5, 0, 5, 0));
		else
			HBox.setMargin(textField, new Insets(0, 5, 0, 5));
			
		setAlignment(Pos.CENTER_LEFT);
		
		textField.setOnAction((event) -> {
				T object = creationPolicy.call(textField.getText());
				if (object != null) {
					addLabel(object);
					textField.clear();
				}
		});
		box.getChildren().add(textField);
	}
	
	private void addLabel(T object) {
		// Do not created duplicate nodes
		if (objectNodes.containsKey(object))
			return;
		
		HBox itemBox = new HBox(0);
		itemBox.setCursor(Cursor.HAND);
		itemBox.setPadding(new Insets(0));
		itemBox.getStyleClass().add("object-box");
		
		itemBox.setFocusTraversable(true);
		itemBox.setOnMousePressed((event) -> {
			event.consume();
			itemBox.requestFocus();
		});
		
		Label label = new Label();
		label.textProperty().bindBidirectional(property.getProperty(object));
		label.prefHeightProperty().bind(itemHeight);
		
        itemBox.getChildren().add(label);
		
        if (editable) {
    		HBox.setMargin(label, new Insets(0, 5, 0, 0));
    		
    		CloseButton clearButton = new CloseButton();
    		clearButton.visibleProperty().bind(itemBox.hoverProperty());
	
			itemBox.hoverProperty().addListener((obs, oldValue, newValue) -> fadeObject(clearButton, newValue));
	        
			Runnable delete = () -> {
				fadeObject(itemBox, false).setOnFinished(finished -> getChildren().remove(itemBox));
				textField.requestFocus();
				deletionPolicy.accept(object);
			};
		
			clearButton.setOnAction(() -> delete.run());
			itemBox.setOnKeyPressed(event -> { if (event.getCode().equals(KeyCode.DELETE)) delete.run(); });
			
	        itemBox.getChildren().add(clearButton);
        }
        
        objectNodes.put(object, itemBox);
		
		box.getChildren().add(editable ? box.getChildren().size() - 1 : box.getChildren().size(), itemBox);

    	if (layout == ObjectLayout.VERTICAL)
			VBox.setMargin(itemBox, new Insets(5, 0, 5, 0));
		else
			HBox.setMargin(itemBox, new Insets(0, 5, 0, 5));
	}
	
	private FadeTransition fadeObject(Node object, boolean toVisible) {
		FadeTransition fader = new FadeTransition(Duration.millis(350), object);
		fader.setFromValue(toVisible ? 0.0 : 1.0);
		fader.setToValue(toVisible ? 1.0 : 0.0);
		fader.play();
		return fader;
	}
	
	public interface StringPropertyGetter<T> {
		public Property<String> getProperty(T object);
	}

	public void clearTextField() {
		textField.clear();
	}
}