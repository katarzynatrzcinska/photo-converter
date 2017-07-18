/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package photoconverter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 *
 * @author Kasia
 */
public class FXMLDocumentController implements Initializable {
    @FXML TableView<ImageProcessingJob> filesTable;
    ObservableList<ImageProcessingJob> jobs = FXCollections.observableArrayList();
    
    File outputDirectory;
    
    @FXML
    Label timeLabel;
    
    @FXML TableColumn<ImageProcessingJob, String> imageNameColumn;
    @FXML TableColumn<ImageProcessingJob, Double> progressColumn;
    @FXML TableColumn<ImageProcessingJob, String> statusColumn;
    
    @FXML
    protected void choosePhotos(ActionEvent event) throws MalformedURLException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG images", "*.jpg"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        
        for (File file : selectedFiles) {
            ImageProcessingJob ips = new ImageProcessingJob(file);
            jobs.add(ips);
            filesTable.setItems(jobs);
        }
    }
    
    @FXML
    protected void chooseOutputDirection(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        outputDirectory = directoryChooser.showDialog(null);
    }

    private void process(ImageProcessingJob job) {
        job.setStatus(job.STATUS_PROCESSING);
        convertToGrayscale(job.getFile(), outputDirectory, job.getProgressProperty());
        job.setStatus(job.STATUS_DONE);
    }
    
    @FXML
    public void processFilesSequentially(ActionEvent event) {
        new Thread(this::sequentialProcessing).start();
    }
    
    private void sequentialProcessing(){
        long start = System.currentTimeMillis(); //zwraca aktualny czas [ms]
        filesTable.getItems().stream().forEach(this::process);
        long end = System.currentTimeMillis(); //czas po zakończeniu operacji [ms]
        long duration = end-start; //czas przetwarzania [ms]
        Platform.runLater(() -> {
            timeLabel.setText("Processing time: " + duration + " ms ");
        });  
    }
    
    @FXML
    public void processFilesParallellyWithCustomPool(ActionEvent event) {
        List<String> choices = new ArrayList<>();
        choices.add("1");
        choices.add("2");
        choices.add("3");
        choices.add("4");
        choices.add("5");
        choices.add("6");
        choices.add("7");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("2", choices);
        dialog.setTitle("Selecting number of threads");
        dialog.setHeaderText("Custom Pool");
        dialog.setContentText("Select number of threads: ");

        Optional<String> result = dialog.showAndWait();
        ForkJoinPool pool = new ForkJoinPool(Integer.parseInt(result.get())); //pożądana liczba wątków
        pool.submit(this::parallelProcessing);
    }
    
    @FXML
    public void processFilesParallellyWithCommonPool(ActionEvent event) {
        ForkJoinPool pool = new ForkJoinPool(ForkJoinPool.commonPool().getParallelism()); 
        pool.submit(this::parallelProcessing);
    }
 
    private void parallelProcessing(){
        long start = System.currentTimeMillis(); //zwraca aktualny czas [ms]
        filesTable.getItems().parallelStream().forEach(this::process);
        long end = System.currentTimeMillis(); //czas po zakończeniu operacji [ms]
        long duration = end-start; //czas przetwarzania [ms]
        Platform.runLater(() -> {
            timeLabel.setText("Processing time: " + duration + " ms ");
        });  
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        imageNameColumn.setCellValueFactory( //nazwa pliku
        p -> new SimpleStringProperty(p.getValue().getFile().getName()));
        
        statusColumn.setCellValueFactory( //status przetwarzania
        p -> p.getValue().getStatusProperty());
        
        progressColumn.setCellFactory( //wykorzystanie paska postępu
        ProgressBarTableCell.<ImageProcessingJob>forTableColumn());
        
        progressColumn.setCellValueFactory( //postęp przetwarzania
        p -> p.getValue().getProgressProperty().asObject());
    }

    private void convertToGrayscale(
        File originalFile, //oryginalny plik graficzny
        File outputDir, //katalog docelowy
        DoubleProperty progressProp //własność określająca postęp operacji
    ) {
        try {
            //wczytanie oryginalnego pliku do pamięci
            BufferedImage original = ImageIO.read(originalFile);
            //przygotowanie bufora na grafikę w skali szarości
            BufferedImage grayscale = new BufferedImage(
            original.getWidth(), original.getHeight(), original.getType());
            //przetwarzanie piksel po pikselu
            for (int i = 0; i < original.getWidth(); i++) {
                for (int j = 0; j < original.getHeight(); j++) {
                    //pobranie składowych RGB
                    int red = new Color(original.getRGB(i, j)).getRed();
                    int green = new Color(original.getRGB(i, j)).getGreen();
                    int blue = new Color(original.getRGB(i, j)).getBlue();
                    //obliczenie jasności piksela dla obrazu w skali szarości
                    int luminosity = (int) (0.21*red + 0.71*green + 0.07*blue);
                    //przygotowanie wartości koloru w oparciu o obliczoną jaskość
                    int newPixel =
                    new Color(luminosity, luminosity, luminosity).getRGB();
                    //zapisanie nowego piksela w buforze
                    grayscale.setRGB(i, j, newPixel);
                }
                //obliczenie postępu przetwarzania jako liczby z przedziału [0, 1]
                double progress = (1.0 + i) / original.getWidth();
                //aktualizacja własności zbindowanej z paskiem postępu w tabeli
                Platform.runLater(() -> progressProp.set(progress));
            }
            //przygotowanie ścieżki wskazującej na plik wynikowy
            Path outputPath =
            Paths.get(outputDir.getAbsolutePath(), originalFile.getName());
            //zapisanie zawartości bufora do pliku na dysku
            ImageIO.write(grayscale, "jpg", outputPath.toFile());
        } catch (IOException ex) {
        //translacja wyjątku
            throw new RuntimeException(ex);
        }
    }
}
