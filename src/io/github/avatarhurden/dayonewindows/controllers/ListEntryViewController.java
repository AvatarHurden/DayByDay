package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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
	
	ListView<Entry> listView;
	
	SimpleIntegerProperty listSize;
	
	@FXML
	private void initialize() {
		listSize = new SimpleIntegerProperty();
		
		listView = new ListView<Entry>();
		
		listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue instanceof DayOneEntry)
				entryViewController.setEntry((DayOneEntry) newValue);
		});
		
		listView.setCellFactory(table -> new EntryCell());
		AnchorPane.setTopAnchor(listView, 0d);
    	AnchorPane.setRightAnchor(listView, 0d);
    	AnchorPane.setBottomAnchor(listView, 0d);
    	AnchorPane.setLeftAnchor(listView, 0d);
		
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
	}
	
	public void showList() {
		contentPane.getChildren().setAll(listView);
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
		        });
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EntryCell.fxml"));
			try {
				n = loader.load();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
