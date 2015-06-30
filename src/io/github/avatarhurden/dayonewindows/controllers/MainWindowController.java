package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.components.DropdownPane;
import io.github.avatarhurden.dayonewindows.components.MultiPane;
import io.github.avatarhurden.dayonewindows.components.MultiPane.MultiPaneOrientation;
import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.models.Journal;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class MainWindowController {

	@FXML private AnchorPane contentPane, root, parent;
	
	@FXML private ToggleButton newButton, listButton;
	private ToggleGroup group;
	
	private MultiPane multiPane;
	
	// Creation view
	private AnchorPane newEntryView;
	private EntryViewController entryViewController;
	
	// List View
	private AnchorPane entryListView;
	private ListEntryViewController entryListViewController;
	
	// Config View
	private VBox configView;
	private ConfigViewController configViewController;
	private DropdownPane wrapper;
	
	private Journal journal;

	public void setJournal(Journal manager) {
		this.journal = manager;
		
		configViewController.setJournal(manager);
		
		entryListViewController.setItems(manager.getEntries());
		entryListViewController.setAvailableTags(manager.getTags());
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
		
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"));
    	try {
			configView = loader.load();
		} catch (IOException e) {}
    	
    	configViewController = loader.<ConfigViewController>getController();
    	configViewController.setParent(this);
		
    	wrapper = new DropdownPane(parent, configView);
    	wrapper.setSpacing(25);
    	
    	root.getChildren().setAll(wrapper);
    	AnchorPane.setTopAnchor(wrapper, 0d);	
    	AnchorPane.setRightAnchor(wrapper, 0d);
    	AnchorPane.setBottomAnchor(wrapper, 0d);
    	AnchorPane.setLeftAnchor(wrapper, 0d);
    	
    	configViewController.setOnClose(() -> wrapper.hide(Config.get().getBoolean("enable_animations")));
		
		loader = new FXMLLoader(getClass().getResource("/fxml/EntryView.fxml"));
    	try {
    		newEntryView = loader.<AnchorPane>load();
		} catch (IOException e) {}
    	
    	entryViewController = loader.<EntryViewController>getController();
    	
    	loader = new FXMLLoader(getClass().getResource("/fxml/ListEntryView.fxml"));
    	try {
    		entryListView = loader.<AnchorPane>load();
		} catch (IOException e) {}
    	entryListViewController = loader.<ListEntryViewController>getController();

    	multiPane = new MultiPane(MultiPaneOrientation.VERTICAL);
    	contentPane.getChildren().add(multiPane);
    	AnchorPane.setTopAnchor(multiPane, 0d);	
    	AnchorPane.setRightAnchor(multiPane, 0d);
    	AnchorPane.setBottomAnchor(multiPane, 0d);
    	AnchorPane.setLeftAnchor(multiPane, 0d);
    	
    	multiPane.getChildren().addAll(newEntryView, entryListView);
	}
	
	private void transitionTo(Node view) {
		multiPane.show(view, Config.get().getBoolean("enable_animations"));
	}

	@FXML
	private void showConfigView() {
		wrapper.show(Config.get().getBoolean("enable_animations"));
	}
	
	@FXML
	private void showNewEntry() {
		entryListViewController.showList(false);
		transitionTo(newEntryView);
		
		entryViewController.setEntry(journal.getEmptyEntry());
	}
	
	@FXML
	private void showEntryList() {
		entryListViewController.showList(true);
		transitionTo(entryListView);
	}

	public Journal getJournal() {
		return journal;
	}

	public void saveState() {
		entryListViewController.saveState();

		Node selected = multiPane.getSelectedNode();
		if (selected == entryListView)
			Config.get().setProperty("last_screen", "Entry List");
		else if (selected == newEntryView)
			Config.get().setProperty("last_screen", "New Entry");
	}
	
	public void loadState() {
		entryListViewController.loadState();
		
		String startScreen = Config.get().getProperty("start_screen", "Open New Entry View");
		
    	if (startScreen.equals("Open Last View"))
    		startScreen = "Open " + Config.get().getProperty("last_screen", "New Entry") + " View";
    	
    	switch (startScreen) {
		case "Open New Entry View":
			multiPane.show(0, false);
			newButton.fire();
			break;
		case "Open Entry List View":
			multiPane.show(1, false);
			listButton.fire();
			break;
		}
    	
	}
}
