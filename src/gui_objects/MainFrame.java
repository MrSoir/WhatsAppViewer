package gui_objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.dropbox.core.DbxException;

import gui_objects.MainFrame.Interrupter;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.control.Alert;
import message.Message;
import static_methods.StaticMethods;
import static_methods.StaticMethods.Contact;
import whatsappbackupviewer.MainWAH;

public class MainFrame {
	private Scene scene;
	private List<Message> messages;
	
	private Button nextSearchRsltBtn;
	private Button prevSearchRsltBtn;
	
	private MessagesPane scrPne ;
	
	private VBox mainLayout = new VBox();
	
	private final FancyWaitingBar waitBar;
	private final AtomicBoolean isDownloading = new AtomicBoolean(false);
	
	public MainFrame(Stage primaryStage, List<Message> messages){
		
		primaryStage.setTitle("WhatsAppViewer");
		primaryStage.getIcons().add(new Image("file:./pics"+File.separator+"Whatsapp.png"));
		
		this.messages = messages;
				
		mainLayout.setBackground(new Background(new BackgroundFill(Color.web("#000000"), CornerRadii.EMPTY, Insets.EMPTY)));
				
		scene = new Scene(mainLayout, 800, 600);
		
		ToolBar toolBar = getToolBar(primaryStage);

		scrPne = new MessagesPane(messages, scene);
		
		waitBar = new FancyWaitingBar(20, 20);
		waitBar.setRectanglesHeight(20);
		waitBar.setArcFactor(0d);
		
		scene.widthProperty().addListener(t->{
			waitBar.setWidth(scene.getWidth());
		});
		
		mainLayout.getChildren().add(toolBar);
//		vBox.getChildren().add(waitBar);
		mainLayout.getChildren().add(scrPne);
		
		primaryStage.setMinWidth(350);
		
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		
		primaryStage.setScene(scene);
		primaryStage.show();

		mainLayout.getChildren().add(1, waitBar);
		isDownloading.set(true);
		waitBar.run();
//			this.looooongTakingTestMethond(
    	MainWAH.updateMessages( //<- diese Methode ist die eigentliche download-methode die dann ablaufen muss!
    			new Interrupter(){
					@Override
					public void interrupted() {
						waitBar.interruptBar();
						mainLayout.getChildren().remove(waitBar);
						isDownloading.set(false);
						MainFrame.this.updateData();
					}
					@Override
					public void finished(List<Message> newMessages){
						waitBar.interruptBar();
						mainLayout.getChildren().remove(waitBar);
						isDownloading.set(false);
						MainFrame.this.messages = newMessages;
						scrPne.messagesHaveChanged(newMessages);
						MainFrame.this.updateData();
					}
					@Override
					public void failed(){
						waitBar.interruptBar();
						mainLayout.getChildren().remove(waitBar);
						isDownloading.set(false);
						MainFrame.this.updateData();
					}
					@Override
					public void updateProgress(String state){
						waitBar.updateProgress(state);
					}
    			});
	}
	
	public void updateMessages(List<Message> newMessages){
		this.messages = newMessages;
		scrPne.messagesHaveChanged(newMessages);
	}
	
	public interface Interrupter{
		public void interrupted();
		public void finished(List<Message> newMessages);
		public void failed();
		public void updateProgress(String state);
	}
	
	private static Button getButton(String pngPath){
		return getButton(pngPath, 0);
	}
	private static Button getButton(String pngPath, int rotate){
		Image imageOk = new Image(String.format("file:%s", pngPath));
		ImageView imgVw = new ImageView(imageOk);
		imgVw.setRotate(rotate);
		return new Button("", imgVw);
	}
	private enum LastInput{TEXT_SEARCH_FIELD, DATE_SEARCH_FIELD}
	private LastInput lastInputFrom = LastInput.TEXT_SEARCH_FIELD;
	private String lastSearchTxt = "";
	private String lastDateTxt = "";
	private void textFieldSearch(TextField seachTxtFld){
		lastInputFrom = LastInput.TEXT_SEARCH_FIELD;
		if ( lastSearchTxt.equals( seachTxtFld.getText()) ){
			scrPne.focusNextSearchResult();
		}else{
			lastSearchTxt = seachTxtFld.getText();
			SearchForText( lastSearchTxt );
		}
	}
	private void dateFieldSearch(TextField dateTxtFld){
//		if ( lastDateTxt.equals( dateTxtFld.getText()) ){
			long inputDate = -1l;
			try {
				inputDate = StaticMethods.parse_date(dateTxtFld.getText());
			} catch (Exception e) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error Dialog");
				alert.setHeaderText("Invalid Date!");
				alert.setContentText(String.format("The date format must match: %n"
						+ "'dd:mm:yy HH:MM(:SS) (AM|PM)' or%n"
						+ "'dd:mm:yy'", "" ));

				alert.showAndWait();
			}

