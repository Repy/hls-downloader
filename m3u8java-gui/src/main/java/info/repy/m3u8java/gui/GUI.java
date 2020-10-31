/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.repy.m3u8java.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author repy
 */
public class GUI extends Application {

	private Stage stage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setTitle("m3u8 GUI");
		stage.setScene(new Scene(FXMLLoader.load(getClass().getClassLoader().getResource("Form.fxml"))));
		stage.show();
	}

	@Override
	public void stop() throws Exception {
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv6Addresses", "true");
		System.setProperty("http.agent", "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53");
		launch(args);
	}

}
