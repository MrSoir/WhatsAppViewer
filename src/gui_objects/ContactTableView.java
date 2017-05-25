package gui_objects;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import static_methods.StaticMethods;
import static_methods.StaticMethods.Contact;
 
public class ContactTableView extends Stage {
	 
    private final TableView<Contact> table = new TableView<>();
    private ObservableList<Contact> data;
    final HBox hb = new HBox();
    
    protected ContactTableView(List<Contact> data){
    	super();
    	if (data != null){
        	this.data = FXCollections.observableArrayList(data);
    		this.initModality(Modality.APPLICATION_MODAL);
    		render();
    	}
    }

    public void render() {
        Scene scene = new Scene(new Group());
        this.setWidth(450);
        this.setHeight(550);
 
        final Label label = new Label("Address Book");
        label.setFont(new Font("Arial", 20));
 
        table.setEditable(true);
 
        TableColumn<Contact, String> firstNameCol = 
            new TableColumn<>("phone");
        firstNameCol.setMinWidth(100);
        firstNameCol.setCellValueFactory(
            new PropertyValueFactory<>("phone"));
        
        firstNameCol.setCellFactory(TextFieldTableCell.<Contact>forTableColumn());
        firstNameCol.setOnEditCommit(
            (CellEditEvent<Contact, String> t) -> {
            	if (validatePhoneEntry(t.getNewValue())){
	            	((Contact) t.getTableView().getItems().get(
	                        t.getTablePosition().getRow())
	                        ).setPhone(t.getNewValue());
            	}
                
        });
 
 
        TableColumn<Contact, String> lastNameCol = 
            new TableColumn<>("name");
        lastNameCol.setMinWidth(100);
        lastNameCol.setCellValueFactory(
            new PropertyValueFactory<>("name"));
       lastNameCol.setCellFactory(TextFieldTableCell.<Contact>forTableColumn());
       lastNameCol.setOnEditCommit(
            (CellEditEvent<Contact, String> t) -> {
                ((Contact) t.getTableView().getItems().get(
                        t.getTablePosition().getRow())
                        ).setName(t.getNewValue());
        });

 
        table.setItems(data);
        table.getColumns().addAll(firstNameCol, lastNameCol);
 
        final TextField phoneTxtFld = new TextField();
        phoneTxtFld.setPromptText("Phone number");
        phoneTxtFld.setMaxWidth(firstNameCol.getPrefWidth());
        final TextField nameTxtFld = new TextField();
        nameTxtFld.setMaxWidth(lastNameCol.getPrefWidth());
        nameTxtFld.setPromptText("Name");
 
        final Button addButton = new Button("Add");
        addButton.setOnAction((ActionEvent e) -> {
        	if (validatePhoneEntry(phoneTxtFld.getText())){
	            data.add(new Contact(
	            		phoneTxtFld.getText(),
	            		nameTxtFld.getText()));
	            phoneTxtFld.clear();
	            nameTxtFld.clear();
        	}
        });
 
        hb.getChildren().addAll(phoneTxtFld, nameTxtFld, addButton);
        hb.setSpacing(3);
        
        int insets = 10;
        
        HBox okCncl = new HBox();
        final Button okBtn = new Button("ok");
        final Button cnclBtn = new Button("cancel");
        okBtn.setOnAction(t->{
        	saveData();
        	ContactTableView.this.close();
        });
        cnclBtn.setOnAction(t->{
        	ContactTableView.this.close();
        });
        okCncl.getChildren().addAll(okBtn, cnclBtn);
        okBtn.prefWidthProperty().bind(scene.widthProperty().multiply(0.5).subtract(insets));
        cnclBtn.prefWidthProperty().bind(scene.widthProperty().multiply(0.5).subtract(insets));

        okCncl.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(okBtn, Priority.ALWAYS);
        HBox.setHgrow(cnclBtn, Priority.ALWAYS);

        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(insets, 0, 0, insets));
        vbox.getChildren().addAll(label, table, hb);
        
        BorderPane bordPne = new BorderPane();
        bordPne.setCenter(vbox);
        bordPne.setBottom(okCncl);
        
        firstNameCol.prefWidthProperty().bind(scene.widthProperty().multiply(0.5).subtract(insets));
        lastNameCol .prefWidthProperty().bind(scene.widthProperty().multiply(0.5).subtract(insets));
 
        ((Group) scene.getRoot()).getChildren().addAll(bordPne);
        
        this.setOnCloseRequest(t->{
        	saveData();
        });
 
        this.setScene(scene);
        this.show();
    }
    
    private void saveData(){
    	StaticMethods.writeContactsToFile(data);
    }
    
    private boolean validatePhoneEntry(String phoneNumb){
    	String phoneStr = phoneNumb;
    	if (StaticMethods.validPhoneNumber(phoneStr)){
    		return true;
    	}else{
    		Alert alert = new Alert(AlertType.ERROR);
    		alert.setTitle("Error Dialog");
    		alert.setHeaderText("Invalid phone number!");
    		String txt = String.format("%s%n-> phone number must be of type: +XXXXXXXX!%n(spaces in between don't matter)", phoneNumb);
    		alert.setContentText(txt);

    		alert.showAndWait();
    		return false;
    	}
    }
}