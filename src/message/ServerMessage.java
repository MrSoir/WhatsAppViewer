package message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static_methods.StaticMethods;

public class ServerMessage extends Message {
    public ServerMessage(String line) throws Exception {
        Matcher matcher = getServerPattern().matcher(line);   
        if (matcher.find()) {
            this.timestamp = StaticMethods.parse_date(matcher.group(1));
            this.actor = matcher.group(2);
//            this.actor = "";
            this.content = matcher.group(3)+matcher.group(4);
        } else {
            throw new Exception("Failed to parse servermessage");
        }
    }
    
    private static Pattern getServerPattern(){
//    	String cutDatePattern = StaticMethods.DATE_PATTERN.pattern().substring(1, StaticMethods.DATE_PATTERN.pattern().length()-1);
//    	return Pattern.compile(String.format(
//				"(?:%s)(.*)", cutDatePattern));
    	return Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}, \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)): (.+)( now an admin| changed to| created| added| joined| left|'s sec)(.*)");
    }
    
    public static boolean isServerevent(String line){
    	Pattern ATT_PATTERN = getServerPattern();
    	return ATT_PATTERN.matcher(line).find();
    }
}
