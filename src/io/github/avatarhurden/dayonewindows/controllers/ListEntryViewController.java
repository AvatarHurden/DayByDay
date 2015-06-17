package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;
import io.github.avatarhurden.dayonewindows.models.Tag;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.util.Duration;


public class ListEntryViewController {

	@FXML private AnchorPane root;
	
	@FXML
	private SVGPath previousButton, homeButton, nextButton;
	@FXML
	private HBox buttonBar;
	@FXML private AnchorPane contentPane;
	@FXML private AnchorPane singleViewPane;
	
	private EntryViewController entryViewController;
	private AnchorPane entryView;
	
	@FXML private ListView<Entry> listView;

	private ObservableList<Entry> visibleItems;
	private DoubleProperty visibleSize;
	
	private Set<MonthEntry> months;
	
	private ObservableList<Entry> items;
	private FilteredList<Entry> filteredItems;
	
	@FXML private Label monthLabel;
	@FXML private AnchorPane listViewPane;
	
	@FXML private HBox filterPane; 
	private FilterBar filterBox;
	
	private SimpleIntegerProperty listSize;
	
	@FXML
	private void initialize() {
		months = new HashSet<MonthEntry>();
		
		listSize = new SimpleIntegerProperty();
		
		listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue instanceof DayOneEntry)
				entryViewController.setEntry((DayOneEntry) newValue);
		});
		
		visibleItems = FXCollections.<Entry>observableArrayList();
		visibleSize = new SimpleDoubleProperty(0);
		visibleItems.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			while (event.next()) {
				for (Entry t : event.getRemoved())
					visibleSize.setValue(visibleSize.getValue() - (t instanceof DayOneEntry ? 90 : 23));
				for (Entry t : event.getAddedSubList())
					visibleSize.setValue(visibleSize.getValue() + (t instanceof DayOneEntry ? 90 : 23));
			}
		});
		
		listView.setCellFactory(table -> new EntryCell());
		
		previousButton.strokeProperty().bind(Bindings.when(previousButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		nextButton.strokeProperty().bind(Bindings.when(nextButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		homeButton.strokeProperty().bind(Bindings.when(homeButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		
		nextButton.fillProperty().bind(Bindings.when(nextButton.disableProperty()).then(Color.LIGHTGRAY).otherwise(Color.BLACK));
		previousButton.fillProperty().bind(Bindings.when(previousButton.disableProperty()).then(Color.LIGHTGRAY).otherwise(Color.BLACK));
		
		previousButton.disableProperty().bind(listView.getSelectionModel().selectedIndexProperty().isEqualTo(0));
		nextButton.disableProperty().bind(listView.getSelectionModel().selectedIndexProperty().isEqualTo(listSize.subtract(1)));
		
		homeButton.setOnMouseClicked(event -> showList());
    	previousButton.setOnMouseClicked(event -> listView.getSelectionModel().selectPrevious());
    	nextButton.setOnMouseClicked(event -> listView.getSelectionModel().selectNext());
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EntryView.fxml"));
    	try {
    		entryView = loader.<AnchorPane>load();
	    	AnchorPane.setTopAnchor(entryView, 0d);
	    	AnchorPane.setRightAnchor(entryView, 0d);
	    	AnchorPane.setBottomAnchor(entryView, 0d);
	    	AnchorPane.setLeftAnchor(entryView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	entryViewController = loader.<EntryViewController>getController();
    	contentPane.getChildren().setAll(entryView);
    	
    	listView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER)
				showSingle();
		});
    	
    	entryView.setOnKeyPressed(event -> {
    		if (event.getCode() == KeyCode.LEFT) {
				listView.getSelectionModel().selectPrevious();
				event.consume();
    		} else if (event.getCode() == KeyCode.RIGHT) {
				listView.getSelectionModel().selectNext();
				event.consume();
    		} else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.ESCAPE)
				showList();
    	});
    	
    	
    	visibleItems.addListener((ListChangeListener.Change<? extends Entry> event) -> {
    		if (visibleItems.isEmpty())
    			return;
    		
    		monthLabel.setText(visibleItems.get(0).getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
		});
    	
    	createSearchToolTip();
    	
    	root.sceneProperty().addListener((obs, oldValue, newValue) -> {
    		if (newValue != null)
    			createViewAnchor();
		});
	}
	
	private void createSearchToolTip() {
    	filterBox = new FilterBar();
    	
    	filterPane.getChildren().setAll(filterBox);
    	filterPane.prefWidthProperty().bind(filterBox.prefWidthProperty());
    	
    	filterBox.setOnSelected(() -> setFilterBoxExpanded(true));
    	
    	filterBox.setOnUnselected(() -> {
    		if (!filterBox.getFilters().isEmpty())
    			return;
    		
    		setFilterBoxExpanded(false);
    	});
    	
    	filterBox.getFilters().addListener((ListChangeListener.Change<? extends Predicate<Entry>> event) -> {
			filteredItems.setPredicate(entry -> {
				BooleanProperty accepted = new SimpleBooleanProperty(true);
				
				filterBox.getFilters().forEach(predicate -> accepted.setValue(accepted.getValue() && predicate.test(entry)));
				
				return accepted.getValue();
			});
    		addMonths();
    		if (filterBox.getFilters().isEmpty())
    			listView.scrollTo(filteredItems.size() - 1);
    		listView.scrollTo(listView.getSelectionModel().getSelectedItem());
		});
    	
	}
	
	private void setFilterBoxExpanded(boolean expand) {
		double opacity = expand ? 0 : 1;
		double width = expand ? root.getWidth() - 40 : 230;
		
		if (!Config.get().getBoolean("enable_animations")) {
			filterBox.setPrefWidth(width);
			monthLabel.setOpacity(opacity);
			
			if (expand)
				filterBox.showPopup(50);
			else 
				filterBox.hidePopup();
			
		} else {
			Timeline timeline = new Timeline();
			timeline.getKeyFrames().add(new KeyFrame(new Duration(200), 
				new KeyValue(filterBox.prefWidthProperty(), width),
				new KeyValue(monthLabel.opacityProperty(), opacity)
			));

			timeline.setOnFinished(event -> {
				if (expand)
					filterBox.showPopup();
				else 
					filterBox.hidePopup();
			});
			
					
			timeline.play();
		}
	}

	public void setAvailableTags(ObservableList<Tag> tags) {
		filterBox.setAvailableTags(tags);
	}
	
	private void addMonths() {
		items.removeAll(months);
		months.clear();
		for (Entry t : filteredItems) {
			if (t instanceof MonthEntry)
				continue;
			MonthEntry month = new MonthEntry(t.getCreationDate());
			if (!months.contains(month))
				months.add(month);
		}
		items.addAll(months);
	}
	
	public void setItems(ObservableList<Entry> items) {
		this.items = items;
		SortedList<Entry> sorted = new SortedList<Entry>(items);
		// Compares opposite so that later entries are on top
		sorted.setComparator((entry1, entry2) -> entry1.compareTo(entry2));
		
		filteredItems = sorted.filtered(entry -> true);
		filteredItems.predicateProperty().addListener((obs, oldValue, newValue) ->  visibleItems.clear());
		
		addMonths();
		
		listView.setItems(filteredItems);
		
		sorted.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			event.next();
			if (event.wasRemoved()) {
				listView.getSelectionModel().clearSelection();
				showList();
			}
		});
		listView.scrollTo(sorted.size() - 1);
		listSize.bind(Bindings.size(sorted));
		
		monthLabel.setText(sorted.get(sorted.size() - 1).getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	}
	
	private DoubleProperty viewAnchor;
	
	private void createViewAnchor() {
		if (viewAnchor == null) {
			viewAnchor = new SimpleDoubleProperty(root.getScene().getWidth());

			viewAnchor.addListener((obs, oldValue, newValue) -> {
				AnchorPane.setLeftAnchor(listViewPane, -newValue.doubleValue());
				AnchorPane.setRightAnchor(listViewPane, +newValue.doubleValue());
				
				AnchorPane.setRightAnchor(singleViewPane, -root.getScene().getWidth() + newValue.doubleValue());
				AnchorPane.setLeftAnchor(singleViewPane, root.getScene().getWidth() - newValue.doubleValue());
			});
		} else 
			viewAnchor.setValue(root.getScene().getWidth());
		
		root.getScene().widthProperty().addListener((obs, oldValue, newValue) -> {
			double diff = newValue.doubleValue() - oldValue.doubleValue();
			
			if (AnchorPane.getLeftAnchor(singleViewPane) != 0)
				AnchorPane.setLeftAnchor(singleViewPane, AnchorPane.getLeftAnchor(singleViewPane) + diff);
			
			if (AnchorPane.getRightAnchor(listViewPane) != 0)
				AnchorPane.setRightAnchor(listViewPane, AnchorPane.getRightAnchor(listViewPane) + diff);
		}); 
	}
	
	private void transitionTo(Node view) {
		if (!Config.get().getBoolean("enable_animations")) {
				
			if (view == listViewPane)
				viewAnchor.setValue(0);
			else if (view == singleViewPane)
				viewAnchor.setValue(root.getScene().getWidth());
				
		} else {
			Timeline timeline = new Timeline();
			if (view == listViewPane)
				timeline.getKeyFrames().add(new KeyFrame(new Duration(200),	new KeyValue(viewAnchor, 0d)));
			else if (view == singleViewPane)
				timeline.getKeyFrames().add(new KeyFrame(new Duration(200), new KeyValue(viewAnchor, root.getScene().getWidth())));

			timeline.play();
		}
	}
	
	public void showList() {
		transitionTo(listViewPane);
		
		listView.requestFocus();
	}
	
	private void showSingle() {
		transitionTo(singleViewPane);
	}
	
	private class EntryCell extends ListCell<Entry> {
		
		private Node n;
		private EntryCellController controller;
		
		{
			setOnMouseClicked(event -> {
		        if (event.getClickCount() == 2)
		        	showSingle();
		    });
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EntryCell.fxml"));
			
			try {
				n = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
			controller = loader.<EntryCellController>getController();
		}
		
		@Override public void updateItem(Entry item, boolean empty) {
	        super.updateItem(item, empty);
	        
	        if (empty) {
	            setText(null);
	            setGraphic(null);
	        } else {
	        	
	        	boolean addedBeggining = true;
	        	
	        	if (visibleItems.isEmpty())
	        		visibleItems.add(item);
	        	else if (item.getCreationDate().isBefore(visibleItems.get(0).getCreationDate()))
	        		visibleItems.add(0, item);
	        	else if (item.getCreationDate().isAfter(visibleItems.get(visibleItems.size() - 1).getCreationDate())) {
	        		visibleItems.add(item);
	        		addedBeggining = false;
	        	}
	        	
	        	if (visibleSize.getValue() > listView.getHeight())
	        		visibleItems.remove(addedBeggining ? visibleItems.size() - 1 : 0);
	        	
	        	if (item instanceof MonthEntry) {
	        		setGraphic(null);
	        		setText(item.getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	        		setFont(Font.font(30));
	        		setAlignment(Pos.CENTER);
	        	} else {
		            setText(null);
	        		setGraphic(n);
					setPadding(Insets.EMPTY);

					controller.setContent((DayOneEntry) item);
					
					int index = listView.getItems().indexOf(item);
					if (index == 0 || listView.getItems().get(index - 1) instanceof MonthEntry) 
						controller.setDateEnabled(true); // First item, and items after monthEntries, must always have the date
					else {
						Entry previous = listView.getItems().get(index - 1);
						controller.setDateEnabled(previous.getCreationDate().getDayOfYear() != item.getCreationDate().getDayOfYear());
					}
				
					controller.widthProperty().bind(listView.widthProperty().subtract(40));
	        	}
	        }
	    }
		
	}

}
