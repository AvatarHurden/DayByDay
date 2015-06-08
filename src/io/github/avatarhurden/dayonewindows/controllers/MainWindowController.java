package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.managers.EntryManager;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

public class MainWindowController {

	@FXML private AnchorPane contentPane;
	
	@FXML private ToggleButton newButton, listButton;
	private ToggleGroup group;
	
	// Creation view
	private AnchorPane entryView;
	private EntryViewController entryViewController;
	
	// List View
	private BorderPane entryListView;
	private ListEntryViewController entryListViewController;
	
	// Config View
	private VBox configView;
	
	
	private EntryManager manager;

	public void setDiaryManager(EntryManager manager) {
		this.manager = manager;
		
		entryListViewController.setItems(manager.getEntries());
		
		String startScreen = Config.get().getProperty("start_screen", "Open New Entry View");
    	if (startScreen.equals("Open Last View"))
    		startScreen = "Open " + Config.get().getProperty("last_screen", "New Entry") + " View";
    	switch (startScreen) {
		case "Open New Entry View":
			newButton.fire();
			break;
		case "Open Entry List View":
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
    		entryView = loader.<AnchorPane>load();
	    	AnchorPane.setTopAnchor(entryView, 0d);
	    	AnchorPane.setRightAnchor(entryView, 0d);
	    	AnchorPane.setBottomAnchor(entryView, 0d);
	    	AnchorPane.setLeftAnchor(entryView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	entryViewController = loader.<EntryViewController>getController();
    	
    	loader = new FXMLLoader(getClass().getResource("/fxml/ListEntryView.fxml"));
    	try {
    		entryListView = loader.<BorderPane>load();
	    	AnchorPane.setTopAnchor(entryListView, 0d);	
	    	AnchorPane.setRightAnchor(entryListView, 0d);
	    	AnchorPane.setBottomAnchor(entryListView, 0d);
	    	AnchorPane.setLeftAnchor(entryListView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	entryListViewController = loader.<ListEntryViewController>getController();

    	loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"));
    	try {
			configView = loader.load();
	    	AnchorPane.setTopAnchor(configView, 0d);	
	    	AnchorPane.setRightAnchor(configView, 0d);
	    	AnchorPane.setBottomAnchor(configView, 0d);
	    	AnchorPane.setLeftAnchor(configView, 0d);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void showConfigView() {
		Alert alert = new Alert(AlertType.NONE);
		alert.initModality(Modality.APPLICATION_MODAL);
		
		alert.getButtonTypes().setAll(ButtonType.CLOSE);
		alert.getDialogPane().setContent(configView);
		
		alert.setX(contentPane.getScene().getWindow().getX() + configView.getPrefWidth() / 2 );
		alert.setY(contentPane.getScene().getWindow().getY() + 32);
		alert.showAndWait();
	}
	
	@FXML
	private void showNewEntry() {
		if (entryViewController.getEntry() != null && entryViewController.getEntry().isEmpty())
			manager.deleteEntry(entryViewController.getEntry());
		entryViewController.setEntry(manager.addEntry());
		contentPane.getChildren().setAll(entryView);
		Config.get().setProperty("last_screen", "New Entry");
	}
	
	@FXML
	private void showEntryList() {
		if (entryViewController.getEntry() != null && entryViewController.getEntry().isEmpty())
			manager.deleteEntry(entryViewController.getEntry());
		contentPane.getChildren().setAll(entryListView);
		entryListViewController.showList();
		Config.get().setProperty("last_screen", "Entry List");
	}
}
