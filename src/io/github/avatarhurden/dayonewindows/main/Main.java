package io.github.avatarhurden.dayonewindows.main;

import io.github.avatarhurden.dayonewindows.controllers.MainWindowController;
import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.managers.EntryManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import org.controlsfx.control.NotificationPane;

public class Main extends Application {

	private EntryManager entryManager;
	private static Stage primaryStage;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Main.primaryStage = primaryStage;
		
		if (Config.get().getProperty("default_folder") == null)
			showConfigView();
		
		entryManager = new EntryManager();
		entryManager.loadAndWatch();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
		
		NotificationPane pane = new NotificationPane(loader.load());
		Scene scene = new Scene(pane);
		
		MainWindowController controller = loader.<MainWindowController>getController();
		controller.setDiaryManager(entryManager);
		
		primaryStage.setTitle("LifeOrganizer");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		setPosition(primaryStage);
	
		primaryStage.setOnCloseRequest(event -> {
			try {
				savePosition(primaryStage);
				entryManager.close();
//				controller.saveState();
				Config.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});	
		
//		((AnchorPane) loader.getRoot()).setDisable(true);
	}
	
	private void showConfigView() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConfigView.fxml"));
		
		AnchorPane n = null;
		try {
			n = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Alert alert = new Alert(AlertType.NONE);
		alert.initStyle(StageStyle.UNDECORATED);
		alert.initOwner(primaryStage);
		alert.initModality(Modality.NONE);
		alert.getDialogPane().setContent(n);
		
		primaryStage.xProperty().addListener((obs, oldValue, newValue) -> {
			alert.setX(alert.getX() + (newValue.doubleValue() - oldValue.doubleValue()));
		});
		primaryStage.yProperty().addListener((obs, oldValue, newValue) -> {
			alert.setY(alert.getY() + (newValue.doubleValue() - oldValue.doubleValue()));
		});

		alert.setY(primaryStage.getY() + 31);
		alert.setX(primaryStage.getX() + n.getPrefWidth() / 2);
		
		alert.show();
	}
	
	private void savePosition(Window window) {
		List<Double> pos = new ArrayList<Double>();
		pos.add(window.getX());
		pos.add(window.getY());
		pos.add(window.getHeight());
		pos.add(window.getWidth());
		Config.get().setListProperty("window_position", pos, d -> d.toString());
	}
	
	private void setPosition(Window window) {
		List<Double> pos = Config.get().getListProperty("window_position", s -> Double.valueOf(s), null);
		if (pos == null)
			return;
		
		window.setX(pos.get(0));
		window.setY(pos.get(1));
		window.setHeight(pos.get(2));
		window.setWidth(pos.get(3));
	}
	
	public static void exit() {
		primaryStage.close();
	}
	
	
}
