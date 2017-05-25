/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static_methods.StaticMethods;

public class TextMessage extends Message {
    public TextMessage(String line) throws Exception {
    	Pattern TEXT_MSG_PATTERN = getMessagePattern();
    	Matcher matcher = TEXT_MSG_PATTERN.matcher(line);
        if (matcher.find()) {
            this.timestamp = StaticMethods.parse_date(matcher.group(1));
            this.actor = matcher.group(2);
            this.content = matcher.group(3);
        }else{
            throw new Exception("Failed to parse textmessage");
        }
    }
    public TextMessage(long timestamp, String actor, String content){
    	this.timestamp = timestamp;
    	this.actor = actor;
    	this.content = content;
    }
    
    private static Pattern getMessagePattern(){
//    	String cutDatePattern = StaticMethods.DATE_PATTERN.pattern().substring(1, StaticMethods.DATE_PATTERN.pattern().length()-1);
//    	return Pattern.compile(String.format(
//    			"(%s)([\\s]*?(.)[\\s])(.*?)([:]{1}?)(\\s)*(.*)(\\s)*$", cutDatePattern));
    	return Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}, \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)): ([^:]+): ((?:.|\n)+)");
    }
    
    public static boolean isText(String line){
    	Pattern TEXT_MSG_PATTERN = getMessagePattern();
    	return TEXT_MSG_PATTERN.matcher(line).find();
    }
}

