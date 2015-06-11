package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.models.DayOneEntry;
import io.github.avatarhurden.dayonewindows.models.Entry;
import io.github.avatarhurden.dayonewindows.models.MonthEntry;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import org.joda.time.DateTime;

import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer.PointerType;
import com.mdimension.jchronic.utils.Span;

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
	
	private FilteredList<Entry> allItems;
	
	@FXML private Label monthLabel;
	@FXML private TextField searchField;
	@FXML private AnchorPane listViewPane;
	
	private SimpleIntegerProperty listSize;
	
	private ObservableList<Entry> visibleItems;
	private DoubleProperty visibleSize;
	
	@FXML
	private void initialize() {
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
			monthLabel.setText(visibleItems.get(0).getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
		});
    	
    	searchField.textProperty().addListener((obs, oldValue, newValue) -> {
    		try {	
    			Options p = new Options(false);
    			p.setContext(PointerType.PAST);
    			p.setNow(new DateTime().withMillisOfDay(0).toCalendar(Locale.getDefault()));
    			p.setAmbiguousTimeRange(24);
    			
    			final Span t = Chronic.parse(newValue, p);
    			
    			Span endT;
    			if (new DateTime(t.getBeginCalendar()).withMillisOfDay(0).isEqual(new DateTime(t.getEndCalendar()).withMillisOfDay(0)))
    				endT = new Span(t.getBeginCalendar(), Calendar.DAY_OF_MONTH, 1);
    			else
    				endT = t;
    			
    			allItems.setPredicate(s -> s.getCreationDate().isAfter(new DateTime(t.getBeginCalendar())) 
					&& s.getCreationDate().isBefore(new DateTime(endT.getEndCalendar())));
    			
			} catch (Exception e) {
				e.printStackTrace();
				allItems.setPredicate(entry -> true);
			}
		});
	}
	
	public void setItems(ObservableList<Entry> items) {
		SortedList<Entry> sorted = new SortedList<Entry>(items);
		// Compares opposite so that later entries are on top
		sorted.setComparator((entry1, entry2) -> entry1.compareTo(entry2));
		
		DateTime first = sorted.get(0).getCreationDate();
		DateTime last = sorted.get(items.size() - 1).getCreationDate();
		
		for (int i = first.getYear()*12 + first.getMonthOfYear(); i <= last.getYear()*12 + last.getMonthOfYear(); i++)
			items.add(new MonthEntry(i/12, i%12));

		allItems = sorted.filtered(entry -> true);
		allItems.predicateProperty().addListener((obs, oldValue, newValue) -> {
			visibleItems.clear();
		});
    	
		listView.setItems(allItems);
		
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
		root.getChildren().setAll(listViewPane);
	}
	
	private void showSingle() {
		root.getChildren().setAll(singleViewPane);
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
