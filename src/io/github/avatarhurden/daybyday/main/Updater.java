package io.github.avatarhurden.daybyday.main;

import io.github.avatarhurden.daybyday.managers.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;

import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import org.json.JSONObject;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.littlebigberry.httpfiledownloader.FileDownloader;
import com.littlebigberry.httpfiledownloader.FileDownloaderDelegate;

public class Updater implements FileDownloaderDelegate {

	private NotificationPane pane;
	ProgressIndicator p;
	
    private double currentVersion, latestVersion;
    private String currentVersionName, latestVersionName;
    private String latestChanges;
    private String fileLocation;

    public Updater(double currentVersion, String versionName, String url, NotificationPane pane) {
        this.currentVersion = currentVersion;
        this.currentVersionName = versionName;
        this.pane = pane;
        
        try {
        	JSONObject changeLog = getChangeLog(new URL(url));
        
        this.latestVersion = changeLog.getJSONObject("current").getDouble("version");
        this.latestVersionName = changeLog.getJSONObject("current").getString("versionName");
        this.latestChanges = changeLog.getJSONObject("current").getString("changes");
        this.fileLocation = changeLog.getJSONObject("current").getString("url");
        } catch (IOException e) {}
        
    }

    private JSONObject getChangeLog(URL url) throws IOException {
    	StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        int cp;
        while ((cp = reader.read()) != -1) {
            sb.append((char) cp);
        }
        return new JSONObject(sb.toString());
    }
    
    public void start() {
        if (Config.get().<Boolean>getProperty("skip_updates", s -> Boolean.valueOf(s), false))
            return;
        
    	if (currentVersion >= latestVersion)
    		return;

		Platform.runLater(() -> requestUserUpdate());
    }
    
    private void requestUserUpdate() {
    	pane.getActions().add(new Action("Changes", event -> {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.initModality(Modality.NONE);
			alert.setTitle("Changes in new version");
			alert.setHeaderText(null);
			
			alert.getDialogPane().setContent(getChangeLogView(latestChanges));
			alert.show();
		}));
			
		pane.getActions().add(new Action("Yes", event -> {
			FileChooser chooser = new FileChooser();
			chooser.setInitialDirectory(new File(System.getProperty("user.home") + File.separator + "Downloads"));
			chooser.setInitialFileName(new File(fileLocation).getName());
			chooser.getExtensionFilters().add(new ExtensionFilter("Executable File", "*.exe"));
			File f = chooser.showSaveDialog(null);
			if (f != null) {
				p = new ProgressIndicator();
				HBox box = new HBox(new ProgressIndicator());
		    	box.setPadding(new Insets(5));
				pane.setGraphic(box);
				beginDownload(f);
			}
		}));
		pane.getActions().add(new Action("No", event -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.initModality(Modality.APPLICATION_MODAL);
			alert.setHeaderText(null);
			alert.setTitle("New Updates");
			alert.setContentText("Do you wish to receive new updates in the future?");
			ButtonType sim = new ButtonType("Yes");
			ButtonType nao = new ButtonType("No");
			alert.getButtonTypes().setAll(sim, nao);
			Optional<ButtonType> result = alert.showAndWait();
			
			if (result.get() == nao)
				Config.get().setProperty("skip_updates", String.valueOf(true));
			
			pane.hide();
		}));
		
		pane.show("Do you wish to update from version " + this.currentVersionName + " to version " + this.latestVersionName + "?");
    }
    
    private Node getChangeLogView(String changes) {
    	WebView webView = new WebView();
    	
        String html = new PegDownProcessor(Extensions.TABLES).markdownToHtml(changes);
    
        webView.getEngine().loadContent(html);
        webView.setBlendMode(BlendMode.DARKEN);
        
        HBox box = new HBox(webView);
        box.setPadding(new Insets(5));
    	
        return box;
    }


    /**
     * Inicia o download usando a url remota e baixando no local no jar sendo executado.
     */
    private void beginDownload(File location) {
        FileDownloader fileDownloader = new FileDownloader(this);
        fileDownloader.setUrl(fileLocation);
        fileDownloader.setLocalLocation(location.getAbsolutePath());
        fileDownloader.beginDownload();
    }

    @Override
    public void didStartDownload(FileDownloader fileDownloader) {
    	HBox box = new HBox(p);
    	box.setPadding(new Insets(5));
    	pane.setGraphic(box);
    	pane.setText("Downloading...");
    }

    @Override
    public void didProgressDownload(FileDownloader fileDownloader) {
    	Platform.runLater(() -> {
    		p.setProgress(Double.valueOf(fileDownloader.getPercentComplete().replace("%", "").replace(",", "."))/100.0);
    	});
    }

    @Override
    public void didFinishDownload(FileDownloader fileDownloader) {
		Platform.runLater(() -> {
			pane.setText("Restart the program for the changes to take place.");
			pane.getActions().setAll(new Action("Ok", event -> {
				try {
					new ProcessBuilder(fileDownloader.getLocalLocation()).start();
				} catch (Exception e) {}
			}));
		});
    }

    @Override
    public void didFailDownload(FileDownloader fileDownloader) {
    }
}
