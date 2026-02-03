package com.converter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import atlantafx.base.theme.PrimerDark;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Устанавливаем современную темную тему
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Загружаем интерфейс из FXML файла
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);

        stage.setTitle("Finstore to Snowball Converter");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

