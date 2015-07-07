package io.github.avatarhurden.dayonewindows.controllers;

import io.github.avatarhurden.dayonewindows.components.TagEditor;
import io.github.avatarhurden.dayonewindows.managers.Config;
import io.github.avatarhurden.dayonewindows.models.JournalEntry;

import java.awt.Desktop;
import java.io.File;
import java.util.Calendar;
import java.util.Locale;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jfxtras.scene.control.CalendarPicker;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.joda.time.DateTime;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;


public class EntryViewController {

	@FXML private AnchorPane root;
	@FXML private SplitPane splitPane;
	
	// Date
	@FXML private Label dayOfWeekLabel, dayOfMonthLabel, monthYearLabel, timeLabel;
	@FXML private HBox dateBox;
	private Property<DateTime> dateProperty;
	
	// Tag
	@FXML private SVGPath tagIcon;
	@FXML private Label tagsLabel;
	@FXML private StackPane tagPane;
	private TagEditor tagEditor;
	
	// Star
	@FXML private SVGPath favoriteIcon;
	
	// Photo
	@FXML private SVGPath photoIcon;
	@FXML private StackPane photoPane;
	
	// Delete
	@FXML private SVGPath  deleteLidIcon, deleteBodyIcon;
	@FXML private VBox deleteIcon;
	
	// ImageView
	@FXML private ScrollPane imageScroll;
	@FXML private StackPane imageStack;
	@FXML private ImageView imageView;
	private Property<Number> zoomProperty;
	
	// Entry Text
	@FXML private VBox contentPane;
	@FXML private TextArea textArea;
	@FXML private WebView webView;
	@FXML private Button editButton, saveButton;

	//DragAndDropImage
	private StackPane dragAndDropPane;
	private ImageView dragAndDropImageView;
	private SnapshotParameters snapshotParameters;
	
	private JournalEntry entry;
	