			if (inputDate > -1l){
				int i = 0;
				for(; i < messages.size(); i++){
					if (messages.get(i).get_timestamp() > inputDate){
//						i--;
						break;
					}
				}
				if (i < 0){
					i = 0;
				}
				if (i >= messages.size()){
					i = messages.size() -1;
				}
				if (i >= 0 && i < messages.size() && inputDate > -1l){
					scrPne.setSearchResults( Arrays.asList( i ) );
				}
			}
//		}
	}
	private ToolBar getToolBar(Stage primaryStage){		
		final ToolBar toolBar = new ToolBar();
		
		final TextField searchTxtFld = new TextField("");
		searchTxtFld.textProperty().addListener(t->{
			lastInputFrom = LastInput.TEXT_SEARCH_FIELD;
		});
		searchTxtFld.setOnAction(t->{
			textFieldSearch(searchTxtFld);
		});
		
		final TextField dateTxtFld = new TextField(lastDateTxt);
		dateTxtFld.setPromptText("dd:mm:yy[yy] [HH:MM:SS] [AM|PM]");
		dateTxtFld.textProperty().addListener(t->{
			lastInputFrom = LastInput.DATE_SEARCH_FIELD;
		});
		dateTxtFld.setOnAction(t->{
			dateFieldSearch(dateTxtFld);
		});
		
				
		Button loupeBtn = getButton("./pics" + File.separator + "loupe_small.png");
		loupeBtn.setOnAction(t->{
			if (lastInputFrom == LastInput.TEXT_SEARCH_FIELD){
				textFieldSearch(searchTxtFld);
			}
			else{
				dateFieldSearch(dateTxtFld);
			}
		});
		
		nextSearchRsltBtn = getButton("./pics" + File.separator + "next_smallII.png", 90);
		nextSearchRsltBtn.setOnAction(t->{
			scrPne.focusNextSearchResult();
		});
		prevSearchRsltBtn = getButton("./pics" + File.separator + "next_smallII.png", -90);
		prevSearchRsltBtn.setOnAction(t->{
			scrPne.focusPrevSearchResult();
		});
		
		VBox nxtPrevBx = new VBox(prevSearchRsltBtn, nextSearchRsltBtn);
		nxtPrevBx.setAlignment(Pos.CENTER);
		nxtPrevBx.setSpacing(2);
		
		Button loadDataBtn = getButton("./pics" + File.separator + "refresh_small.png");
		loadDataBtn.setOnAction(t->{
			updateData();
		});
		
		Button uploadBackupBtn = getButton("./pics" + File.separator + "upload_small.png");
		uploadBackupBtn.setOnAction(t->{
			if (!isDownloading.get()){
				mainLayout.getChildren().add(1, waitBar);
				isDownloading.set(true);
				waitBar.run();
				File fle = loadDirectoryChooser(primaryStage);
				if (fle != null){
					MainWAH.updateChatBackupToServer(
							Paths.get(fle.getAbsolutePath()),
							new Interrupter(){
								@Override
								public void interrupted() {
									waitBar.interruptBar();
									mainLayout.getChildren().remove(waitBar);
									isDownloading.set(false);
								}
								@Override
								public void finished(List<Message> newMessages){
									waitBar.interruptBar();
									mainLayout.getChildren().remove(waitBar);
									isDownloading.set(false);
									MainFrame.this.updateData();
								}
								@Override
								public void failed(){
									waitBar.interruptBar();
									mainLayout.getChildren().remove(waitBar);
									isDownloading.set(false);
								}
								@Override
								public void updateProgress(String state){
									waitBar.updateProgress(state);
								}
			    			});
				}else{
					waitBar.interruptBar();
					mainLayout.getChildren().remove(waitBar);
					isDownloading.set(false);
				}
			}
		});
		
//		seachTxtFld.prefHeightProperty().bind(loupeBtn.heightProperty());
		final Label txtLbl = new Label("Text:");
		final Label dteLbl = new Label("Date:");
		txtLbl.setLayoutY(txtLbl.getBaselineOffset());
		dteLbl.setLayoutY(dteLbl.getBaselineOffset());
		
		final GridPane txtFldsGrd = new GridPane();
		txtFldsGrd.add(txtLbl,  		0, 0);
		txtFldsGrd.add(searchTxtFld,  1, 0);
		txtFldsGrd.add(dteLbl,  		0, 1);
		txtFldsGrd.add(dateTxtFld,  1, 1);
		txtFldsGrd.getColumnConstraints().add(new ColumnConstraints(50));
		
		scene.widthProperty().addListener(t->{
			double width = scene.getWidth() * 0.4;
			if (width > 600)
				width = 600;
			if (width < 50)
				width = 50;

			txtFldsGrd.setPrefWidth(width);
			dateTxtFld.setPrefWidth(width);
		});
		Button contactsBtn = getButton("./pics" + File.separator + "contacts_small.png");
		contactsBtn.setOnAction(t->{
			Path cntcsPth = Paths.get(StaticMethods.CONTACTS_PATH);
			if (!Files.exists(cntcsPth)){
				try {
					Files.createFile(cntcsPth);
				} catch (IOException e) {
					System.err.printf("could not create contacts-file!%n");
					return;
				}
			}
			
			try{
				List<Contact> contacts = StaticMethods.getContacts();
				new ContactTableView(contacts); // will display a modal window with the contacts utilizing a tableview
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		toolBar.getItems().addAll(
				txtFldsGrd,
				loupeBtn,
				nxtPrevBx,
//				prevSearchRsltBtn,
//				nextSearchRsltBtn,
				new Separator(Orientation.VERTICAL),
				loadDataBtn,
				new Separator(Orientation.VERTICAL),
				uploadBackupBtn,
				new Separator(Orientation.VERTICAL),
				contactsBtn
		);
		return toolBar;
	}
	
	public File loadDirectoryChooser(Stage primaryStage){
		 DirectoryChooser directoryChooser = new DirectoryChooser();
         return directoryChooser.showDialog(primaryStage);
	}
	public File loadFileDialog(Stage primaryStage, String fileDescriptn, String... extensions){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Backup Zip");
		
		if (extensions == null || extensions.length == 0){
		}
		else{
			fileChooser.getExtensionFilters().addAll(
					new ExtensionFilter(fileDescriptn, ".zip"));
		}
		return fileChooser.showOpenDialog(primaryStage);
	}
	
	private void updateData(){
		if (!isDownloading.get()){
			mainLayout.getChildren().add(1, waitBar);
			isDownloading.set(true);
			waitBar.run();
			
//			this.looooongTakingTestMethond(
	    	MainWAH.loadDataFromServerAPI( //<- diese Methode ist die eigentliche download-methode die dann ablaufen muss!
	    			new Interrupter(){
						@Override
						public void interrupted() {
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
						}
						@Override
						public void finished(List<Message> newMessages){
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
							MainFrame.this.messages = newMessages;
							scrPne.messagesHaveChanged(newMessages);
						}
						@Override
						public void failed(){
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
						}
						@Override
						public void updateProgress(String state){
							waitBar.updateProgress(state);
						}
	    			});
		}
	}
	
	private void looooongTakingTestMethond(Interrupter interrupter){
		Task<List<Message>> tsk = new Task<List<Message>>(){
			@Override
			protected List<Message> call() throws Exception {
				double progress = 0d;
				int max = 1000;
				for(int i=0; i < max; i++){
					Thread.sleep(20);
					progress = (double)i / (double)max;
					interrupter.updateProgress(String.format("%.2f%s", progress, '%'));
				}
				return null;
			}	
			
		};
		tsk.setOnSucceeded(t->{
			List<Message> loadedMessags = tsk.getValue();
			if (loadedMessags != null){
				interrupter.finished(messages);
			}else{
				interrupter.failed();
			}
		});
		tsk.setOnCancelled(t->{
			interrupter.failed();
		});
		tsk.setOnFailed(t->{
			interrupter.failed();
		});
		
		
		Thread thrd = new Thread(tsk);
		thrd.setDaemon(true);
		thrd.start();
	}
	
	private void SearchForText(String searchTxt){
		searchTxt = searchTxt.toLowerCase();
		List<Integer> treffer = new ArrayList<>();
		for(int i=0; i < messages.size(); i++){
			if (messages.get(i).get_content().toLowerCase().contains(searchTxt)){
				treffer.add(i);
			}
		}
		
		boolean disblSearBtns = treffer.size() == 0;
		nextSearchRsltBtn.setDisable(disblSearBtns);
		prevSearchRsltBtn.setDisable(disblSearBtns);
		
		System.out.printf("treffer: %s%n", treffer);
		
		scrPne.setSearchResults(treffer);
	}
}
