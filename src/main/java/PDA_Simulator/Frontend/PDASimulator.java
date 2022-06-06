package PDA_Simulator.Frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * This class is used to launch the JavaFX application and serves no other purpose.
 *
 * @author Savraj Bassi
 */

public class PDASimulator extends Application {

    /**
     * Starts the application by loading main-view.fxml, creating a Scene for the Stage, showing
     * the Stage and initialises the MainController by passing the Stage object to it.
     *
     * @param stage The primary Stage object for the application.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PDASimulator.class.getResource("/FXML/" +
                "main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 700);
        stage.setTitle("PDA Simulator");
        stage.setScene(scene);
        stage.show();

        MainController mainController = fxmlLoader.getController();
        mainController.setStage(stage);
    }

    /**
     * Invokes the launch method of the javafx.application.Application class to launch the
     * application.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        launch();
    }

}