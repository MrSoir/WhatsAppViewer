package message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static_methods.StaticMethods;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttachmentMessage extends Message {
//	private String fileName;
    public AttachmentMessage(String line) throws Exception {
    	Pattern ATT_PATTERN = getAttachmentPattern();      
    	Matcher matcher = ATT_PATTERN.matcher(line);   
        if (matcher.find()) {
            this.timestamp = StaticMethods.parse_date(matcher.group(1));
            this.actor = matcher.group(2);
//            this.fileName = matcher.group(21);
            this.content = matcher.group(3);
        } else {
            throw new Exception("Failed to parse attachmentmessage");
        }
    }
//    public String getFileName(){return fileName;}
    
    private static Pattern getAttachmentPattern(){
//    	String cutDatePattern = StaticMethods.DATE_PATTERN.pattern().substring(1, StaticMethods.DATE_PATTERN.pattern().length()-1);
//    	return Pattern.compile(String.format(
//    			"(%s)([\\s]*(.)[\\s])(.*)([:])(\\s)*(.*)(\\s)*(<attached>|[(]Datei angehängt[)])$", cutDatePattern));
    	return Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}, \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)): ([^:]+): (.+) (?:<attached>)");
    }
    
    public static boolean isAttachment(String line){
    	Pattern ATT_PATTERN = getAttachmentPattern();
    	return ATT_PATTERN.matcher(line).find();
    }
}