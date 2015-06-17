package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.managers.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.imageio.ImageIO;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class ConfigViewController {

	@FXML private VBox root;
	
	@FXML private HBox menuBar;
	
	@FXML private ComboBox<String> startScreenSelector;
	
	@FXML private CheckBox animationCheckBox;
	
	@FXML private Label folderPathLabel;
	@FXML private ImageView folderIcon;
	
	private Runnable onClose;
	
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
		File file = new File(folder);
		
		while (file != null && !file.getName().equals("Dropbox"))
			file = file.getParentFile();
		
		if (file != null) {
			System.out.println(file.getPath() + File.separator);
			folder = folder.replace(file.getPath() + File.separator, "");
			
			folderIcon.setImage(new Image(getClass().getResourceAsStream("/style/dropbox.png")));
		}
		folderPathLabel.setText(folder);
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
	private void changeStartScreen() {
		Config.get().setProperty("start_screen", startScreenSelector.getValue());
	}
	
	@FXML
	private void changeFolderLocation() {
		DirectoryChooser chooser = new DirectoryChooser();
		System.out.println(chooser.showDialog(root.getScene().getWindow()));
	}
	
	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}
	
	@FXML
	private void close() {
		onClose.run();
	}
	
}
