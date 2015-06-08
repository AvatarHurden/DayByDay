package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import org.joda.time.DateTime;

public class ListEntryViewController {

	@FXML
	private SVGPath previousButton, homeButton, nextButton;
	@FXML
	private HBox buttonBar;
	@FXML
	private AnchorPane contentPane;
	@FXML
	private BorderPane borderPane;
	
	private EntryViewController entryViewController;
	private AnchorPane entryView;
	
	private ListView<Entry> listView;
	private BorderPane monthView;
	private Label monthLabel;
	
	private SimpleIntegerProperty listSize;
	
	private ObservableList<Entry> visibleItems;
	private DoubleProperty visibleSize;
	
	@FXML
	private void initialize() {
		listSize = new SimpleIntegerProperty();
		
		listView = new ListView<Entry>();
		
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
		visibleSize.addListener((obs, oldValue, newValue) -> {
			System.out.println(newValue);
		});
		
		listView.setCellFactory(table -> new EntryCell());
		AnchorPane.setTopAnchor(listView, 0d);
    	AnchorPane.setRightAnchor(listView, 0d);
    	AnchorPane.setBottomAnchor(listView, 0d);
    	AnchorPane.setLeftAnchor(listView, 0d);
    	
    	monthLabel = new Label();
    	monthLabel.setFont(Font.font(30));
    	monthView = new BorderPane(monthLabel);
    	monthView.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		AnchorPane.setTopAnchor(monthView, 0d);
    	AnchorPane.setRightAnchor(monthView, 0d);
    	AnchorPane.setLeftAnchor(monthView, 0d);
		
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
    	
    	showList();
	}
	
	public void setItems(ObservableList<Entry> items) {

		SortedList<Entry> sorted = new SortedList<Entry>(items);
		// Compares opposite so that later entries are on top
		sorted.setComparator((entry1, entry2) -> entry1.compareTo(entry2));
		
		DateTime first = sorted.get(0).getCreationDate();
		DateTime last = sorted.get(items.size() - 1).getCreationDate();
		
		for (int i = first.getYear()*12 + first.getMonthOfYear(); i <= last.getYear()*12 + last.getMonthOfYear(); i++)
			items.add(new MonthEntry(i/12, i%12));

		listView.setItems(sorted);
		
		sorted.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			event.next();
			if (event.wasRemoved()) 
				listView.getSelectionModel().clearSelection();
		});
		listView.scrollTo(sorted.size() - 1);
		listSize.bind(Bindings.size(sorted));
		
		monthLabel.setText(sorted.get(sorted.size() - 1).getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	}
	
	public void showList() {
		contentPane.getChildren().setAll(listView, monthView);
		buttonBar.setManaged(false);
		buttonBar.setVisible(false);
	}
	
	private void showSingle() {
		contentPane.getChildren().setAll(entryView);
		buttonBar.setManaged(true);
		buttonBar.setVisible(true);
	}
	
	private class EntryCell extends ListCell<Entry> {
		
		private Node n;
		private EntryCellController controller;
		{
			 setOnMouseClicked(event -> {
		        	if (event.getClickCount() == 2)
		        		showSingle();
		        	entryView.requestFocus();
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
	        	
	        	if (visibleSize.getValue() > listView.getHeight()) {
	        		if (item.getCreationDate().isBefore(visibleItems.get(0).getCreationDate())) {
	        			visibleItems.remove(visibleItems.size() - 1);
	        			visibleItems.add(0, item);
	        		} else if (item.getCreationDate().isAfter(visibleItems.get(visibleItems.size() - 1).getCreationDate())) {
	        			visibleItems.remove(0);
	        			visibleItems.add(item);
	        		}
	        	} else
	        		visibleItems.add(item);
	        	
	        	monthLabel.setText(visibleItems.get(0).getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	        	
	        	if (item instanceof MonthEntry) {
	        		setGraphic(null);
	        		setText(item.getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	        		setFont(Font.font(30));
	        		setAlignment(Pos.CENTER);
	        		setBackground(new Background(new BackgroundFill(Color.valueOf("#1D1D45"), CornerRadii.EMPTY, Insets.EMPTY)));
	        	} else {
		            setText(null);
	        		setGraphic(n);
					setPadding(Insets.EMPTY);

					controller.setContent((DayOneEntry) item);
					
					Entry previous;
					if (listView.getItems().indexOf(item) > 0)
						previous = listView.getItems().get(listView.getItems().indexOf(item) - 1);
					else
						previous = item;
					
					if (previous instanceof MonthEntry)
						if (listView.getItems().indexOf(item) > 1)
							previous = listView.getItems().get(listView.getItems().indexOf(item) - 2);
						else
							previous = item;
					
					controller.setDateEnabled(previous.getCreationDate().getDayOfYear() != item.getCreationDate().getDayOfYear());
				
					controller.widthProperty().bind(listView.widthProperty().subtract(40));
	        	}
	        }
	    }
		
	}
}
