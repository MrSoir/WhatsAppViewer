package whatsappbackupviewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileTypeDetector;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWriteMode;

import gui_objects.MainFrame;
import gui_objects.MainFrame.Interrupter;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import message.AttachmentMessage;
import message.Message;
import message.ServerMessage;
import message.TextMessage;
import static_methods.StaticMethods;
import static_methods.StaticMethods.Contact;

public class MainWAH extends Application{
	
	public static Scene scene;
	
	private static List<Message> messages;
	
	private final static String _CHAT_TXT_PATH = "./data/_chat.txt";

	public static void main(String[] args) {
		
//		try {
//			standardizeFileAttachments();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		launch(args);
	}
		
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		createDirIfNonexistent("./res");
		createDirIfNonexistent("./temp");
		createDirIfNonexistent("./data");
		createFileIfNonexistent("./res/version.txt");
		createFileIfNonexistent("./res/contacts");
				
//		messages = getMessages("./data/_chat.txt");
		new MainFrame(primaryStage, messages);
	}
	
	public static void updateMessages(Interrupter interrupter){
		Task<List<Message>> tsk = new Task<List<Message>>(){
			@Override
			protected List<Message> call() throws Exception {
				interrupter.updateProgress("loading messages");
				return (messages = getMessages(_CHAT_TXT_PATH));
			}
		};
		tsk.setOnSucceeded(t->{
			List<Message> loadedMessags = tsk.getValue();
			if (loadedMessags != null){
				interrupter.finished(loadedMessags);
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

    public static Message process_line(String line) {
        try {
            // if the message fits the server message regex make servermessageOBJ
            // if the message fits the attachment message regex make attachmentmessageOBJ
            if (Message.isAttachment(line)) {
//            	System.out.printf("isAttachment: %s%n", line);
                return new AttachmentMessage(line);
            } 
            // if the message fits the text message regex make textmessageOBJ
            else if (Message.isText(line)) {
//            	System.out.printf("isText: %s%n", line);
                return new TextMessage(line);
            } else if (Message.isServerevent(line)) {
//            	System.out.printf("isServerevent: %s%n", line);
                return new ServerMessage(line);                
            }else {
            	System.exit(0);
//                throw new Exception("Message [" + line + "] doesn't conform to known patterns.");
            }           
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
	private static List<Message> getMessages(String filePath){//String f){
		System.out.println("getMessages");
		try {
			Path path = Paths.get(filePath);
			if (Files.exists(path) && path.getParent() != null){
				replaceNamesInPlacesOfPhoneNumbers(path.toString());
				standardizeFileAttachments(path.getParent().toString());
				System.out.printf("%n%nstandardizeFileAttachments finished%n--------------------------------------------------%n");
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		if (Files.exists(Paths.get(filePath))){
			// 1. read from file structure: 
			// filePath == filepath of _chat.txt-file
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(
	        		new FileInputStream(new File(filePath)), StaticMethods.charset))){
				return readChatTxtFromBfReader(reader);
			}catch(FileNotFoundException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
				
		return null;
	}

	private static List<Message> readChatTxtFromBfReader(BufferedReader reader) throws IOException{
		List<Message> messages = new ArrayList<>();
		  
		  Message msg = null;
		  
		  String line;
		  
		  int counter = 0;
		  
		  while((line = reader.readLine()) != null){
			  
//			  if (counter++ > 100)break;
			  
//			  System.out.printf("%s%n", line);
			  
			    line = processString(line);
			    			    			  						  						
				if ( lineIsAMessage(line) ){
					msg = process_line(line);
					
					if (msg == null){
						System.err.printf("msg-error! something went wrong at line: %s%n", counter);
					}
					messages.add( msg );
				}else{
					// falls die derzeitige zeile keine message ist, kann sie (nach derzeitigem verstaendnis)
					// nur zusaetzlicher text des vorangeganegnen message-objekts sein -> damit muss das vorangegangene 
					// message objekt vom typ TextMessage sein:
					if (msg != null && 
							msg.getClass() == TextMessage.class || msg.getClass() == AttachmentMessage.class){
						if (msg.getClass() == TextMessage.class){
							Message txtMsg = (Message)msg;
							txtMsg.append_message( System.lineSeparator() + line );
						}else{
							Message message = new TextMessage(msg.get_timestamp(), msg.get_actor(), line);
							messages.add(message);
							msg = message;
						}
						
					}else{
						System.err.printf("Derzeitige line ist keine message, aber zuvorige line was auch keine TextMessage -> was also ist diese zeile???"
								+ "%n		%s%n", line);
					}
				}
		  }
		  return messages;
	}
	
	// checkt, ob eine zeile der anfang einer message ist (textmessage, servermessage etc.)
	private static boolean lineIsAMessage(String line){
		return Message.isServerevent(line) || Message.isAttachment(line) || Message.isText(line);
	}
	
	// in dem WhatsApp-backup-text-file ("_chat.txt") stecken einige schraege characters drin, dieser wird sich hier
	// entledigt:
	private static String processString(String str){
		return str.replaceAll(String.format("%s", (char)160), " ")
				  .replaceAll(String.format("%s", (char)8234), "")
				  .replaceAll(String.format("%s", (char)8236), "")
				  .replaceAll(String.format("%s", (char)8206), "");
	}
	
	
	
	//-------------------------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------------------------
	
	private static String standardizeFileName(Message msg, String... additionals){
		StringBuilder strB = new StringBuilder( String.format("%s", msg.get_timestamp()) );
		for(String str: additionals){
			strB.append(str);
		}
		return strB.toString();
	}
	private static boolean isAlreadyStandardized(String fileName){
		int dotIndex = fileName.lastIndexOf(".") < 0 ? fileName.length() : fileName.lastIndexOf(".");
		String fileNameSansExt = fileName.substring(0, dotIndex);
		return (fileNameSansExt.length() >= "00000000000000".length() && // datumsformate sehen so aus: 1451602800000
				fileNameSansExt.length() <= "0000000000000000".length() && // dann kommt noch der duplicateCounter hinzu: heir also  bis zu 100 nachrichten, die gleichzeitig versendet werden koennen (extreeeem unwahrsch.)
				onlyDigits(fileNameSansExt) && 
				Long.parseLong(fileNameSansExt.substring(0, 13)) >= 1451602800000l && // 01.01.2016
				Long.parseLong(fileNameSansExt.substring(0, 13)) <= 1577833200000l);  // 01.01.2020
	}
	private static boolean onlyDigits(String str){
		for(char c: str.toCharArray()){
			if (c < 48 || c > 57){
				return false;
			}
		}
		return true;
	}
	private static String guessFileType(String fileName){
		int dotId = fileName.lastIndexOf(".");
		String ext = fileName.substring(dotId, fileName.length());
		if (ext.contains(" ")){
			String[] frgmnts = ext.split(" ");
			if (frgmnts != null && frgmnts.length > 0 && frgmnts[0].length() > 0){
				return frgmnts[0];
			}
		}else{
			return ext;
		}
		return null;
	}
	private static String filterNumbers(String str){
		StringBuilder strB = new StringBuilder();
		for(int i=0; i < str.length(); i++){
			char c = str.charAt(i);
			if (c >= 48 && c <= 57){
				strB.append(c);
			}
		}
		return strB.toString();
	}
	private static class LineInChat{
		private String line;
		private int lineID;
		private String info;
		private LineInChat(String line, int lineID, String info){
			this.line = line;
			this.lineID = lineID;
			this.info = info;
		}
	}
	public static void replaceNamesInPlacesOfPhoneNumbers(String _chat_path) throws Exception{
		List<Contact> contacts = StaticMethods.getContacts();
		List<String> names = new ArrayList<>();
		for(Contact c: contacts){
			names.add(c.getName());
		}
		Path chatPath = Paths.get(_chat_path);
		if (Files.exists(chatPath)){
			Path tempFile = Paths.get( StaticMethods.TEMP_PATH + File.separator + "_chat_temp.txt" );
			BufferedReader chat = Files.newBufferedReader(chatPath, StaticMethods.charset);
			BufferedWriter writer = Files.newBufferedWriter(tempFile, StaticMethods.charset);
			
			String line;
			while( (line = chat.readLine()) != null){
				line = processString(line);
				Message msg = null;
				if (Message.isAttachment(line)){
					msg = new AttachmentMessage(line);
				}else if(Message.isServerevent(line)){
					msg = new ServerMessage(line);
				}else if (Message.isText(line)){
					msg = new TextMessage(line);
				}
				if (msg != null){
					if( !StaticMethods.validPhoneNumber(msg.get_actor()) 
							&& !msg.get_actor().endsWith(" was")  // spezifische server-messages die hier nicht abgefangen werden wollen
							&& !msg.get_actor().endsWith("You're")){
						String invalidPhonNumb = msg.get_actor();
						if (names.contains(invalidPhonNumb)){
							for(int i=0; i < contacts.size(); i++){
								if (contacts.get(i).getName().equals(invalidPhonNumb)){
									if (contacts.get(i).getPhone() != null && StaticMethods.validPhoneNumber(contacts.get(i).getPhone())){
										line = line.replaceFirst(invalidPhonNumb, contacts.get(i).getPhone());
									}
									break;										
								}
							}
						}else{
							contacts.add(new StaticMethods.Contact("",invalidPhonNumb));
							names.add(invalidPhonNumb);
						}
					}
				}
				writer.write(line + System.lineSeparator());
			}
			writer.close();
			chat.close();
			StaticMethods.writeContactsToFile(contacts);
			Files.move(tempFile, chatPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	public static void standardizeFileAttachments(String directory) throws Exception{
		System.out.printf("in standardizeFileAttachments%n");
//		Task<Void> tsk = new Task<Void>(){
//			@Override
//			protected Void call() throws Exception {
				Path chatPath = Paths.get(directory + File.separator + "_chat.txt");
				Path adjChatPath = Paths.get("./temp" + File.separator + "_chat_in_progress.txt");
				
				BufferedReader chat = Files.newBufferedReader(chatPath, StaticMethods.charset);
				BufferedWriter chatInProg = Files.newBufferedWriter(adjChatPath, StaticMethods.charset);
				
				String line;
				List<LineInChat> filesNotFound = new ArrayList<>();
				
				// 1. in einem ersten schritt die attachments standardisieren(umbenennen), deren 
				// exakter dateiname gefunden werden kann:
				int lineCounter = 0;
				while( (line = chat.readLine()) != null){
				    line = processString(line);

//					System.out.printf("%s%n", line);
					if (Message.isAttachment(line)){
						AttachmentMessage atchmnt = new AttachmentMessage(line);
						String fileName = atchmnt.get_content().trim();
						if(	isAlreadyStandardized(fileName) ){ 
							// bereits in regulaerem format vorliegend!
//							System.out.printf("file %s already standardized!%n", fileName);
						}else{
							System.out.printf("	isAttachment:%n			fileName: %s%n", fileName);
							Path filePath = Paths.get(directory + File.separator + fileName);
							if (Files.exists(filePath)){
								String fileTyp = guessFileType( fileName );
								
								String stndrdzdFleNme;
								Path stndrdzdFilePath;
								int duplicateCounter = 0;
								do{
									stndrdzdFleNme = standardizeFileName( atchmnt, Integer.toString(duplicateCounter++), fileTyp );
									stndrdzdFilePath = Paths.get(directory + File.separator + stndrdzdFleNme);
								}while(Files.exists(stndrdzdFilePath));
								
								System.out.printf("			stndrdzdFleNme: %s%n", 	stndrdzdFleNme);
	
								Files.move(filePath, stndrdzdFilePath, StandardCopyOption.REPLACE_EXISTING);
								if (!line.contains(fileName)){
									System.err.printf("line does not contain file name!%n");
								}
								line = line.replaceFirst(fileName, stndrdzdFleNme);
								System.out.printf("->%s%n", line);
							}else{
								System.out.printf("file not found: %s%n", line);
								filesNotFound.add( new LineInChat(line, lineCounter, fileName));
							}
						}
					}
//					System.out.printf("		-> %s%n", line);
					chatInProg.write(line + System.lineSeparator());
					lineCounter++;
				}
				chat.close();
				chatInProg.close();
				Files.move(adjChatPath, chatPath, StandardCopyOption.REPLACE_EXISTING);
				// 2. nun noch die nicht entdeckten file-names ersetzen, indem der am aehnlich klingendste
				// dateiname herangezogen wird: (daher sind 2 schritte notwendig!! auf gar keinen fall in einem durchfuehren!!!!)
				if (filesNotFound.size() > 0){
					chat = Files.newBufferedReader(chatPath, StaticMethods.charset);
					chatInProg = Files.newBufferedWriter(adjChatPath, StaticMethods.charset);
//					dffd;
					lineCounter = 0;
					int notFoundCounter = 0;
					while( (line = chat.readLine()) != null){
			    		line = processString(line);
			    		if (notFoundCounter < filesNotFound.size()){
			    			LineInChat lic = filesNotFound.get(notFoundCounter);
												
							if (lineCounter == lic.lineID){
								String oldFileName = lic.info;
								File mostSimilarFile = StaticMethods.getAttachmentFile(new File(directory + File.separator + oldFileName));
								System.out.printf("	isAttachment:%n"
										+ "		fileName:			%s%n", lic.info);
								System.out.printf("		mostSimilarFile:		%s%n", mostSimilarFile.getName());
								if (mostSimilarFile.exists()){
									Message atchmnt = new AttachmentMessage(line);
									
									String fileTyp = guessFileType(mostSimilarFile.getName());
									
									Path srcFilePath = mostSimilarFile.toPath();
									
									String stndrdzdFleNme;
									Path tarFilePath;
									int duplicateCounter = 0;
									do{
										stndrdzdFleNme = standardizeFileName( atchmnt, Integer.toString(duplicateCounter++), fileTyp );
										tarFilePath = Paths.get(directory + File.separator + stndrdzdFleNme);
									}while(Files.exists(tarFilePath));
									
									System.out.printf("	replacing file from:%n		%s%n	to%n		%s%n", srcFilePath.toString(), 
											tarFilePath.toString());
									line = line.replaceFirst(oldFileName, stndrdzdFleNme);
									System.out.printf("line after manipulation:%n		%s%n", line);
									Files.move(srcFilePath, tarFilePath, StandardCopyOption.REPLACE_EXISTING);
								}
								notFoundCounter++;
							}
						}
						lineCounter++;
						chatInProg.write(line + System.lineSeparator());
					}
					chat.close();
					chatInProg.close();
					Files.move(adjChatPath, chatPath, StandardCopyOption.REPLACE_EXISTING);
				}
				
//				return null;
//			}
//			
//			private void failed(Exception ex) throws Exception{
//				interrupter.updateProgress("updating failed");
//				try {
//						Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				throw ex;
//			}
//		};
//		tsk.setOnSucceeded(t->{
//			interrupter.finished(null);
//		});
//		tsk.setOnCancelled(t->{
//			interrupter.failed();
//		});
//		tsk.setOnFailed(t->{
//			interrupter.failed();
//		});
//		
//		Thread thrd = new Thread(tsk);
////		thrd.setDaemon(true);
//		thrd.start();
	}
	
	
	//-------------------------------------------------------------------------------------------------------


	// downloading files via Dropbox-API:
	
	private static class DialogThrower extends Exception{
		private String header;
		private String content;
		private DialogThrower(String header, String content){
			this.header = header;
			this.content = content;
		}
	}
	private static boolean _chat_txt_isValid(Path localDataPath) throws IOException, DialogThrower{
		Path chatPath = localDataPath.resolve("_chat.txt");
		if (!Files.exists( chatPath ) ){
			System.out.printf("_chat.txt does not exist!%n");
    		String txt = String.format("The folder specified (%s)%nmust contain the file '_chat.txt'!", localDataPath.toString());
    		throw new DialogThrower("No chat history detected!", txt);
		}
		
		getMessages(chatPath.toString()); // wird nur dazu genutzt, abzufragen ob 
										  // 1. die aktuelle datei keine noch nicht erfassten kontakte enthaelt
										  // 2. die dateinamen in dem angegebenen ordner standardisiert
		List<Contact> contacts = StaticMethods.getContacts();
		
		for(Contact cnt: contacts){
			if (cnt.getPhone() == null || cnt.getPhone().isEmpty()){
				System.out.printf("address boook incomplete!%n");
	    		String txt = String.format("Please complete your address book%n"
	    				+ "before updating the chat histor.%n"
	    				+ "Some contacts are missing!");
	    		throw new DialogThrower("Incomplete Address Book!", txt);
			}
		}
		return true;
	}
	
	public static void updateChatBackupToServer(Path localDataPath, Interrupter interrupter){
		
		Task<Void> tsk = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				try{
			        // Get your app key and secret from the Dropbox developers website.
			        final String APP_KEY = "INSERT_APP_KEY";
			        final String APP_SECRET = "INSERT_APP_SECRET";
			        
			        if (_chat_txt_isValid(localDataPath)){
				//        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
				
				        DbxRequestConfig config = new DbxRequestConfig(
				            "JavaTutorial/1.0", Locale.getDefault().toString());
				//        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
				        
				        DbxClient client = new DbxClient(config, StaticMethods.DROP_BOX_TOKEN);
				        System.out.println("Linked account: " + client.getAccountInfo().displayName);
				//        try{
				        	List<String> alrUploadedAttachments = getFileNamesInServerFolder(StaticMethods.attServ, client); // <- bereits wieder auf normalen fileName umgewandelt!
				//        	int count = 0;
				//        	for(String str: alrUploadedAttachments){
				//        		System.out.printf("alrUploadedAttachments: %s%n", str);
				//        		count++;
				//        	}
				////        	System.out.printf("uploaded: %s%n",  count);
				//          System.out.printf("%n%n%n%n%n%n%n");
				
				        
				         try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(localDataPath)){
				         for (Path inputFile : directoryStream) {
				        	 
				        	 String inputFileStrUnmainp = inputFile.getFileName().toString();
				        	 String inputFileStrDropBox = convertToDropBoxStr(inputFileStrUnmainp, Files.isDirectory(inputFile));
				        	         	 
				        	 if ( !inputFileStrUnmainp.equals("_chat.txt") ){
				        		 if (isAlreadyStandardized(inputFileStrUnmainp)){
					            	 boolean alrUploaded = false;
					            	 
					            	 for(int i=0; i < alrUploadedAttachments.size(); i++){
					                	 if (inputFileStrUnmainp.equals(alrUploadedAttachments.get(i))){
					                		 alrUploaded = true;
					                		 break;
					                	 }
					            	 }
					            	 if (!alrUploaded){
					            		 interrupter.updateProgress(String.format("uploading: %s%n", inputFileStrUnmainp));
					            		 System.out.printf("	uploading file: %s%n", inputFileStrUnmainp);
					            		 uploadFile(inputFile, StaticMethods.attServ + "/" + inputFileStrDropBox, client);
					            	 }
					            	 else{
					            		 System.out.printf("	file already uploaded: %s!%n", inputFileStrUnmainp);
					            	 }
				        		 }
				        	 }
				        	 else{
				        		 interrupter.updateProgress(String.format("updating: %s%n", "_chat.txt"));
				        		 System.out.println("updating _chat.txt");
				        		 update_chat_txt(inputFile, StaticMethods.rootServ + "/_chat.txt", client);
				        	 }
				         }
				         }catch(IOException e){
				        	 failed(e);
				         }
			        }else{
			        	System.out.printf("chat path is invalid!%n");
			        }
				}catch(DialogThrower e){
					throw e;
				}catch(Exception e){
					failed(e);
				}
				return null;
			}
			
			private void failed(Exception ex) throws Exception{
				interrupter.updateProgress("updating failed");
				try {
						Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				throw ex;
			}
		};
		tsk.setOnSucceeded(t->{
			interrupter.finished(null);
		});
		tsk.setOnCancelled(t->{
			interrupter.failed();
		});
		tsk.setOnFailed(t->{
			if (tsk.getException().getClass() == DialogThrower.class){
				DialogThrower ex = (DialogThrower)tsk.getException();
				Alert alert = new Alert(AlertType.ERROR);
	    		alert.setTitle("Error Dialog");
	    		alert.setHeaderText(ex.header);
	    		alert.setContentText(ex.content);
	//
	    		alert.showAndWait();
			}
			interrupter.failed();
		});
		
		Thread thrd = new Thread(tsk);
		thrd.setDaemon(true);
		thrd.start();
    }
	
	private static void update_chat_txt(Path localVersionPth, String serverPath, DbxClient client) throws IOException, DbxException{		
		Path oldServerChatHistPth = Paths.get("./temp" + File.separator + "last_server_chat.txt");
		if (client.getMetadata(serverPath) == null){
			System.out.printf("char_txt nicht vorhanden: serverPath: %s%nlocalVersionPth: %s%n!", serverPath.trim(), localVersionPth);
			uploadFile(localVersionPth, serverPath, client);
		}else{
			downloadFile(oldServerChatHistPth, serverPath, client);
			
			if (Files.exists(oldServerChatHistPth)){			
				BufferedReader readerOldServer = Files.newBufferedReader(oldServerChatHistPth, StaticMethods.charset);
				BufferedReader readerLtstLocal = Files.newBufferedReader(localVersionPth, StaticMethods.charset);
	
				StringBuilder oldServerStrB = new StringBuilder();
				StringBuilder ltstLoclStrB = new StringBuilder();
				
				String curLine;
				while( (curLine = readerOldServer.readLine()) != null){
					oldServerStrB.append(curLine).append(System.lineSeparator());
				}
				
				while( (curLine = readerLtstLocal.readLine()) != null){
					ltstLoclStrB.append(curLine).append(System.lineSeparator());
				}
				String   oldServerStr    = oldServerStrB.toString().trim();
				String[] ltstLoclStrArr  = ltstLoclStrB .toString().trim().split( System.lineSeparator() );
							
				StringBuilder tempStrBldr = new StringBuilder(  );
	
				int equalLines = 0;
							
				tempStrBldr.append(ltstLoclStrArr[0]);
				String nextLine = "";
				if (oldServerStr.contains(tempStrBldr)){
					equalLines = 1;
					int i;
					for(i=1; i < ltstLoclStrArr.length; i++){
						nextLine = ltstLoclStrArr[i];
						if (oldServerStr.contains(tempStrBldr + System.lineSeparator() + nextLine)){
							tempStrBldr.append( System.lineSeparator() + nextLine );
							equalLines++;
						}
					}
					String overlap = tempStrBldr.toString().trim();
					
					if (equalLines > 30 && oldServerStr.endsWith(overlap)){
						String totalChat = oldServerStr;
						totalChat += ltstLoclStrB.substring(overlap.length(), ltstLoclStrB.length());
						
						BufferedWriter writer = Files.newBufferedWriter(oldServerChatHistPth, StaticMethods.charset, 
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);
						
						writer.write( totalChat );
						writer.close();
						
						// delete & upload file -> gibt leider keine überrschreiben-methode wies ausschaut
						delteFile(serverPath, client); 
						uploadFile(oldServerChatHistPth, serverPath, client);
					}
				}
				
				if (Files.exists(oldServerChatHistPth)){
					Files.delete(oldServerChatHistPth);
				}
			}else{
				System.out.printf("file not downloaded%n");
			}
		}
	}
	
	private static String convertToDropBoxStr(String str, boolean isDirectory){
		StringBuilder strB = new StringBuilder();
		int dotId;
		String fileExt;
		String fileName;
		if (isDirectory){
			fileExt = ".dropboxFolder";
			fileName = str;
		}
		else{
			if ( (dotId = str.lastIndexOf('.')) > -1){
				fileName = str.substring(0,  dotId);
				fileExt = str.substring(dotId, str.length());
			}else{
				fileExt = ".pseudofileext"; // pseudofileextension...
				fileName = str;
			}
		}
		
		for(int i=0; i < fileName.length(); i++){
			int c = fileName.charAt(i);
			strB.append('u').append(Integer.toString((int)c));
		}
		
		strB.append(fileExt);
		
		return strB.toString();
	}
	private static String convertFromDropBoxStr(String str){
		String fileExt;
		String fileName;
		if ( str.endsWith(".dropboxFolder") ){
			fileExt = "";
			fileName = str.substring(0,  str.length() - ".dropboxFolder".length());
		}else if ( str.endsWith(".pseudofileext") ){
			fileExt = "";
			fileName = str.substring(0,  str.length() - ".pseudofileext".length());
		}else{
			fileExt = str.substring(str.lastIndexOf("."), str.length());
			fileName = str.substring(0,  str.lastIndexOf("."));
		}
				
		StringBuilder strB = new StringBuilder();
		String[] ints = fileName.split("u");
		for(int i=0; i < ints.length; i++){
			if (ints[i] != null && !ints[i].isEmpty()){ // erster string ist empty, da gleich das erste zeichen ein 'u' ist im string!!!
				strB.append((char)Integer.parseInt(ints[i]));
			}
		}
		
		strB.append(fileExt);
		
		return strB.toString();
	}
	
	private static void uploadFile(Path inputFile, String serverPath, DbxClient client) throws DbxException, IOException{
		InputStream inputStream = Files.newInputStream(inputFile);
        try {
            DbxEntry.File uploadedFile = client.uploadFile(serverPath, DbxWriteMode.add(), Files.size(inputFile), inputStream);
            System.out.println("Uploaded: " + uploadedFile.toString());
        } finally {
            inputStream.close();
        }
	}
	private static void delteFile(String serverPath, DbxClient client) throws DbxException{
		client.delete(serverPath);
	}
	
	private static List<String> getFileNamesInServerFolder(String servFold, DbxClient client) throws DbxException{
		List<String> filesInFold = new ArrayList<>();
		
		DbxEntry.WithChildren listing = client.getMetadataWithChildren(servFold);
        for (DbxEntry child : listing.children) {
        	if (child.isFile()){
        		filesInFold.add( convertFromDropBoxStr(child.name) );
        	}
        }
        return filesInFold;
	}
	
	private static DbxClient createDropboxConnection(){
		DbxRequestConfig config = new DbxRequestConfig( "JavaTutorial/1.0", Locale.getDefault().toString());
		return new DbxClient(config, StaticMethods.DROP_BOX_TOKEN);
	}
	
	private static int checkVersionInfo(boolean updateVersionIfOutdated) throws DbxException, IOException{
        DbxClient client = createDropboxConnection();
        
        String lastVersion = getLocalVersion();
        if (lastVersion == null){ // dann ist die datei schlicht leer
        	lastVersion = "";
        }
        if (client != null){
			DbxEntry meta = client.getMetadata("/whatsapp_backup/_chat.txt");
			if (meta != null){
				Date d = meta.asFile().lastModified;
				if (d != null){
					if ( lastVersion.equals(d.toString()) ){
						return 1;
					}else{
						if (updateVersionIfOutdated){
							updateVersionInfo(d.toString());
						}
						return 0;
					}
				}
			}
        }
        return 0;
	}
	
	private static void writeServerVersionInfoToFile()throws DbxException, IOException{
		System.out.printf("in writeServerVersionInfoToFile%n");
		DbxClient client = createDropboxConnection();
		if (client != null){
			DbxEntry meta = client.getMetadata("/whatsapp_backup/_chat.txt");
			if (meta != null){
				Date d = meta.asFile().lastModified;
				if (d != null){
					updateVersionInfo(d.toString());
				}
			}
		}
	}
	private static void updateVersionInfo(String newVersionInfo) throws IOException{
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(StaticMethods.oldVersTmpFle), StaticMethods.charset);
		writer.write(newVersionInfo, 0, newVersionInfo.length());
		writer.close();
	}
	
	private static String getLocalVersion() throws IOException{
		Path localVersionPth = Paths.get(StaticMethods.oldVersTmpFle);
		if ( !Files.exists(localVersionPth) ){
			Files.createFile(localVersionPth);
		}
		BufferedReader reader = Files.newBufferedReader(localVersionPth, StaticMethods.charset);
		return reader.readLine();
	}
	
	public static void loadDataFromServerAPI(Interrupter interrupter){
		System.out.printf("in loadDataFromServerAPI%n");
		
		Task<List<Message>> tsk = new Task<List<Message>>(){
			@Override
			protected List<Message> call() throws Exception {
//				String attPthSrvr = "/whatsapp_backup/attachments";
				
				interrupter.updateProgress("checking backup-version...");

				if (checkVersionInfo(false) == 0){
					DbxClient client = createDropboxConnection();
					
					System.out.println("downloading latest history");
                    interrupter.updateProgress(String.format("downloading latest history: %.2f%s", 0d, '%'));
					
					if (client != null){
						try {
							System.out.println("1");
							List<String> attchmntsOnServer 		 = getFileNamesInServerFolder(StaticMethods.attServ, client);
							System.out.println("1.1");
							DirectoryStream<Path> attchmntsLocal = getFilesInLocalFolder(Paths.get(StaticMethods.data_dir));
							System.out.println("2");

							long folderSize = getFolderSizeDP(StaticMethods.attServ, client);
							
							List<String> attchmntsLocalStrLst = new ArrayList<>();
							for(Path pth: attchmntsLocal){
								attchmntsLocalStrLst.add(pth.getFileName().toString());
							}
							System.out.println("3");

							
							long progress = 0;
							for(String curFilePthSrvr: attchmntsOnServer){
								if ( !attchmntsLocalStrLst.contains( curFilePthSrvr )){
									downloadFile(
											Paths.get( StaticMethods.data_dir + File.separator + curFilePthSrvr ),
											StaticMethods.attServ + "/" + convertToDropBoxStr(curFilePthSrvr, false), 
											client);
									if ( folderSize > 0 ){
										DbxEntry entry = client.getMetadata(StaticMethods.attServ + "/" + convertToDropBoxStr(curFilePthSrvr, false));
										if (entry != null){
											progress += entry.asFile().numBytes;
											interrupter.updateProgress(String.format("downloading: %.2f%s", 
																				(double)progress / (double)folderSize * 100, '%'));
										}
									}
								}
							}
							System.out.println("4");
							
							System.out.println("downloading _chat.txt");
							downloadFile(
									Paths.get( StaticMethods.data_dir + File.separator + "_chat.txt" ),
									StaticMethods.rootServ + "/" + "_chat.txt", 
									client);
							
							// wenn das programm bis hierher kam, hats alles problemfrei heruntergeladen, daher wird erst jetzt, die 
							// neue version ins log-file geschrieben:
							writeServerVersionInfoToFile();

			                interrupter.updateProgress("getting messages...");
			                return getMessages(_CHAT_TXT_PATH);
			                
						} catch (DbxException e) {
							failed(e);
						} catch (IOException e) {
							failed(e);
						}
					}
				}else{
					interrupter.updateProgress("chat-history already up-to-date!");
					return null;
				}
				return null;
			}
			private void failed(Exception ex) throws Exception{
				interrupter.updateProgress("updating failed");
				try {
						Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				throw ex;
			}
		};
		tsk.setOnSucceeded(t->{
			System.out.println("in succeeded");
			List<Message> loadedMessags = tsk.getValue();
			if (loadedMessags != null){
				System.out.println("successfully updated backup!");
				interrupter.finished(loadedMessags);
			}else{
				interrupter.failed();
			}
		});
		tsk.setOnCancelled(t->{
			System.out.println("in cancelled");
			tsk.getException().printStackTrace();
			interrupter.failed();
		});
		tsk.setOnFailed(t->{
			System.out.println("in failed");
			tsk.getException().printStackTrace();
			interrupter.failed();
		});
		
		Thread thrd = new Thread(tsk);
		thrd.setDaemon(true);
		thrd.start();
	}
	
	private static void downloadFile(Path outpuFile, String serverPath, DbxClient client) throws DbxException, IOException{
		System.out.printf("in downloadFile: outpuFile: %s	serverPath: %s%n", outpuFile, serverPath);
		if (client.getMetadata(serverPath) != null){ // erstmal checken ob die datei auf dem server ueberhaupt existiert
			System.out.println("	file to download exists!");
			FileOutputStream outputStream = new FileOutputStream(outpuFile.toFile());
			try {
			    DbxEntry.File downloadedFile = client.getFile(serverPath, null,
			        outputStream);
			    System.out.println("Metadata: " + downloadedFile.toString());
			} finally {
			    outputStream.close();
			}
		}
	}
	
	
	private static long getFolderSizeDP(String servFold, DbxClient client) throws DbxException{
		DbxEntry.WithChildren listing = client.getMetadataWithChildren(servFold);
		
		long size = 0l;
        for (DbxEntry child : listing.children) {
        	if (child.isFile()){
        		DbxEntry.File fl = child.asFile();
        		if (fl != null){
        			size += child.asFile().numBytes;
        		}
        	}else{
        		size += getFolderSizeDP(child.path, client);
        	}
        }
        return size;
	}
	
	private static DirectoryStream<Path> getFilesInLocalFolder(Path root) throws IOException{
		return Files.newDirectoryStream(root);
	}
	
	//-------------------------------------------------------------------------------------------------------
	
	
	private static void createFileIfNonexistent(String absPth){
		Path pth = Paths.get(absPth);
		if (!Files.exists(pth)){
			try {
				Files.createFile(pth);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private static void createDirIfNonexistent(String dir){
		Path pth = Paths.get(dir);
		if (!Files.exists(pth)){
			try {
				Files.createDirectory(pth);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public static void loadDataFromServer(Interrupter interrupter){
		
		Task<List<Message>> tsk = new Task<List<Message>>(){
						
			
	        String version_url = "https://www.dropbox.com/s/pv5xtkh6fz7f7cf/version.txt?dl=1";
	        String zip_url = "https://www.dropbox.com/s/oijpgcle4615iq5/WAChatBackup.zip?dl=1";
	        
			@Override
			protected List<Message> call() throws Exception {
				
				try{
					createDirIfNonexistent("./res");
					createDirIfNonexistent("./temp");
					createDirIfNonexistent("./data");
					createFileIfNonexistent("./res/version.txt");
					
					interrupter.updateProgress("checking backup-version...");
					
//                    System.out.println("Getting new version file from: " + version_url); // rausgenommen, da man so die url beim programmablauf direkt sehen kann
                    downloadFromDropbox(version_url, StaticMethods.newVersTmpFle, null);

                    // get version file
                    File versFle = new File(StaticMethods.newVersTmpFle);
                    // if it exists...
                    if (versFle.exists()){
                        System.out.println("Got version, proceeding to checking it's content.");
                        // ...check if it's the newest version
                        String versionNew = null, versionOld = null;
                        try(BufferedReader readerNew = new BufferedReader(new FileReader(versFle))){
                            try(BufferedReader readerOld = new BufferedReader(new FileReader(StaticMethods.oldVersTmpFle))){
                                versionNew = readerNew.readLine();
                                versionOld = readerOld.readLine();
                                System.out.printf("versionOld: %s%n", versionOld);
                                System.out.printf("versionNew: %s%n", versionNew);
                            } catch(FileNotFoundException e) { e.printStackTrace(); failed(e); } 
                              catch(IOException e) { e.printStackTrace(); failed(e); }
                        } catch(MalformedURLException e) { failed(e); }
                          catch(IOException e) { failed(e); }

                        if ( !versionNew.equals(versionOld) ){
                            System.out.println("Not newest version.");
                            
                        } else {
                            System.out.println("Chat-history already up-to-date!");
        					interrupter.updateProgress("chat-history already up-to-date!");
//        					Thread.sleep(2000);
                            return null;
                        }

                        // if it was not the newest version download and process the zip
//                        System.out.println("Getting latest zip from: " + zip_url);
                        interrupter.updateProgress(String.format("downloading latest history: %.2f%s", 0d, '%'));
                        downloadFromDropbox(zip_url, StaticMethods.zipFilePath, interrupter);
                        List<String> oldFlNms = new ArrayList<>();
                        Path path = Paths.get("./data");
                        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
                            for (Path entry : stream){
                                oldFlNms.add(entry.getFileName().toString());
                            }
                            stream.close();
                        }

                        System.out.println("Proceeding to unpacking process.");
                        interrupter.updateProgress("unzipping...");
                        unzip(StaticMethods.zipFilePath, StaticMethods.data_dir);

                        System.out.println("Finnished unpacking.");

                        System.out.println("Saving newest version file.");
                        Path from = Paths.get(StaticMethods.newVersTmpFle); 
                        Path to   = Paths.get(StaticMethods.oldVersTmpFle); 
                        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(from);
                    }
                } catch(Exception e) { failed(e); }
				System.out.println("successfully downloaded latest backup!");
                interrupter.updateProgress("getting messages...");
                return getMessages(_CHAT_TXT_PATH);
			}
			
			private void failed(Exception ex) throws Exception{
				interrupter.updateProgress("updating failed");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				throw ex;
			}
		};
		tsk.setOnSucceeded(t->{
			List<Message> loadedMessags = tsk.getValue();
			if (loadedMessags != null){
				System.out.println("successfully updated backup!");
				interrupter.finished(loadedMessags);
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
	private static void unzip(String zipFilePath, String tarFoldPath) throws IOException {
		try (ZipFile zf = new ZipFile( zipFilePath )){
			for ( Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ){
				ZipEntry entry = e.nextElement();
				
				Path tarFilePath = Paths.get( tarFoldPath + File.separator + entry.getName() );
				
				if (entry.isDirectory()){
					if ( !Files.exists(tarFilePath) ){
						Files.createDirectory(tarFilePath);
					}
				}else{
					InputStream in = zf.getInputStream( entry );
		            Files.copy(in, tarFilePath, StandardCopyOption.REPLACE_EXISTING);
		            in.close();
				}
			}
		} 
	}
    

    private static void downloadFromDropbox(String urlStr, String tarFile, Interrupter interrupter) 
    					throws MalformedURLException, FileNotFoundException, IOException{
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        long size = conn.getContentLengthLong();

        try(BufferedInputStream in = new BufferedInputStream(url.openStream());
        		FileOutputStream out = new FileOutputStream(tarFile)){
	        byte data[] = new byte[1024];
	        int count;
	        double sumCount = 0.0;
	        
	        double lastProg = 0d;
	
	        while ((count = in.read(data, 0, 1024)) != -1) {
	            out.write(data, 0, count);
	
	            sumCount += count;
	            if (size > 0 && interrupter != null) {
	            	double progress = (sumCount / size * 100.0);
	            	if (progress-lastProg >= 0.3){
	            		interrupter.updateProgress(String.format("downloading: %.2f%s", progress, '%'));
		            	lastProg = progress;
	            	}
	            }
	        }
        }
    }

}
