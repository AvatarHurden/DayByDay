package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.managers.EntryManager;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainWindowController {

	@FXML private AnchorPane contentPane, root;
	
	@FXML private ToggleButton newButton, listButton;
	private ToggleGroup group;
	
	private DoubleProperty viewAnchors, configAnchors;
	
	// Creation view
	private AnchorPane newEntryView;
	private EntryViewController entryViewController;
	
	// List View
	private AnchorPane entryListView;
	private ListEntryViewController entryListViewController;
	
	// Config View
	private VBox configView;
	private ConfigViewController configViewController;
	private ImageView blurView;
	private Effect frostEffect;
	private SnapshotParameters snapshotParameters;
	
	private EntryManager manager;

	public void setDiaryManager(EntryManager manager) {
		this.manager = manager;
		
		entryListViewController.setItems(manager.getEntries());
		entryListViewController.setAvailableTags(manager.getTags());
		
		String startScreen = Config.get().getProperty("start_screen", "Open New Entry View");
		
    	if (startScreen.equals("Open Last View"))
    		startScreen = "Open " + Config.get().getProperty("last_screen", "New Entry") + " View";
    	
    	switch (startScreen) {
		case "Open New Entry View":
			viewAnchors.setValue(0);
			newButton.fire();
			break;
		case "Open Entry List View":
			viewAnchors.setValue(contentPane.getHeight());
			listButton.fire();
			break;
		}
	}
	
	@FXML
	private void initialize() {
		group = new ToggleGroup();
		newButton.setToggleGroup(group);
		listButton.setToggleGroup(group);
		
		group.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == null)
				group.selectToggle(oldValue);
		});
		
		
    	
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EntryView.fxml"));
    	try {
    		newEntryView = loader.<AnchorPane>load();
	    	AnchorPane.setTopAnchor(newEntryView, 0d);
	    	AnchorPane.setRightAnchor(newEntryView, 0d);
	    	AnchorPane.setBottomAnchor(newEntryView, 0d);
	    	AnchorPane.setLeftAnchor(newEntryView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	contentPane.getChildren().add(newEntryView);
    	
    	entryViewController = loader.<EntryViewController>getController();
    	
    	loader = new FXMLLoader(getClass().getResource("/fxml/ListEntryView.fxml"));
    	try {
    		entryListView = loader.<AnchorPane>load();
	    	AnchorPane.setTopAnchor(entryListView, 0d);	
	    	AnchorPane.setRightAnchor(entryListView, 0d);
	    	AnchorPane.setBottomAnchor(entryListView, 0d);
	    	AnchorPane.setLeftAnchor(entryListView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	contentPane.getChildren().add(entryListView);
    	
    	entryListViewController = loader.<ListEntryViewController>getController();

    	loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"));
    	try {
			configView = loader.load();
	    	AnchorPane.setTopAnchor(configView, -configView.getPrefHeight());	
	    	AnchorPane.setRightAnchor(configView, (root.getPrefWidth() - configView.getPrefWidth()) / 2);
	    	AnchorPane.setBottomAnchor(configView, root.getHeight());
	    	AnchorPane.setLeftAnchor(configView, (root.getPrefWidth() - configView.getPrefWidth()) / 2);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	configViewController = loader.<ConfigViewController>getController();
    	
    	configViewController.setOnClose(() -> {
    		if (Config.get().getBoolean("enable_animations")) {
    			Timeline time2 = new Timeline();
    			time2.setOnFinished(event2 -> blurView.setVisible(false));
    			time2.getKeyFrames().add(new KeyFrame(new Duration(100), 
    					new KeyValue(blurView.opacityProperty(), 0),
    					new KeyValue(configAnchors, 0)));
    			time2.play();
    		} else {
    			configAnchors.setValue(0);
    			blurView.setVisible(false);
    		}
		});
    	
    	blurView = new ImageView();
    	AnchorPane.setTopAnchor(blurView, 0d);	
    	AnchorPane.setRightAnchor(blurView, 0d);
    	AnchorPane.setBottomAnchor(blurView, 0d);
    	AnchorPane.setLeftAnchor(blurView, 0d);
    	root.getChildren().add(blurView);
    	root.getChildren().add(configView);
    	blurView.setVisible(false);
    	
    	initializeViewProperty();
    	initializeConfigAnimations();
	}
	
	private void initializeViewProperty() {
		viewAnchors = new SimpleDoubleProperty(0);
		
		viewAnchors.addListener((obs, oldValue, newValue) -> {
			AnchorPane.setTopAnchor(newEntryView, -newValue.doubleValue());
			AnchorPane.setBottomAnchor(newEntryView, newValue.doubleValue());
			
			AnchorPane.setTopAnchor(entryListView, contentPane.getHeight() - newValue.doubleValue());
			AnchorPane.setBottomAnchor(entryListView, -contentPane.getHeight() + newValue.doubleValue());
		});
	
		contentPane.heightProperty().addListener((obs, oldValue, newValue) -> {
			double diff = newValue.doubleValue() - oldValue.doubleValue();
			
			if (viewAnchors.get() != 0)
				AnchorPane.setBottomAnchor(newEntryView, AnchorPane.getBottomAnchor(newEntryView) + diff);
			
			if (AnchorPane.getTopAnchor(entryListView) != 0)
				AnchorPane.setTopAnchor(entryListView, AnchorPane.getTopAnchor(entryListView) + diff);
		});
	}
	
	private void initializeConfigAnimations() {
		configAnchors = new SimpleDoubleProperty(-1);
		
		configAnchors.addListener((obs, oldValue, newValue) -> {
			AnchorPane.setTopAnchor(configView, - configView.getPrefHeight() + newValue.doubleValue());
			AnchorPane.setBottomAnchor(configView, root.getHeight() - newValue.doubleValue());
		});
		
		root.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> {
			double diffHeight = newValue.getHeight() - oldValue.getHeight();
			
			AnchorPane.setBottomAnchor(configView, AnchorPane.getBottomAnchor(configView) + diffHeight);
			
			AnchorPane.setLeftAnchor(configView, (newValue.getWidth() - configView.getPrefWidth()) / 2);
	    	AnchorPane.setRightAnchor(configView, (newValue.getWidth() - configView.getPrefWidth()) / 2);

    		snapshotParameters.setViewport(new Rectangle2D(0, 0, newValue.getWidth(), newValue.getHeight()));
	    	if (Config.get().getBoolean("enable_animations")) {
				blurView.setOpacity(0d);
			
				Image frostImage = root.snapshot(snapshotParameters, null);
				blurView.setImage(frostImage);

				blurView.setOpacity(1d);
	    	}
		});

		blurView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)
				configViewController.close();
		});
		
		frostEffect = new BoxBlur(7, 7, 3);
    	blurView.setEffect(frostEffect);
		
		snapshotParameters = new SnapshotParameters();
		snapshotParameters.setViewport(new Rectangle2D(0, 0, root.getLayoutBounds().getWidth(), root.getLayoutBounds().getHeight()));
	}
	
	private void transitionTo(Node view) {
		if (!Config.get().getBoolean("enable_animations")) {
			
			if (view == newEntryView)
				viewAnchors.setValue(0);
			else if (view == entryListView)
				viewAnchors.setValue(contentPane.getHeight());
			
		} else {
			Timeline timeline = new Timeline();
			
			if (view == newEntryView)
				timeline.getKeyFrames().add(new KeyFrame(new Duration(200),	new KeyValue(viewAnchors, 0)));
			else if (view == entryListView)
				timeline.getKeyFrames().add(new KeyFrame(new Duration(200), new KeyValue(viewAnchors, contentPane.getHeight())));
			
			timeline.play();
		}
	}

	@FXML
	private void showConfigView() {
		configView.setVisible(true);
		if (!Config.get().getBoolean("enable_animations")) 
			configAnchors.setValue(configView.getPrefHeight() + 25);
		else {
			blurView.setOpacity(0d);
			blurView.setVisible(true);
		
			Image frostImage = root.snapshot(snapshotParameters, null);
    		blurView.setImage(frostImage);
    		blurView.setEffect(frostEffect);
    	
    		Timeline time = new Timeline();
    		time.getKeyFrames().add(new KeyFrame(new Duration(100),	
    				new KeyValue(blurView.opacityProperty(), 1),
    				new KeyValue(configAnchors, configView.getPrefHeight() + 25)));
			time.play();
		}
	}
	
	@FXML
	private void showNewEntry() {
		entryListViewController.showList();
		transitionTo(newEntryView);
		
		if (entryViewController.getEntry() != null && entryViewController.getEntry().isEmpty())
			manager.deleteEntry(entryViewController.getEntry());
		entryViewController.setEntry(manager.addEntry());
		
		Config.get().setProperty("last_screen", "New Entry");
	}
	
	@FXML
	private void showEntryList() {
		entryListViewController.showList();
		transitionTo(entryListView);
		
		if (entryViewController.getEntry() != null && entryViewController.getEntry().isEmpty())
			manager.deleteEntry(entryViewController.getEntry());
		
		Config.get().setProperty("last_screen", "Entry List");
	}
}
