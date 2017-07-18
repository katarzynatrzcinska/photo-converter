/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package photoconverter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Kasia
 */
class ImageProcessingJob {
    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_PROCESSING = "processing...";
    public static final String STATUS_DONE = "completed";
    
    File file;
    SimpleStringProperty status;
    DoubleProperty progress;
    
    public ImageProcessingJob(File file) throws MalformedURLException {
        this.file = new File(file.getPath());
        this.status = new SimpleStringProperty(STATUS_WAITING);
        this.progress = new SimpleDoubleProperty(0);
    }
    
    public File getFile() {
        return file;
    }

    public SimpleStringProperty getStatusProperty() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    public DoubleProperty getProgressProperty() {
        return progress;
    }
    
    public void setProgress(Double progress) {
        this.progress.set(progress);
    }   
}
