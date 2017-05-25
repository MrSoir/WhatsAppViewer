package message;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static_methods.StaticMethods;

public abstract class Message {
    protected String actor;
    protected long timestamp;
    protected String content;
    
    public String get_actor() { return this.actor; }
    public long get_timestamp() { return this.timestamp; }
    public String get_content() { return this.content; }
    
    public void append_message(String line) {
        this.content += line;
    }
    
    public static boolean isText(String line) {
        return TextMessage.isText(line) && !isAttachment(line);
    }
     
    public static boolean isServerevent(String line) {
    	return ServerMessage.isServerevent(line);
    }
    
    public static boolean isAttachment(String line) {
        return AttachmentMessage.isAttachment(line);
    }
}
