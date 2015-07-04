package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.components.CloseButton;
import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.models.Journal;

import java.io.File;
import java.io.FileNotFoundException;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class ConfigViewController {

	@FXML private VBox root;
	
	@FXML private HBox menuBar;
	
	@FXML private ComboBox<String> startScreenSelector;
	
	@FXML private CheckBox animationCheckBox;
	@FXML private CheckBox boldCheckBox;
	
	@FXML private Label folderPathLabel;
	
	@FXML private RadioButton allFiltersButton, anyFilterButton;
	private ToggleGroup filterGroup;
	
	private Runnable onClose;
	
	private Journal manager;
	private MainWindowController parent;
	
	@FXML
	private void initialize() throws FileNotFoundException {
		CloseButton closeButton = new CloseButton();
		closeButton.setOnMouseReleased(event -> close());
		menuBar.getChildren().add(closeButton);
		
		populateScreenComboBox();
		
		initializeCheckBoxes();
	}
	
	private void initializeCheckBoxes() throws FileNotFoundException {
		String animation = Config.get().getPropertyAndSave("enable_animations", "true");
		animationCheckBox.setSelected(Boolean.valueOf(animation));
		
		String folder = Config.get().getProperty("data_folder");
		folderPathLabel.setText(folder);
		
		String boldTitles = Config.get().getPropertyAndSave("bold_titles", "true");
		boldCheckBox.setSelected(Boolean.valueOf(boldTitles));
		
		filterGroup = new ToggleGroup();
		allFiltersButton.setToggleGroup(filterGroup);
		allFiltersButton.setUserData("all");
		anyFilterButton.setToggleGroup(filterGroup);
		anyFilterButton.setUserData("any");
		
		String filter = Config.get().getPropertyAndSave("list_filter_system", "all");
		if (filter.equals("all"))
			filterGroup.selectToggle(allFiltersButton);
		else if (filter.equals("any"))
			filterGroup.selectToggle(anyFilterButton);
		
		filterGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
			Config.get().setProperty("list_filter_system", (String) newValue.getUserData());
		});
	}

	private void populateScreenComboBox() {
		ObservableList<String> items = startScreenSelector.getItems();
		
		items.add("Open Last View");
		items.add("Open New Entry View");
		items.add("Open Entry List View");
		
		startScreenSelector.setValue(Config.get().getPropertyAndSave("start_screen", "Open Last View"));
	}
	
	@FXML
	private void enableAnimations() {
		boolean selected = animationCheckBox.isSelected();
		Config.get().setProperty("enable_animations", String.valueOf(selected));
	}
	
	@FXML
	private void enableBold() {
		boolean selected = boldCheckBox.isSelected();
		Config.get().setProperty("bold_titles", String.valueOf(selected));
	}
	
	@FXML
	private void changeStartScreen() {
		Config.get().setProperty("start_screen", startScreenSelector.getValue());
	}
	
	@FXML
	private void changeFolderLocation() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setInitialDirectory(new File(Config.get().getProperty("data_folder")).getParentFile());
			
		File chosen = chooser.showDialog(root.getScene().getWindow());
		
		if (chosen != null && chosen.getAbsolutePath() != Config.get().getProperty("data_folder")) {
			Config.get().setProperty("data_folder", chosen.getAbsolutePath());
			Journal oldJournal = manager;
			
			manager = new Journal(chosen.getAbsolutePath());
			manager.loadAndWatch();
			manager.setKeepEmptyEntry(true);
			
			parent.setJournal(manager);
			oldJournal.close();
			folderPathLabel.setText(chosen.getAbsolutePath());
		}
	}
	
	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}
	
	@FXML
	public void close() {
		onClose.run();
	}

	public void setJournal(Journal manager) {
		this.manager = manager;
	}
	
	public void setParent(MainWindowController parent) {
		this.parent = parent;
	}
	
}
