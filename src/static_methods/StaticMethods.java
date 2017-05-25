package static_methods;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gui_objects.ContactTableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import static_methods.StaticMethods.Contact;

public class StaticMethods {
	
	public static final String CONTACTS_PATH = "./res" + File.separator + "contacts";
	public static final String RES_DIR = "./res";
	public static final String oldVersTmpFle = "./res/version.txt";
	public static final String newVersTmpFle = "./temp/version.txt";
	public static final String TEMP_PATH = "./temp";
	public static final String zipFilePath = "./res/data.zip";
	public static final String data_dir = "./data";
	public static final Charset charset = Charset.forName("UTF8");
	public static final String DROP_BOX_TOKEN = "KnHzsvxtnmAAAAAAAAAATTZZXujC2Yz94Z7rB3hj3dQdVptzaCeQyi0fyfhyQHtc";
	public static final String rootServ = "/whatsapp_backup";
	public static final String attServ = rootServ + "/attachments";
	
	private static final Map<String,String> contactsMap = new HashMap<>();

	
	
	public static File getAttachmentFile(File file){
		File tarFile = file;
		
		File dataFold = file.getParentFile();
		if (!dataFold.exists()){
			dataFold = new File("./data");
		}
		int maxDist = Integer.MAX_VALUE;
		for (File curFle: dataFold.listFiles()){
			if (!file.exists()){
				int curDist = LevenshteinDistance.computeLevenshteinDistance(file.getName(), curFle.getName());
				if (curDist < maxDist){
					maxDist = curDist;
					tarFile = curFle;
				}
			}
		}
		return tarFile;
	}
	public static class LevenshteinDistance {                                               
	    private static int minimum(int a, int b, int c) {                            
	        return Math.min(Math.min(a, b), c);                                      
	    }                                                                            
	                                                                                 
	    public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {      
	        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
	                                                                                 
	        for (int i = 0; i <= lhs.length(); i++)                                 
	            distance[i][0] = i;                                                  
	        for (int j = 1; j <= rhs.length(); j++)                                 
	            distance[0][j] = j;                                                  
	                                                                                 
	        for (int i = 1; i <= lhs.length(); i++)                                 
	            for (int j = 1; j <= rhs.length(); j++)                             
	                distance[i][j] = minimum(                                        
	                        distance[i - 1][j] + 1,                                  
	                        distance[i][j - 1] + 1,                                  
	                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));
	                                                                                 
