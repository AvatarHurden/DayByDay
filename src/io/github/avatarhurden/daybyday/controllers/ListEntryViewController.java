package io.github.avatarhurden.daybyday.controllers;

import io.github.avatarhurden.daybyday.components.FilterBar;
import io.github.avatarhurden.daybyday.components.MultiPane;
import io.github.avatarhurden.daybyday.components.MultiPane.MultiPaneOrientation;
import io.github.avatarhurden.daybyday.managers.Config;
import io.github.avatarhurden.daybyday.models.Entry;
import io.github.avatarhurden.daybyday.models.JournalEntry;
import io.github.avatarhurden.daybyday.models.MonthEntry;
import io.github.avatarhurden.daybyday.models.Tag;
import io.github.avatarhurden.daybyday.models.VisibleListItems;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class ListEntryViewController {

	@FXML private AnchorPane root;
	
	private MultiPane multiPane;
	
	// Single View Pane
	@FXML private AnchorPane singleViewPane;
	@FXML private HBox buttonBar;
	@FXML private SVGPath previousButton, homeButton, nextButton;
		// Entry View
	@FXML private AnchorPane contentPane;
	private EntryViewController entryViewController;
	private AnchorPane entryView;
	
	// List View Pane
	@FXML private AnchorPane listViewPane;
	@FXML private ListView<Entry> listView;
	
	@FXML private StackPane monthPane;
	@FXML private ComboBox<MonthEntry> monthCombo;
	private ChangeListener<MonthEntry> monthListener;
	
	@FXML private SVGPath orderButton;
	
		// Filter Pane
	@FXML private HBox filterPane; 
	private FilterBar filterBox;
	
	// Wide View
	private BooleanProperty wideView;
	private SplitPane splitView;
	
	private VisibleListItems<Entry> visibleItems;
	private BooleanProperty ascendingOrder;
	private Comparator<Entry> descendingComparator, ascendingComparator;
	
	private ObservableMap<MonthEntry, Set<JournalEntry>> monthsMap;
	private Property<MonthEntry> earliestMonth;
	
	private ObservableList<Entry> source, items;
	private SortedList<Entry> sortedItems;
	private FilteredList<Entry> filteredItems;
	private ListChangeListener<Entry> changeListener;
	
	@FXML
	private void initialize() {
		monthListener = (obs, oldValue, newValue) -> {
			visibleItems.clear();
			if (filteredItems.contains(newValue))
				listView.scrollTo(newValue);
			else if (monthsMap.containsKey(newValue))
				listView.scrollTo(monthsMap.get(newValue).stream().sorted(sortedItems.getComparator()).collect(Collectors.toList()).get(0));
		};
		monthCombo.valueProperty().addListener(monthListener);
		monthCombo.setCellFactory(list -> new ListCell<MonthEntry>() {
			@Override
			public void updateItem(MonthEntry item, boolean empty) {
				super.updateItem(item, empty);
				setAlignment(Pos.CENTER);
				setFont(Font.font(20));
				if (!empty && item != null)
					setText(item.toString());
			}
		});
		monthCombo.setButtonCell(new ListCell<MonthEntry>() {
			@Override
			public void updateItem(MonthEntry item, boolean empty) {
				super.updateItem(item, empty);
				setAlignment(Pos.CENTER);
				setFont(Font.font(30));
				if (!empty && item != null)
					setText(item.toString());
			}
		});
		
		earliestMonth = new SimpleObjectProperty<>();
		monthsMap = FXCollections.observableHashMap();
		monthsMap.addListener((MapChangeListener.Change<? extends MonthEntry,? extends Set<JournalEntry>> event) -> {
			MonthEntry key = event.getKey();
			MonthEntry old = earliestMonth.getValue();
			monthCombo.getItems().setAll(monthsMap.keySet().stream().sorted(sortedItems.getComparator()).collect(Collectors.toList()));
			
			if (monthsMap.containsKey(key)) {
				if (old == null || sortedItems.getComparator().compare(key, old) < 0) {
					earliestMonth.setValue(key);
					if (old != null) 
						Platform.runLater(() -> items.add(old));
				} else 
					Platform.runLater(() -> items.add(key));
			} else 
				Platform.runLater(() -> items.remove(key));
		});
		
		listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue instanceof JournalEntry)
				entryViewController.setEntry((JournalEntry) newValue);
		});
		
		listView.setCellFactory(table -> new EntryCell());
		
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
    	
    	multiPane = new MultiPane(MultiPaneOrientation.HORIZONTAL);
    	root.getChildren().setAll(multiPane);
    	AnchorPane.setTopAnchor(multiPane, 0d);	
    	AnchorPane.setRightAnchor(multiPane, 0d);
    	AnchorPane.setBottomAnchor(multiPane, 0d);
    	AnchorPane.setLeftAnchor(multiPane, 0d);
    	multiPane.getChildren().addAll(listViewPane, singleViewPane);
    	
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
				showList(true);
    	});

    	visibleItems = new VisibleListItems<Entry>((e1, e2) -> e2.compareTo(e1));
    	visibleItems.maxHeightProperty().bind(listView.heightProperty());
    	visibleItems.getList().addListener((ListChangeListener.Change<? extends Entry> event) -> {
    		if (visibleItems.getList().isEmpty())
    			return;
    		
    		Entry first = visibleItems.get(0);
    		for (java.util.Map.Entry<MonthEntry, Set<JournalEntry>> entry : monthsMap.entrySet())
    			if (entry.getValue().contains(first) || entry.getKey() == first) {
    				monthCombo.valueProperty().removeListener(monthListener);
    				monthCombo.setValue(entry.getKey());
    				monthCombo.valueProperty().addListener(monthListener);
    				break;
    			}
		});
    	
    	createSearchToolTip();
    	
    	setupWideView();
    	homeButton.visibleProperty().bind(wideView.not());
    	homeButton.managedProperty().bind(homeButton.visibleProperty());
    	
    	items = FXCollections.<Entry>observableArrayList();
    	items.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			event.next();
			if (event.wasRemoved()) {
				listView.getSelectionModel().clearSelection();
				showList(true);
			}
		});
    	
    	sortedItems = new SortedList<>(items);
		// Compares opposite so that later entries are on top
    	descendingComparator = (e1, e2) -> {
    		if (e1 == null || e1.isEmpty())
    			return -1;
    		else if (e2 == null || e2.isEmpty())
    			return 1;
    		else if (e1 instanceof JournalEntry && e2 instanceof JournalEntry)
				return e2.compareTo(e1);
			else if (e1 instanceof MonthEntry && e2 instanceof MonthEntry)
				return e2.compareTo(e1);
			else if (e2 instanceof MonthEntry)
				return e2.getCreationDate().millisOfDay().withMaximumValue().
						dayOfMonth().withMaximumValue().compareTo(e1.getCreationDate());
			else
				return e2.getCreationDate().compareTo(e1.getCreationDate().millisOfDay().withMaximumValue().
						dayOfMonth().withMaximumValue());
		};
		ascendingComparator = (e1, e2) -> {
			if (e1 instanceof JournalEntry && e2 instanceof JournalEntry)
				return e1.compareTo(e2);
			else if (e1 instanceof MonthEntry && e2 instanceof MonthEntry)
				return e1.compareTo(e2);
			else if (e1 instanceof MonthEntry)
				return e1.getCreationDate().millisOfDay().withMinimumValue().
						dayOfMonth().withMinimumValue().compareTo(e2.getCreationDate());
			else
				return e1.getCreationDate().compareTo(e2.getCreationDate().millisOfDay().withMinimumValue().
						dayOfMonth().withMinimumValue());
		};
    	sortedItems.setComparator(descendingComparator);
    	visibleItems.comparatorProperty().bind(sortedItems.comparatorProperty());
    	
		filteredItems = sortedItems.filtered(entry -> true);
		filteredItems.predicateProperty().addListener((obs, oldValue, newValue) -> visibleItems.getList().clear());
		
		filteredItems.addListener((ListChangeListener.Change<? extends Entry> event) -> {
			while (event.next()) {
				for (Entry t : event.getRemoved()) 
					if (t instanceof MonthEntry)
						continue;
					else
						removeMonth(new MonthEntry(t.getCreationDate()), (JournalEntry) t);
				for (Entry t : event.getList()) 
					if (t instanceof MonthEntry)
						continue;
					else
						addMonth(new MonthEntry(t.getCreationDate()), (JournalEntry) t);
			}
		});
		
		changeListener = (ListChangeListener.Change<? extends Entry> event) -> {
			while (event.next()) {
				for (Entry t : event.getRemoved())
					Platform.runLater(() -> this.items.remove(t));
				for (Entry t : event.getAddedSubList()) 
					Platform.runLater(() -> this.items.add(t));
			}
		};
		
		listView.setItems(filteredItems);
		
		previousButton.strokeProperty().bind(Bindings.when(previousButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		nextButton.strokeProperty().bind(Bindings.when(nextButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		homeButton.strokeProperty().bind(Bindings.when(homeButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
		
		nextButton.fillProperty().bind(Bindings.when(nextButton.disableProperty()).then(Color.LIGHTGRAY).otherwise(Color.BLACK));
		previousButton.fillProperty().bind(Bindings.when(previousButton.disableProperty()).then(Color.LIGHTGRAY).otherwise(Color.BLACK));
		
		previousButton.disableProperty().bind(listView.getSelectionModel().selectedIndexProperty().isEqualTo(0));
		nextButton.disableProperty().bind(listView.getSelectionModel().selectedIndexProperty().isEqualTo(Bindings.size(sortedItems).subtract(1)));
		
		homeButton.setOnMouseClicked(event -> showList(true));
    	previousButton.setOnMouseClicked(event -> listView.getSelectionModel().selectPrevious());
    	nextButton.setOnMouseClicked(event -> listView.getSelectionModel().selectNext());
    	
    	orderButton.strokeProperty().bind(Bindings.when(orderButton.hoverProperty()).then(Color.BLUE).otherwise(Color.TRANSPARENT));
    	orderButton.setOnMouseClicked(event -> ascendingOrder.setValue(!ascendingOrder.get()));

    	ascendingOrder = new SimpleBooleanProperty(false);
    	ascendingOrder.addListener((obs, oldValue, newValue) -> setDisplayOrder(newValue));
	}
	
	private void setDisplayOrder(boolean ascending) {
		orderButton.setRotate(ascending ? 180 : 0);
		if (ascending) {
			sortedItems.setComparator(ascendingComparator);
			visibleItems.clear();
			listView.scrollTo(sortedItems.size() - 1);
		} else {
			sortedItems.setComparator(descendingComparator);
			visibleItems.clear();
			listView.scrollTo(0);
		}

		ObservableMap<MonthEntry, Set<JournalEntry>> newMap = FXCollections.observableHashMap();
		newMap.putAll(monthsMap);
		monthCombo.valueProperty().removeListener(monthListener);
		for (MonthEntry entry : newMap.keySet())
			monthsMap.remove(entry);
		earliestMonth.setValue(null);
		for (MonthEntry entry : newMap.keySet())
			monthsMap.put(entry, newMap.get(entry));
		monthCombo.valueProperty().addListener(monthListener);
	}

	private void setupWideView() {
		splitView = new SplitPane(listViewPane, singleViewPane);
    	AnchorPane.setTopAnchor(splitView, 0d);
    	AnchorPane.setRightAnchor(splitView, 0d);
    	AnchorPane.setBottomAnchor(splitView, 0d);
    	AnchorPane.setLeftAnchor(splitView, 0d);
    	
		wideView = new SimpleBooleanProperty();
		
		wideView.bind(root.widthProperty().greaterThan(1050));
		wideView.addListener((obs, oldValue, newValue) -> {
			if (newValue) {
				splitView.getItems().setAll(listViewPane, singleViewPane);
				root.getChildren().setAll(splitView);
				double div = splitView.getDividerPositions()[0];
				splitView.setDividerPositions(0.6);
				splitView.setDividerPositions(div);
			} else {
				multiPane.getChildren().setAll(listViewPane, singleViewPane);
				root.getChildren().setAll(multiPane);
			}
		});
	}
	
	private void createSearchToolTip() {
    	filterBox = new FilterBar();
		filterBox.setPrefWidth(120);
    	
    	filterPane.getChildren().setAll(filterBox);
    	filterPane.prefWidthProperty().bind(filterBox.prefWidthProperty());
    	
    	filterBox.setOnSelected(() -> setFilterBoxExpanded(true));
    	
    	filterBox.setOnUnselected(() -> {
    		if (!filterBox.getFilters().isEmpty())
    			return;
    		
    		setFilterBoxExpanded(false);
    	});
    	
    	filterBox.getFilters().addListener((ListChangeListener.Change<? extends Predicate<Entry>> event) -> {
    		Entry selected = listView.getSelectionModel().getSelectedItem();

			earliestMonth.setValue(null);
    		Predicate<Entry> pred = filterBox.getCombinedFilter();
			filteredItems.setPredicate(entry -> pred.test(entry) || entry.isEmpty());
			
    		listView.getSelectionModel().select(selected);
    		if (selected != null)
    			listView.scrollTo(selected);
    		else
    			listView.scrollTo(visibleItems.get(0));
		});
	}
	
	private void setFilterBoxExpanded(boolean expand) {
		double opacity = expand ? 0 : 1;
		double width = expand ? listViewPane.getWidth() - 10 : 120;

		if (expand)
	    	monthPane.setManaged(false);
		
		if (!Config.get().getBoolean("enable_animations")) {
	    	monthPane.setManaged(!expand);
			filterBox.setPrefWidth(width);
			monthPane.setOpacity(opacity);
			
			if (expand)
				filterBox.showPopup(50);
			else 
				filterBox.hidePopup();
			
		} else {
			Timeline timeline = new Timeline();
			timeline.getKeyFrames().add(new KeyFrame(new Duration(200), 
				new KeyValue(filterBox.prefWidthProperty(), width),
				new KeyValue(monthPane.opacityProperty(), opacity)
			));

			timeline.setOnFinished(event -> {
				if (expand)
					filterBox.showPopup();
				else 
					filterBox.hidePopup();
		    	monthPane.setManaged(!expand);
			});
			
					
			timeline.play();
		}
	}

	public void setAvailableTags(ObservableList<Tag> tags) {
		filterBox.setAvailableTags(tags);
	}
	
	private void addMonth(MonthEntry month, JournalEntry entry) {
		if (monthsMap.containsKey(month))
			monthsMap.get(month).add(entry);
		else
			monthsMap.put(month, new HashSet<JournalEntry>(Arrays.asList(entry)));
	}
	
	private void removeMonth(MonthEntry month, JournalEntry entry) {
		if (monthsMap.containsKey(month)) {
			monthsMap.get(month).remove(entry);
			if (monthsMap.get(month).isEmpty())
				monthsMap.remove(month);
		}
	}
	
	public void setItems(ObservableList<Entry> items) {
		if (source != null)
			source.removeListener(changeListener);
		items.addListener(changeListener);

		for (Entry t : this.items)
			removeMonth(new MonthEntry(t.getCreationDate()), (JournalEntry) t);
		
		this.items.setAll(items);
//		listView.scrollTo(listView.getItems().size() - 1);
		source = items;
	}
	
	private void transitionTo(Node view) {
		multiPane.show(view, Config.get().getBoolean("enable_animations"));
	}
	
	public void showList(boolean transition) {
		if (wideView.get()) return;
		if (!transition)
			multiPane.show(listViewPane, false);
		else
			transitionTo(listViewPane);
		
		Platform.runLater(() -> listView.requestFocus());
	}
	
	private void showSingle() {
		if (wideView.get()) return;
		transitionTo(singleViewPane);
	}
	
	public void saveState() {
		Config.get().setProperty("list_view_wide_divider", String.valueOf(splitView.getDividerPositions()[0]));
		Config.get().setProperty("list_view_ascending", String.valueOf(ascendingOrder.get()));
	}
	
	public void loadState() {
		splitView.setDividerPosition(0, Config.get().getDouble("list_view_wide_divider", 0.5));
		ascendingOrder.setValue(Config.get().getBoolean("list_view_ascending", false));
	}
	
	private class EntryCell extends ListCell<Entry> {
		
		private Node n;
		private EntryCellController controller;
		private boolean isEmpty;
		{
			setOnMouseClicked(event -> {
		        if (event.getClickCount() == 2 || isEmpty)
		        	showSingle();
		        if (isEmpty)
    		        entryViewController.setEdit(true);
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
    		setPrefHeight(90);
	        if (empty) {
	            setText(null);
	            setGraphic(null);
	        } else {
		        isEmpty = item.isEmpty();
	        	
        		visibleItems.add(item, 90);
	        	
	        	if (item instanceof MonthEntry) {
	        		setGraphic(null);
	        		setText(item.getCreationDate().toString("MMMMMMMMMMMMMMM YYYY"));
	        		setFont(Font.font(30));
	        		setTextFill(Color.BLACK);
	        		setAlignment(Pos.CENTER);
	        		setBorder(null);
	        		setCursor(Cursor.DEFAULT);
	        	} else if (((JournalEntry) item).isEmpty()) {
	        		setText("Create new Entry");
	        		setFont(Font.font(30));
	        		setTextFill(Color.LIGHTGREY);
	        		setAlignment(Pos.CENTER);
	        		setGraphic(null);
	        		setCursor(Cursor.HAND);
	        		
	        		Border border = new Border(
	        				new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.DASHED, new CornerRadii(6), BorderWidths.DEFAULT));
	        		
	        		setBorder(border);
	        		
	        	} else {
	        		setBorder(null);
		            setText(null);
	        		setGraphic(n);
					setPadding(Insets.EMPTY);
	        		setCursor(Cursor.DEFAULT);

					controller.setContent((JournalEntry) item);
					
					int index = listView.getItems().indexOf(item);
					int min = ascendingOrder.get() ? 1 : listView.getItems().size() - 1;
					if (index <= min)
						controller.setDateEnabled(true);
					else {
						Entry previous = listView.getItems().get(index - 1);
						if (previous instanceof MonthEntry || previous.isEmpty()) 
							// First item, and items after monthEntries, must always have the date
							controller.setDateEnabled(true);
						else 
							controller.setDateEnabled(previous.getCreationDate().getDayOfYear() != item.getCreationDate().getDayOfYear());
					}
					controller.widthProperty().bind(listView.widthProperty().subtract(40));
					
	        	}
	        }
	    }
		
	}

}
