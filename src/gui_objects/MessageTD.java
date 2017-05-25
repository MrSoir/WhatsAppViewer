package gui_objects;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.filechooser.FileSystemView;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import message.Message;
import message.TextMessage;
import static_methods.StaticMethods;
import message.AttachmentMessage;
import message.ServerMessage;

public class MessageTD extends Group{
	private Message message;
	private CursorSetter cursorSetter;
	
	public static interface CursorSetter{
		public void setCursor(Cursor cursor);
	}
	
	public MessageTD(Message message, CursorSetter cursorSetter, double width){
		super();
		this.message = message;
		this.cursorSetter = cursorSetter;
		addChilds(width);
//		if (message != null){
//			if (message.getClass() == AttachmentMessage.class){
//				System.out.println(message.get_content());
//			}
//		}
	}
	
	protected Message getMessage(){return message;}
	private String getStringDate(long dateL){
		Date date = new Date(dateL);
		SimpleDateFormat df2 = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        return df2.format(date);
	}
	protected void setWidth(double width){
		this.getChildren().clear();
		addChilds(width);
	}
	private void addChilds(double width){
		if (message != null){
			if (message.getClass() == AttachmentMessage.class){
				String att = ((AttachmentMessage)message).get_content().trim();
				
//	//			System.out.printf("att: %s	endsWith(.jpg): %s%n", att, att.endsWith(".jpg"));
//				
//	//			if (att.contains("attached")){
//	//				StringBuilder strB1 = new StringBuilder();
//	//				StringBuilder strB2 = new StringBuilder();
//	//				for(int i=0; i < att.length(); i++){
//	//					strB1.append(String.format("	%s	", (int)att.charAt(i)));
//	//					strB2.append("	" + att.charAt(i) + "	");
//	//				}
//	//				System.out.printf("strB1: %s%n",  strB1);
//	//				System.out.printf("strB2: %s%n",  strB2);
//	//				System.out.println();
//	//			}
				
				String actor = message.get_actor();
		        String dateStr = getStringDate(message.get_timestamp());
				Label lbl;
				if (actor != null){
					if (StaticMethods.getContactsMap().containsKey(actor)){
						actor = StaticMethods.getContactsMap().get(actor);
					}
					lbl = new Label(String.format("Am: %s | von: %s%n%s", dateStr,
																  actor,
																  att));
				}else{
					lbl = new Label(String.format("Am: %s%n%s", dateStr,
							  								 att));
				}
				
				final File file = new File(String.format("data/%s", att));
				Node node; 
				if (att.endsWith(".jpg") || att.endsWith(".png")){
					
					File tarFile = StaticMethods.getAttachmentFile(file);
					Image img = new Image(String.format("file:%s", tarFile.getAbsolutePath()));
					ImageView imgVw = new ImageView(img);
					
					double maxImgWdth = 200;
					double maxImgHght = 200;
					double facX = img.getWidth()  / maxImgWdth;
					double facY = img.getHeight() / maxImgHght;
					
					double imgHght = img.getHeight();
					if (facX > 1d || facY > 1d){
						double scaleFactor = 1 / (facX > facY ?  facX : facY);
						
						imgHght *= scaleFactor;
						
						Scale scle = new Scale();
						scle.setX(scaleFactor);
						scle.setY(scaleFactor);
						imgVw.getTransforms().add(scle);
	//					imgVw.setScaleX(scaleFactor);
	//					imgVw.setScaleY(scaleFactor);
					}
					
					lbl.setLayoutY(imgHght);
					lbl.setFont(Font.font(10));
					Rectangle offsRct = new Rectangle(0,0, lbl.getWidth(), 30);
					offsRct.setFill(Color.TRANSPARENT);
					offsRct.setLayoutY(lbl.getLayoutY()+lbl.getBoundsInParent().getHeight());
					
					Group imgGrp = new Group();
					imgGrp.getChildren().addAll(imgVw, offsRct, lbl);
					node = imgGrp;
				}else{
					Rectangle attRct = new Rectangle();
					attRct.setWidth(300);
					attRct.setHeight(100);
					
					attRct.setFill(new Color(1d,1d,1d,0.4d));
									
					final ImageView imgVw = new ImageView();
					
					final Group iconGrp = new Group();
					
					
					lbl.setLayoutY(attRct.getHeight()-30);
					lbl.setFont(Font.font(10));
					lbl.setWrapText(true);
					lbl.setMaxWidth(attRct.getWidth());
					
					iconGrp.getChildren().addAll(attRct, lbl);
					
					node = iconGrp;
					
					Runnable fetchIcon = () -> {
		             // Windows {
			                FileSystemView view = FileSystemView.getFileSystemView();
			                javax.swing.Icon icon = view.getSystemIcon(file);
		                // }
	
		                // OS X {
//			                final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
//			                javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
		                // }
		                
		                if (icon != null){
			                BufferedImage bufferedImage = new BufferedImage(
			                    icon.getIconWidth(), 
			                    icon.getIconHeight(), 
			                    BufferedImage.TYPE_INT_ARGB
			                );
			                icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		
			                Platform.runLater(() -> {
			                    Image fxImage = SwingFXUtils.toFXImage(
			                        bufferedImage, null
			                    );
			                    imgVw.setImage(fxImage);
			                    imgVw.setLayoutX( (attRct.getWidth() - imgVw.getImage().getWidth()) * 0.5 );
			                    imgVw.setLayoutY( (attRct.getHeight() - imgVw.getImage().getHeight()) * 0.5 );
			                    iconGrp.getChildren().add(imgVw);
			                });
		                }
			        };
	
			        javax.swing.SwingUtilities.invokeLater(fetchIcon);   
					
				}
				
				Tooltip toolT = new Tooltip(att);
				Tooltip.install(node, toolT);
				
				final ContextMenu cm = new ContextMenu();
				MenuItem cmItem1 = new MenuItem("Open File");
				cmItem1.setOnAction((e)->{
					StaticMethods.openFile(file);
				});
				MenuItem cmItem2 = new MenuItem("Open Folder Location");
				cmItem2.setOnAction((e)->{
					try {
						final File tarFile = StaticMethods.getAttachmentFile(file);
						if (tarFile.exists()){
							String cmnd = "";
							if (static_methods.StaticMethods.isWindows()){
								cmnd = "explorer.exe /select,%s" + tarFile.getParent(); // windows explorer default for windows
							}else if (static_methods.StaticMethods.isMac()){
								cmnd = String.format("open %s", file.getParent()); // default for mac: wird ueber command 'open' vom terminal aus geoeffnet
							}else if (static_methods.StaticMethods.isUnix()){
								cmnd = String.format("nautilus %s", file.getParent()); // nautilus: default for ubuntu
							}
							if ( !cmnd.isEmpty() ){
								System.out.println(cmnd);
								Runtime.getRuntime().exec( cmnd );
							}
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				});
				MenuItem cmItem3 = new MenuItem("Copy File Path");
				cmItem3.setOnAction((e)->{
					final File tarFile = StaticMethods.getAttachmentFile(file);
					
					if (tarFile.exists()){
						Clipboard clipboard = Clipboard.getSystemClipboard();
				        ClipboardContent content = new ClipboardContent();
				        content.putString( tarFile.getAbsolutePath() );
				        clipboard.setContent(content);
					}
				});
				cm.getItems().addAll(cmItem1, cmItem2, cmItem3);
				
				node.setOnMouseEntered(t->{
					if (cursorSetter != null){
						cursorSetter.setCursor(Cursor.HAND);
					}
				});
				node.setOnMouseExited(t->{
					if (cursorSetter != null){
						cursorSetter.setCursor(Cursor.DEFAULT);
					}
				});
				node.setOnMouseClicked(t->{
					if (t.getButton() == MouseButton.PRIMARY){
						StaticMethods.openFile(file);
					}
					else if (t.getButton() == MouseButton.SECONDARY){
						cm.show(this, t.getScreenX(), t.getScreenY());
					}
				});
				
				this.getChildren().addAll(node);
				
			}else{
				Rectangle backgrRct = new Rectangle();
								
				Text txtArea = new Text();
				if (message.getClass() == TextMessage.class){
			        Stop[] stops = new Stop[] { new Stop(0, Color.WHITE), new Stop(1, new Color(0.95, 0.95, 1.0, 1.0))};
			        LinearGradient lg1 = new LinearGradient(0, 0, 0.75, 0.75, true, CycleMethod.NO_CYCLE, stops);
					backgrRct.setFill( lg1 );
					
					txtArea.setText( ((TextMessage)message).get_content() );
				}else if(message.getClass() == ServerMessage.class){
					backgrRct.setFill(new Color(1.0d, 0.9d, 0.9d, 0.6));
					txtArea.setText( ((ServerMessage)message).get_content() );
					txtArea.setFill(new Color(0d,0d,0d, 0.5d));
					txtArea.setFont(Font.font(11));
				}
				
				String actor = message.get_actor();
		        String dateStr = getStringDate(message.get_timestamp());
		        Text lbl;
				if (actor != null){
					if (StaticMethods.getContactsMap().containsKey(actor)){
						actor = StaticMethods.getContactsMap().get(actor);
					}
					lbl = new Text(String.format("Am: %s | von: %s", dateStr,
																  actor));
				}else{
					lbl = new Text(String.format("Am: %s", dateStr));
				}
				
				lbl.setFont(Font.font(11));
				
				double txtAreaWidth = txtArea.getBoundsInLocal().getWidth();
				double minWidth = width < txtAreaWidth ? width : txtAreaWidth;
				minWidth = lbl.getBoundsInLocal().getWidth() > minWidth ? lbl.getBoundsInLocal().getWidth() : minWidth;
				
				txtArea.setWrappingWidth(minWidth);
				
				double insets = 5d;
				
				double backgrWidth = txtArea.getBoundsInLocal().getWidth()  + 2d*insets;
						
				backgrRct.setWidth(  backgrWidth );
	//			backgrRct.setHeight( txtArea.getBoundsInLocal().getHeight() + 2d*insets);
				backgrRct.setArcWidth(20);
				backgrRct.setArcHeight(20);				
				
				double yOffs = insets;
				
				lbl.setLayoutX(4);
				lbl.setLayoutY( yOffs + lbl.getBaselineOffset() );
				
				yOffs = lbl.getBoundsInLocal().getHeight() + 5;
				
				Line line = new Line(0, yOffs,
									 backgrRct.getWidth(), yOffs);
				
				lbl.setFill(new Color(0d,0d,0d, 0.5d));
				line.setStroke(Color.LIGHTGRAY);
				
				yOffs += 4;
				
				txtArea.setLayoutX(insets);
				txtArea.setLayoutY( yOffs + txtArea.getBaselineOffset() );
				
				yOffs += txtArea.getBoundsInLocal().getHeight() + insets;
				
				backgrRct.setHeight( yOffs );
				
				this.getChildren().addAll(backgrRct, lbl, line, txtArea);
				
				final ContextMenu cm = new ContextMenu();
				MenuItem cmItem1 = new MenuItem("Copy Text");
				cmItem1.setOnAction(new EventHandler<ActionEvent>() {
				    public void handle(ActionEvent e) {
				        Clipboard clipboard = Clipboard.getSystemClipboard();
				        ClipboardContent content = new ClipboardContent();
				        content.putString(message.get_content());
				        clipboard.setContent(content);
				    }
				});
				cm.getItems().add(cmItem1);
				this.setOnMouseClicked(t->{
					if (t.getButton() == MouseButton.SECONDARY){
						cm.show(this, t.getScreenX(), t.getScreenY());
					}else if (t.getButton() == MouseButton.PRIMARY){
					}
				});
			}
		}
	}
	
	
	
	public void close(){
		message = null;
		cursorSetter = null;
	}

}
