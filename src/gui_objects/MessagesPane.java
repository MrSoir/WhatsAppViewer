package gui_objects;

import java.util.List;

import gui_objects.ScrollBarBD.BarListener;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BorderPane;
import message.Message;

public class MessagesPane extends BorderPane{
//	private List<Message> messages;
    private MessagesScrollPane scrPne;

    interface ScrBarValueSetter{
        void setValue(int val);
        void setValueOffset(int offs);
        double getValue();
        void setMaxValue(double max);
        public double getMax();
    }
    
    public void messagesHaveChanged(List<Message> newMessages){
    	if(scrPne != null){
    		scrPne.messagesHaveChanged(newMessages);
    	}
    }

    public MessagesPane(List<Message> messages, Scene scene){
        super();

//	this.messages = messages;
        
        

//        ScrollBar scrBr = new ScrollBar();
//        scrBr.setMin(0);
//        scrBr.setMax(messages.size());
//        scrBr.setOrientation(Orientation.VERTICAL);
        
        scrPne = new MessagesScrollPane(messages, scene);
        ScrollBarBD scrBr = new ScrollBarBD(scrPne.getBarListener());


//        scrBr.valueProperty().addListener(t->{ scrPne.setVValueBD((int)scrBr.getValue()); });

        this.setOnMouseEntered(t->{
        	Platform.runLater(new Runnable() {
                @Override
                public void run() { MessagesPane.this.requestFocus(); }
            });
        });
        this.setOnMouseMoved(t->{
            Platform.runLater(new Runnable() {
                @Override
                public void run() { MessagesPane.this.requestFocus(); }
            });
        });

        this.setCenter(scrPne);
        this.setRight(scrBr);
    }

    public void setSearchResults(List<Integer> treffer){ scrPne.setSearchResults(treffer); }
    public void focusNextSearchResult(){ scrPne.focusNextSearchResult(); }
    public void focusPrevSearchResult(){ scrPne.focusPrevSearchResult(); }
    public void close(){
//	messages = null;
        scrPne.close();
        scrPne = null;
        this.getChildren().clear();
    }
}