	@FXML
	private void initialize() {
		bindWebViewAndTextArea();
		
		dateProperty = new SimpleObjectProperty<DateTime>(new DateTime());
		bindDateLabels();

		tagEditor = new TagEditor();
		tagIcon.getStyleClass().add("tag-pane");
		
		favoriteIcon.getStyleClass().add("starred-pane");
		favoriteIcon.setOnMouseClicked(event -> entry.starredProperty().setValue(!entry.starredProperty().getValue()));
		
		setImageViewMenu();
		setImageViewProperties();
		imageScroll.setVisible(false);
		
		editButton.setOnAction(event -> editButton.setVisible(false));
		saveButton.setOnAction(event -> editButton.setVisible(true));

		saveButton.visibleProperty().bind(editButton.visibleProperty().not());
		
		saveButton.managedProperty().bind(saveButton.visibleProperty());
		editButton.managedProperty().bind(editButton.visibleProperty());
		
		deleteLidIcon.getStyleClass().add("delete-pane");
		deleteLidIcon.setId("lid");
		deleteBodyIcon.getStyleClass().add("delete-pane");
		
		deleteIcon.hoverProperty().addListener((obs, oldValue, newValue) -> {
			deleteLidIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), newValue);
			deleteBodyIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), newValue);
			VBox.setMargin((Node) deleteLidIcon, newValue ? new Insets(4, 0, 0, 13) : new Insets(4, 13, 0, 0));
		});
		
		deleteLidIcon.rotateProperty().bind(Bindings.when(deleteIcon.hoverProperty()).then(40.6).otherwise(0));
		
		photoIcon.getStyleClass().add("photo-pane");
		
		imageView.imageProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == null) {
				splitPane.getItems().remove(imageScroll);
				photoIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), false);
			} else {
				photoIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), true);
				if (!splitPane.getItems().contains(imageScroll))
					splitPane.getItems().add(0, imageScroll);
			}
		});
		// Since it starts with no image, must remove from the items
		splitPane.getItems().remove(imageScroll);
		
		createDragAndDropPanel();
		
		bindDragAndDrop();
	}
	
	public JournalEntry getEntry() {
		return entry;
	}
	
	public void setEntry(JournalEntry newEntry) {
		if (entry != null)
			dateProperty.unbindBidirectional(entry.creationDateProperty());
		dateProperty.bindBidirectional(newEntry.creationDateProperty());
		
		if (newEntry.isEmpty())
			dateProperty.setValue(new DateTime());
		
		if (entry != null)
			textArea.textProperty().unbindBidirectional(entry.entryTextProperty());
		textArea.textProperty().bindBidirectional(newEntry.entryTextProperty());
		editButton.setVisible(true);
		
		tagsLabel.textProperty().unbind();
		
		tagsLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			tagIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), newEntry.getTags().size() > 0);
			
				if (newEntry.getTags().size() > 0) 
					return newEntry.getTags().size()+"";
				else 
					return "";
		}, newEntry.getObservableTags()));
		
		tagEditor.setEntry(newEntry);
		tagEditor.setPossibleTags(newEntry.getManager().getTags());

		ChangeListener<Boolean> favoriteListener = (obs, oldValue, newValue) -> {
			favoriteIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), newValue);
		};
		if (entry != null)
			entry.starredProperty().removeListener(favoriteListener);
		newEntry.starredProperty().addListener(favoriteListener);
		favoriteIcon.pseudoClassStateChanged(PseudoClass.getPseudoClass("active"), newEntry.isStarred());

		if (entry != null)
			entry.decrementImageReferences();
		imageView.imageProperty().bind(newEntry.imageProperty());
		zoomProperty.setValue(1d);
		newEntry.incrementImageReferences();
		
		entry = newEntry;
	}

	private void createDragAndDropPanel() {
		snapshotParameters = new SnapshotParameters();
		
		dragAndDropImageView = new ImageView();
		dragAndDropImageView.setEffect(new BoxBlur(7, 7, 3));
		
		dragAndDropPane = new StackPane();
		AnchorPane.setTopAnchor(dragAndDropPane, 0d);
		AnchorPane.setRightAnchor(dragAndDropPane, 0d);
		AnchorPane.setBottomAnchor(dragAndDropPane, 0d);
		AnchorPane.setLeftAnchor(dragAndDropPane, 0d);
		
		dragAndDropPane.getChildren().add(dragAndDropImageView);
		
		Label label = new Label("Add Photo to Entry");
		label.setPadding(new Insets(30));
		label.setFont(Font.font(20));
		label.setBackground(new Background(new BackgroundFill(Color.WHITE.deriveColor(0, 1, 1, 0.8), new CornerRadii(6), Insets.EMPTY)));
		label.setBorder(new Border(
				new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.DASHED, new CornerRadii(6), BorderWidths.DEFAULT)));
		
		dragAndDropPane.getChildren().add(label);
		
		final WebView box = new WebView();
		box.setPrefWidth(0);
		box.setOpacity(0d);
		
		box.setOnDragEntered(event -> root.fireEvent(event));
		box.setOnDragDropped(event -> root.fireEvent(event));
		box.setOnDragExited(event -> root.fireEvent(event));
		
		dragAndDropPane.getChildren().add(box);
		
		dragAndDropPane.setVisible(false);
		root.getChildren().add(dragAndDropPane);
	}

	private void bindDragAndDrop() {
		HBox rejectBox = new HBox();
		rejectBox.setOpacity(0d);
		AnchorPane.setTopAnchor(rejectBox, 0d);
		AnchorPane.setRightAnchor(rejectBox, 0d);
		AnchorPane.setBottomAnchor(rejectBox, 0d);
		AnchorPane.setLeftAnchor(rejectBox, 0d);
		
		root.setOnDragEntered(event -> {
			Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
            	boolean accepted =  db.getFiles().get(0).getName().endsWith(".jpg");
            	
            	if (accepted) {
            		addImageDragFilter();
                    event.acceptTransferModes(TransferMode.COPY);
            	} else if (!root.getChildren().contains(rejectBox))
            		root.getChildren().add(rejectBox);
            }
		});

		root.setOnDragDropped(event -> {
			Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
            	boolean accepted =  db.getFiles().get(0).getName().endsWith(".jpg");
            	if (accepted) 
            		entry.setNewImage(db.getFiles().get(0));
            }
		});

		root.setOnDragExited(event -> {
			Timeline timeline = new Timeline();
			timeline.setOnFinished(e -> dragAndDropPane.setVisible(false));
			timeline.getKeyFrames().add(new KeyFrame(new Duration(100),	
					new KeyValue(dragAndDropPane.opacityProperty(), 0)));
			timeline.play();
			
			if (root.getChildren().contains(rejectBox))
        		root.getChildren().remove(rejectBox);
		});
	}
	
	private void addImageDragFilter() {
		if (dragAndDropPane.isVisible())
			return;

		dragAndDropPane.setVisible(true);
		dragAndDropPane.setOpacity(0d);
		snapshotParameters.setViewport(new Rectangle2D(0, 0, root.getWidth(), root.getHeight()));
		
		Image frostImage = root.snapshot(snapshotParameters, null);
		dragAndDropImageView.setImage(frostImage);
		
		Timeline timeline = new Timeline();
		timeline.getKeyFrames().add(new KeyFrame(new Duration(100),	
				new KeyValue(dragAndDropPane.opacityProperty(), 1)));
		timeline.play();
	}

	private void bindDateLabels() {
		dayOfWeekLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			dateProperty.getValue().toString("EEE"), dateProperty));
		
		dayOfMonthLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			dateProperty.getValue().toString("dd"), dateProperty));

		monthYearLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			dateProperty.getValue().toString("MMM\nYYYY"), dateProperty));

		timeLabel.textProperty().bind(Bindings.createStringBinding(() -> 
			dateProperty.getValue().toString("HH:mm"), dateProperty));
	}
	
	@FXML
	private void openDateEditor() {
		PopOver over = new PopOver();
		over.setDetachable(false);
		over.setAutoHide(true);
		over.setArrowLocation(ArrowLocation.TOP_CENTER);
		
		CalendarPicker picker = new CalendarPicker();
		picker.setShowTime(true);
		picker.setAllowNull(false);
		
		picker.setCalendar(dateProperty.getValue().toGregorianCalendar());
		
		// Create a string property to bind CalendarPicker's date to dateProperty
		SimpleStringProperty converter = new SimpleStringProperty();
		converter.bindBidirectional(dateProperty, new StringConverter<DateTime>() {
			public DateTime fromString(String string) {
				return new DateTime(string);
			}
			public String toString(DateTime object) {
				return object.toString();
			}
		});
		converter.bindBidirectional(picker.calendarProperty(), new StringConverter<Calendar>() {
			public Calendar fromString(String string) {
				return new DateTime(string).toCalendar(Locale.getDefault());
			}
			public String toString(Calendar object) {
				return new DateTime(object).toString();
			}
		});
		
		StackPane pane = new StackPane(picker);
		pane.setPadding(new Insets(5));
		over.setContentNode(pane);
		
		over.show(dateBox);
	}

	@FXML
	private void openTagEditor() {
		PopOver over = new PopOver();
		over.setDetachable(false);
		over.setAutoHide(true);
		over.setArrowLocation(ArrowLocation.TOP_CENTER);
		
		StackPane pane = new StackPane(tagEditor);
		pane.setPadding(new Insets(5, 0, 5, 0));
		
		over.setContentNode(pane);
		over.show(tagPane);
	}
	
	@FXML
	private void openDeleteDialog() {
		PopOver over = new PopOver();
		over.setDetachable(false);
		over.setAutoHide(true);
		over.setArrowLocation(ArrowLocation.TOP_CENTER);
		
		Button delete = new Button("Delete Entry");
		delete.setTextFill(Color.RED);
		delete.setOnAction(event2 -> { 
			entry.delete();
			over.hide();
		});
		
		VBox box = new VBox(5);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(5));
		box.getChildren().addAll(delete);
		
		over.setContentNode(box);
		over.show(deleteIcon);
	}

	@FXML
	private void openPhotoDialog() {
		PopOver over = new PopOver();
		over.setDetachable(false);
		over.setAutoHide(true);
		over.setArrowLocation(ArrowLocation.TOP_CENTER);
		
		Button select = new Button("Select Image");
		select.setOnAction(event2 -> {
			FileChooser chooser = new FileChooser();
			chooser.setInitialDirectory(new File(System.getProperty("user.home"), "Pictures"));
			chooser.getExtensionFilters().add(new ExtensionFilter("JPEG Images", "*.jpg"));
			File selected = chooser.showOpenDialog(null);
			if (selected != null )
				entry.setNewImage(selected);
		});
		
		Button remove = new Button("Remove Image");
		remove.setTextFill(Color.RED);
		remove.disableProperty().bind(Bindings.createBooleanBinding(() -> entry.getImage() == null, entry.imageProperty()));
		
		remove.setOnAction(event2 -> entry.removeImage());

		VBox box = new VBox(5);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(5));
		box.getChildren().addAll(select, remove);
		
		over.setContentNode(box);
		over.show(photoIcon);
	}
	
	private void bindWebViewAndTextArea() {
		textArea.visibleProperty().bindBidirectional(saveButton.visibleProperty());
		webView.visibleProperty().bindBidirectional(editButton.visibleProperty());
		
		textArea.managedProperty().bind(textArea.visibleProperty());
		webView.managedProperty().bind(webView.visibleProperty());
		
		textArea.prefHeightProperty().bind(contentPane.heightProperty());
		webView.prefHeightProperty().bind(contentPane.heightProperty());
		
		textArea.autosize();
		textArea.setWrapText(true);
		
		PegDownProcessor processor = new PegDownProcessor(Extensions.TABLES);
		textArea.textProperty().addListener((obs, oldValue, newValue) -> {
			String[] lines = newValue.split("\n");
			if (Config.get().getBoolean("bold_titles", true) 
					&& lines[0].length() > 0 && lines[0].length() <= 140 && Character.isAlphabetic(lines[0].charAt(0)))
				newValue = "### " + newValue;
			String html = processor.markdownToHtml(newValue);
	        webView.getEngine().loadContent(html);
		});
        webView.setBlendMode(BlendMode.DARKEN);
        
        saveButton.visibleProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue) {
				textArea.requestFocus();
				textArea.positionCaret(textArea.getLength());
			}
		});
		
        textArea.setOnKeyPressed(event -> {
        	if (event.isControlDown() && event.getCode() == KeyCode.ENTER)
        		editButton.setVisible(true);
        });
        
        webView.setOnKeyPressed(event -> {
        	if (event.isControlDown() && event.getCode() == KeyCode.ENTER)
        		editButton.setVisible(false);
        });
        
        webView.setOnMouseClicked(event -> {
        	if (event.getClickCount() == 2)
        		editButton.setVisible(false);
        });
	}
	
	private void setImageViewMenu() {
		MenuItem openImage = new MenuItem("Open Image");
		openImage.setOnAction(event -> {
			try {
				Desktop.getDesktop().open(entry.getImageFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		MenuItem openExplorer = new MenuItem("Show image location");
		openExplorer.setOnAction(event -> {
			try {
				Runtime.getRuntime().exec("explorer.exe /select,"+entry.getImageFile().getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		ContextMenu menu = new ContextMenu(openImage, openExplorer);
		imageScroll.setContextMenu(menu);
		
		imageScroll.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)
				zoomProperty.setValue(1);
		});
		
		imageScroll.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> {
			imageView.setFitHeight(zoomProperty.getValue().doubleValue() * (newValue.getHeight() - 2));
			imageView.setFitWidth(zoomProperty.getValue().doubleValue() * (newValue.getWidth() - 2));
		});
	}
	
	private void setImageViewProperties() {
		imageScroll.managedProperty().bind(imageScroll.visibleProperty());
		
		imageStack.prefWidthProperty().bind(imageScroll.widthProperty().subtract(20));
		imageStack.prefHeightProperty().bind(imageView.fitHeightProperty());
		
		imageView.imageProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue == null)
				imageScroll.setVisible(false);
			else 
				imageScroll.setVisible(true);
		});
		
		zoomProperty = new SimpleDoubleProperty(0);
		
		zoomProperty.addListener((observable, oldValue, newValue) -> {
			double hvalue = imageScroll.getHvalue();
			double vvalue = imageScroll.getVvalue();
			
			imageView.setFitHeight(newValue.doubleValue() * (imageScroll.getHeight() - 2));
			imageView.setFitWidth(newValue.doubleValue() * (imageScroll.getWidth() - 2));
			
			imageScroll.setHvalue(hvalue);
			imageScroll.setVvalue(vvalue);
		});
	
		imageView.setOnScroll(value -> {
			value.consume();
			if (value.getDeltaY() > 0)
				zoomProperty.setValue(Math.min(10, zoomProperty.getValue().doubleValue() *  1.08));
			else if (value.getDeltaY() < 0)
				zoomProperty.setValue(Math.max(1, zoomProperty.getValue().doubleValue() / 1.08));
			});
	}

}
