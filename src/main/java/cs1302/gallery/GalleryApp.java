package cs1302.gallery;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.URL; // dont need whoops
import java.net.URLEncoder;

import java.util.*;

import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.InputStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.*;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.text.*;
import javafx.geometry.*;

import javafx.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cs1302.gallery.ItunesResponse;
import cs1302.gallery.ItunesResult;

/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    // instance variables
    private Stage stage;
    private Scene scene;
    private VBox root;

    ProgressBar bar;
    Button playButton;
    Text search;
    TextField searchBar;
    Button getImage;
    ComboBox<String> dropDown;
    HBox searchRow;
    HBox instruction;
    Text instructions;
    TilePane tile;
    ImageView imageView;
    HBox lastRow;
    KeyFrame keyFrame;
    Timeline timeline = new Timeline();
    boolean play = true;
    boolean first = true;

    Image image;
    List<Image> images = new ArrayList<Image>();
    List<String> imageUrls = new ArrayList<String>();
    List<Image> tempImages = new ArrayList<Image>();
    List<Image> extImages = new ArrayList<Image>();
    Random rdm = new Random();


    String searchInput = "";

    /**
     * Constructs a {@code GalleryApp} object}.
     */
    public GalleryApp() {
        this.stage = null;
        this.scene = null;
        this.root = new VBox(10);
    } // GalleryApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        // feel free to modify this method
        System.out.println("init() called");
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root); //MAY NEED TO ADD SIZE
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        stage.setMaxWidth(1280);
        stage.setMaxHeight(720);
        setUp(stage); // the interface
        buttonSetUp(); // the buttons
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /**
     * Sets up "Play/Pause" button, Search bar with text field, music dropdown,
     * and "Get Images" button. Builds default images section. Sets up progress bar.
     *
     *@param stage current stage being used
     */
    public void setUp(Stage stage) {
        // parts to the interface
        playButton = new Button("Play"); // play/pause button
        playButton.setDisable(true); // play/pause disabled
        search = new Text("Search :"); // search text
        searchBar = new TextField("metal"); // search textfield initialized to "metal"
        dropDown = new ComboBox<>(); // dropdown result type
        dropDown.getItems().addAll( // options to dropdown
            "movie", "podcast", "music", "musicVideo",
            "audioBook", "shortFilm", "tvShow", "software",
            "ebook", "all"
        );
        dropDown.getSelectionModel().select(2); // selects music as default
        getImage = new Button("Get Images"); // get image button
        searchRow = new HBox(10); // creates new HBox within app
        searchRow.getChildren().addAll(playButton, search, searchBar, dropDown, getImage);
        searchRow.setHgrow(searchBar, Priority.ALWAYS);
        // next HBox for instructions
        instruction = new HBox(10); // creates new HBox under search
        instructions = new Text("Type in a term, select a media type, then click the button.");
        instruction.getChildren().add(instructions); // adds the text to instruction HBox
        // Tilepanes under instruction HBox
        tile = new TilePane();
        tile.setPrefColumns(5); // 20 tiles
        tile.setPrefRows(4);

        for (int i = 0; i < 20; i++) { // sets default images
            imageView = new ImageView("file:resources/default.png");
            imageView.setFitWidth(115);
            imageView.setFitHeight(115);
            tile.getChildren().add(imageView);
        } // for

        // Progress bar HBox
        lastRow = new HBox(10);
        bar = new ProgressBar(0);
        Text credits = new Text("Images provided by iTunes Search API.");
        lastRow.getChildren().addAll(bar, credits);
        bar.setMaxWidth(Double.MAX_VALUE); //Allow progress bar to fill space.
        lastRow.setHgrow(bar, Priority.ALWAYS);

        EventHandler<ActionEvent> playBHandler = event -> randomSwitch();
        keyFrame = new KeyFrame(Duration.seconds(2), playBHandler); // makes switches
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(keyFrame);

        //Visual Adjustments Total
        searchRow.setAlignment(Pos.CENTER_LEFT);
        Insets insets = new Insets(5, 5, 0, 5);
        searchRow.setPadding(insets);
        instruction.setPadding(insets);
        Insets insets2 = new Insets(0, 5, 5, 5);
        lastRow.setPadding(insets2);

        root.getChildren().addAll(searchRow, instruction, tile, lastRow);
    } // buildSearchBar

    /**
     * Stores Button Event Handlers.
     */
    public void buttonSetUp() {

        // Get Image Button Function
        Runnable r = () -> loadImage(); // runnable object for method loadImage
        EventHandler<ActionEvent> getImageHandler = event -> runNow(r);
        getImage.setOnAction(getImageHandler);

        // Play/Pause Button Function
        Runnable s = () -> playImages(); // runnable object for method playImages
        //connects playImages to playButton
        EventHandler<ActionEvent> rSwitchHandler = event -> runNow(s);
        playButton.setOnAction(rSwitchHandler); // Plays Images when Play is Pressed

    } // buttonSetUp

    /**
     * Helper method to read URI and display found images from URL connecte
     * to iTunes API. Used by Get Image Button.
     */
    public void loadImage() {
        searchInput = searchBar.getText();
        getImage.setDisable(true); //disables Get Image button
        instructions.setText("Getting images..."); // changes instructions
        String uri = "https://itunes.apple.com/search?term=" + // form URI
            URLEncoder.encode(searchInput, StandardCharsets.UTF_8) + "&media=" +
            URLEncoder.encode(dropDown.getValue(), StandardCharsets.UTF_8) + "&limit=200";
        try {
            playButton.setDisable(true); // ensures Play/Pause is disabled
            timeline.stop();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) { // ensures request is ok
                throw new IOException(response.toString());
            } // if
            String jsonString = response.body(); // get request body
            ItunesResponse itunesResponse = GSON.fromJson(jsonString, ItunesResponse.class);
            int resultsCount = itunesResponse.resultCount;
            if (resultsCount < 21) {
                throw new IllegalArgumentException("21 or more distinct results not found.");
            } else {
                if (tempImages.isEmpty() == false) {
                    tempImages.clear();
                    resetDisplay(); // resets bar, Play/Pause button
                }
                for (int i = 0; i < itunesResponse.results.length; i++) {
                    ItunesResult result = itunesResponse.results[i];
                    image = new Image(result.artworkUrl100, 115, 115, false, false);
                    if (image.isError()) {
                        throw new IllegalArgumentException(image.getException());
                    }
                    setProgress(1.0 * (i + 1) / resultsCount);
                    if (!imageUrls.contains(result.artworkUrl100)) {
                        imageUrls.add(result.artworkUrl100); //adding url to a stringlist
                        tempImages.add(image);
                    } // if
                }
                if (tempImages.size() < 21) {
                    throw new IllegalArgumentException(tempImages.size() +
                    " distinct results found, but > 20 needed.");
                } else {
                    first = false;
                    if (images.isEmpty() == false || extImages.isEmpty() == false) {
                        images.clear();
                        extImages.clear();
                    } // if
                    images.addAll(tempImages);
                    adjustImageArrays();
                    this.changeDisplay();
                }
            } // if
            imageUrls.clear();
            instructions.setText(uri);
            getImage.setDisable(false);
        } catch (IOException | InterruptedException | IllegalArgumentException e) { //
            changeProgress();
            System.err.println(e);
            printErrorMessage(uri, e);
        } // try
    } // loadImage

    /**
     * Creates pop up alert for errors.
     *
     * @param uri the url of the search
     * @param exception exception being alerted
     */
    private void printErrorMessage(String uri, Throwable exception) {
        // if the imageview is not showing default images
        timeline.stop();
        if (first == true) { //imageView.equals("file:resources/default.png")) {
            Platform.runLater(() -> {  // enable play
                playButton.setText("Play");
                playButton.setDisable(true);
            });
        } else {
            Platform.runLater(() -> {  // enable play
                playButton.setText("Play");
                playButton.setDisable(false);
            });
        } // if
        getImage.setDisable(false);
        instructions.setText("Last attempt to get images failed...");
        Platform.runLater(() -> {
            // pop up error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setWidth(500);
            alert.setContentText("URI: " + uri + "\n\nException: " + exception.toString());
            alert.showAndWait();
        });

    } // returnErrorMessage

    /**
     * Updates the progress bar.
     *
     * @param progress the progress being updated to
     */
    private void setProgress(final double progress) {
        Platform.runLater(() -> bar.setProgress(progress));
    } // setProgress

    /**
     * When exception is caught, progress is reset to what it was before the
     * search that caused the exception.
     */
    private void changeProgress() {
        if (first == false) {
            setProgress(1);
        } else {
            setProgress(0);
        }
    } // changeProgress

    /**
     * Adds images to extImages array.
     */
    private void adjustImageArrays() {
        extImages = new ArrayList<Image>(images.subList(20, images.size()));
        images.removeAll(extImages);
        playButton.setDisable(false);
    } // addToArray

    /**
     * Plays/Pauses images in response to Play/Pause Button.
     */
    private void playImages() {
        if (playButton.getText().equals("Play")) {
            timeline.play();
            Platform.runLater(() -> playButton.setText("Pause"));
        } else {
            Platform.runLater(() -> playButton.setText("Play"));
            timeline.stop();
        }
    } // playImages

    /**
     * Resets the Play/Pause Button to Play and ProgressBar to empty.
     */
    private void resetDisplay() {
        Platform.runLater(() -> {
            playButton.setText("Play");
            playButton.setDisable(true);
            bar.setProgress(0);
        });
    } // resetDisplay

    /**
     * Switches out one of 20 images shown on display to another downloaded image not
     * shown on display.
     */
    private void randomSwitch() {
        int random = rdm.nextInt(20); // assigns a random integer between 0-19
        int exRan = rdm.nextInt(extImages.size() - 1); // between 0-extImages length
        Image onDisplay = images.get(random); // stores the original displayed image
        images.set(random, extImages.get(exRan)); // sets new random image to the random spot
        extImages.remove(exRan); // removes the exRan image from extImages
        extImages.add(onDisplay); // adds the originally displayed image to extImages
        this.changeDisplay();
    } // randomSwitch

    /**
     * Resets {@code tilepane} and refils with changed images.
     */
    public void changeDisplay() {
        Platform.runLater(() -> tile.getChildren().clear());
        for (int i = 0; i < 20; i++) {
            ImageView imageView2 = new ImageView(images.get(i));
            Platform.runLater(() -> tile.getChildren().add(imageView2));
        }
        playButton.setDisable(false);
    } // changeDisplay

    /**
     * Creates and runs new daemon thread that runs {@code target}.
     * Can be called from any thread.
     *
     * @param target {@code Runnable} object that is being ran
     */
    public static void runNow(Runnable target) {
        Thread t = new Thread(target);
        t.setDaemon(true);
        t.start();
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        // feel free to modify this method
        System.out.println("stop() called");
    } // stop

} // GalleryApp