	        return distance[lhs.length()][rhs.length()];                           
	    }                                                                            
	}
	public static boolean isWindows() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.toLowerCase().indexOf("win") >= 0);
	}
	public static boolean isMac() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.toLowerCase().indexOf("mac") >= 0);
	}
	public static boolean isUnix() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}
	public static boolean isSolaris() {
		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.toLowerCase().indexOf("sunos") >= 0);
	}
	
	private static Pattern phone_pattern = Pattern.compile("^[+]([\\s]*[\\d])*$");
	public static boolean validPhoneNumber(String phoneNumb){
		return phone_pattern.matcher(phoneNumb).find();
	}
		
	public static List<Contact> getContacts() throws IOException{
		try(BufferedReader cntctsFle = Files.newBufferedReader(Paths.get(StaticMethods.CONTACTS_PATH), StaticMethods.charset)){
			String line;
			List<Contact> contacts = new ArrayList<>();
			while( (line = cntctsFle.readLine()) != null){
				String[] contData = line.split(";");
				if (contData != null && contData.length == 2){
					if (!contactsMap.containsKey(contData[0])){
	    				contactsMap.put(contData[0], contData[1]);
	    			}
					contacts.add(new Contact(contData[0], contData[1]));
				}
			}
			cntctsFle.close();
			return contacts;			
		}
	}
	public static void writeContactsToFile(List<Contact> contacts){
		System.out.printf("writingContactsToFile%n");
		Path cntcsPth = Paths.get(StaticMethods.CONTACTS_PATH);
    	try(BufferedWriter cntcts = Files.newBufferedWriter(cntcsPth, StaticMethods.charset)){
    		for(Contact cnt: contacts){
    			if (!contactsMap.containsKey(cnt.phone)){
    				contactsMap.put(cnt.getPhone(), cnt.getName());
    			}
    			cntcts.write(String.format("%s;%s", cnt.phone.getValue(), cnt.name.getValue()) + System.lineSeparator());
    		}
    		cntcts.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Map<String,String> getContactsMap(){
		return contactsMap;
	}
	
    public static class Contact {
    	 
        public final SimpleStringProperty name;
        public final SimpleStringProperty phone;
 
        public Contact(String phone, String name) {
            this.name = new SimpleStringProperty(name);
            this.phone = new SimpleStringProperty(phone);
        }
 
        public String getName() {
            return name.get();
        }
 
        public void setName(String name) {
            this.name.set(name);
        }
 
        public String getPhone() {
            return phone.get();
        }
 
        public void setPhone(String phone) {
        	this.phone.set(phone);
        }
    }
    
    public static void openFile(File file){
    	if (file != null){
			final File tarFile = StaticMethods.getAttachmentFile(file);
			
			if (tarFile.exists()){
				new Thread(new Task<Void>(){
					@Override
					protected Void call() throws Exception {
						Desktop.getDesktop().open( tarFile );
						return null;
					}
				}).start();
			}
    	}
    }
	
	public static Pattern DATE_PATTERN = Pattern.compile("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})(\\s)*(\\D)?(\\s)*((\\d{1,2})(\\:)(\\d{1,2})(\\:)?(\\d{1,2})?)?(\\s)*(AM|PM)?$");
    private static int timezone_offset = +2;
    public static long parse_date(String S) throws Exception {
    	return parse_date(S, DATE_PATTERN);
    }
	public static long parse_date(String S, Pattern date_pattern) throws Exception {
		Matcher matcher = date_pattern.matcher(S);
        String timestamp;
        String Y, M, D ,h, m, s = null, PM;
//        System.out.printf("parsing date: %s%n", S);
        if (matcher.find()) {
//        	System.out.printf("found: %s%n", matcher.groupCount());
//           	for(int i=0; i < matcher.groupCount(); i++){
//        		System.out.printf("group(%s): %s%n", i+1, matcher.group(i+1));
//        	}
            Y = matcher.group(3);	if (Y.length() == 2 || Y.length() == 3)Y = "20" + Y.substring(Y.length()-2, Y.length());
            M = matcher.group(2);	M = getTwoDigitNumber(M);
            D = matcher.group(1);	D = getTwoDigitNumber(D);
        	h = matcher.group(8);	h = getTwoDigitNumber(h);
        	m = matcher.group(10);	m = getTwoDigitNumber(m);
        	s = matcher.group(12);	s = getTwoDigitNumber(s);
            PM = matcher.group(14);	if (PM==null) PM="";
            if (matcher.group(7) == null){
            	timestamp = Y + "-" + M + "-" + D;
//            	System.out.printf("timestamp: %s%n", timestamp);
            	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(timestamp);
                return date.getTime();
            }else{
            	SimpleDateFormat dateFormat;
            	String dateFormatStr;
            	String pmFormatter = (PM.equals("")) ? "" : " a";
            	String hhFormatter = (PM.equals("")) ? "HH" : "hh";
            	if (s == null){
            		timestamp = (Y + "-" + M + "-" + D + " " + h + ":" + m + " " + PM).trim();
            		dateFormatStr = "yyyy-MM-dd "+hhFormatter+":mm"		+	pmFormatter;
	            	dateFormat = new SimpleDateFormat(dateFormatStr, Locale.getDefault());// HH:mm:ss");
            	}else{
            		timestamp = (Y + "-" + M + "-" + D + " " + h + ":" + m + ":" + s + " " + PM).trim();
            		dateFormatStr = "yyyy-MM-dd "+hhFormatter+":mm:ss"	+	pmFormatter;
            		dateFormat = new SimpleDateFormat(dateFormatStr, Locale.getDefault());// HH:mm:ss");
            	}
            	
//            	System.out.printf("timestamp: %s%n", timestamp);
//            	System.out.printf("dateFormat: %s%n", dateFormatStr);
                Date date = dateFormat.parse(timestamp);
                return date.getTime();
            }
        }
        return -1;
	}
	private static String getTwoDigitNumber(String str){
		if (str==null)return null;
		return (str.length() == 2) ? str : "0" + str;
	}
}
