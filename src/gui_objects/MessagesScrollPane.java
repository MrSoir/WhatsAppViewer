package gui_objects;

import java.util.ArrayList;
import java.util.List;

import gui_objects.MessagesPane.ScrBarValueSetter;
import gui_objects.ScrollBarBD.BarListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import message.Message;

public class MessagesScrollPane extends Pane {
    private List<Message> messages;
    private List<MessageTD> curDipsMsgs = new ArrayList<>();
    
    private List<Integer> searchedMsgs = new ArrayList<>();
    private int curSearchIndx = 0;

    private List<Dim> messageDims = new ArrayList<>();

    private int firstDispMsgIndx = 0;

    private Scene scene;
    private Group scrPneGrp = new Group();

    private double lastMouseDragPosY = -1;
    
    private double maxWidth, maxHeight;
    
    double getMaxHeightBD(){return maxHeight;}
    
    private int insets = 10;
    
    private int curFrstDispMsg = 0;
    private int curLastDispMsg = 1;
    
    private int curLayoutY = 0;

//    private ScrBarValueSetter scrBarValSetter;
    private BarListener barListener;

    private class Dim{
        private double width, height, layoutY;
        private Dim(){}
        private Dim(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    private int backgrImgId = 1;
    private ImageView backgrImgVw;
    
    void messagesHaveChanged(List<Message> newMessages){
    	this.messages = newMessages;
//    	evaluateDimensions();

    	if (messages != null && messages.size() > 0){
    		render(messages.get(messages.size()-1));
    		render(10d, false);
    	}else{
    		this.closeCurrDispMsgs();
    		this.scrPneGrp.getChildren().clear();
    	}
    }
    
    protected BarListener getBarListener(){return barListener;}
    
    public MessagesScrollPane(List<Message> messages, final Scene scene){
    	super();
        
        clipChildren(this);
        
        this.setBackground(new Background(new BackgroundFill(Color.web("#FFFFFF"), CornerRadii.EMPTY, Insets.EMPTY)));
        this.messages = messages;
        this.scene = scene;
        
        final double maxLayYOffs = 80;
        barListener = (scrBarVal)->{
        	if (scrPneGrp != null){
	    		double layYOffs = maxLayYOffs * -scrBarVal; // scrBarVal is in percentage measures
	    		render(layYOffs, false);
        	}
        };
        
//        evaluateDimensions();

        this.setOnScroll(t->{
        	render( (int)t.getDeltaY(), false ); 
    	});
        
        this.setOnMousePressed(t->{ lastMouseDragPosY = t.getSceneY(); });
        this.setOnMouseDragged(t->{
            double sceneY = t.getSceneY();
            double diff = sceneY - lastMouseDragPosY;
//            scrBarValSetter.setValueOffset( -(int)diff );
            render(diff, false);
            lastMouseDragPosY = sceneY;
        });
        this.setOnMouseExited(t->{ scene.setCursor(Cursor.DEFAULT); });

        backgrImgVw = new ImageView(new Image("file:pics/backgrnd1.jpg"));
        this.widthProperty().addListener(t->{ properBackgroundID();render(0d, true); });
        this.heightProperty().addListener(t->{ properBackgroundID();render(0d, true);  });
        
        this.heightProperty().addListener(t->{ render(); });
        this.widthProperty().addListener(t->{ render(); });
        
        this.getChildren().addAll( this.backgrImgVw, scrPneGrp );
        
        render();
    }

    private void properBackgroundID(){
    	if (scene != null && backgrImgVw != null){
	        int xID = 0, yID = 0;
	        
	        if (scene.getWidth() > 1800) xID = 3;
	        else if (scene.getWidth() > 1200) xID = 2;
	        else xID = 1;
	        
	        if (scene.getHeight() > 1490) yID = 3;
	        else if (scene.getHeight() > 990) yID = 2;
	        else yID = 1;
	
	        int id = Math.max(xID, yID);
	
	        if (backgrImgId != id){
	            backgrImgId = id;
	            backgrImgVw.setImage(new Image("file:pics/backgrnd" + id + ".jpg"));
	        }
        }
    }
    
    public static void clipChildren(Region region) {
        final Rectangle outputClip = new Rectangle();
        region.setClip(outputClip);

        region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });        
    }
    
    private final double yPuffer = 2000d;
    
    private void render(double yScrollOffset, boolean repaintAnyway){
    	
    	if(messages != null && messages.size() > 0 && scrPneGrp != null ){
    		
    		double newLayY =  scrPneGrp.getLayoutY() + scrPneGrp.getBoundsInLocal().getMinY() + yScrollOffset;
    		
    		if ( newLayY > 0 && curFrstDispMsg == 0){
    			scrPneGrp.setLayoutY(0 - scrPneGrp.getBoundsInLocal().getMinY());
    		}
    		else if (yScrollOffset < 0 && curLastDispMsg == messages.size()-1 && 
    				scrPneGrp.getLayoutY() + scrPneGrp.getBoundsInLocal().getMaxY() <= this.getHeight()){
    			scrPneGrp.setLayoutY( -scrPneGrp.getBoundsInLocal().getMaxY() + this.getHeight() );
    		}else{
    			scrPneGrp.setLayoutY( scrPneGrp.getLayoutY() + yScrollOffset );
    		}
    		
	    	if ( repaintAnyway ||
	    			scrPneGrp.getLayoutY() + scrPneGrp.getBoundsInLocal().getMinY() > 0 && curFrstDispMsg > 0 || 
	    		scrPneGrp.getLayoutY() + scrPneGrp.getBoundsInLocal().getMaxY() <= this.getHeight() && curLastDispMsg < messages.size()-1  ){
	    		
	    		Message firstMessage = null;
	    		double firstMessageY = 0d;
	    			    		
	    		for(int i=0; i < scrPneGrp.getChildren().size(); i++){
	    			MessageTD curMsgTD = ((MessageTD)scrPneGrp.getChildren().get(i));
	    			if (scrPneGrp.getLayoutY() + curMsgTD.getLayoutY() >= 0){
	    				firstMessage = curMsgTD.getMessage();
	    				firstMessageY = scrPneGrp.getLayoutY() + curMsgTD.getLayoutY();
	    				break;
	    			}
	    		}
	    			    		
	    		closeCurrDispMsgs();
	    		scrPneGrp.getChildren().clear();
	    		scrPneGrp.setLayoutY(0d);
	    		
	    		if (firstMessage == null){
	    			firstMessage = messages.get(0);
	    			firstMessageY = 0d;
	    		}
	    		
	    		render(firstMessage, firstMessageY);
	    	}
    	}
    }
    
    private void render(Message msg){
    	closeCurrDispMsgs();
    	scrPneGrp.getChildren().clear();
    	scrPneGrp.setLayoutY(0);
    	render(msg, 0d);
    }
    
    private void render(Message msg, double firstMsgLayY){
    	if(messages != null && messages.size() > 0 && scrPneGrp != null ){
	    	double width = this.getWidth() - 30;
	    	
	    	int firstMsgId = messages.indexOf(msg);

	    	if (firstMsgId == -1){firstMsgId = 0;}
	    	
	    	curFrstDispMsg = firstMsgId;
	    	
			double yOffs = firstMsgLayY;
			for(int i=firstMsgId; i < messages.size(); i++){
				MessageTD msgTD = new MessageTD(messages.get(i), cursor->{
		                scene.setCursor(cursor);
		        }, width);
	    		
		        msgTD.setLayoutX(insets);
		        msgTD.setLayoutY(yOffs);
		        
		        scrPneGrp.getChildren().add(msgTD);
		        	        
		        yOffs += msgTD.getBoundsInLocal().getHeight() + insets;
		        
	        	curLastDispMsg = i;
	        	
		        if (yOffs > this.getHeight() + yPuffer){
		        	break;
		        }
			}
			yOffs = firstMsgLayY - insets;
			for(int i=firstMsgId-1; i >= 0; i--){
				MessageTD msgTD = new MessageTD(messages.get(i), cursor->{
		                scene.setCursor(cursor);
		        }, width);
	    		
		        msgTD.setLayoutX(insets);
		        msgTD.setLayoutY(yOffs - msgTD.getBoundsInLocal().getHeight());
		        
		        scrPneGrp.getChildren().add(0, msgTD);
		        	        
		        yOffs -= msgTD.getBoundsInLocal().getHeight() + insets;
		        
	        	curFrstDispMsg = i;
	        	
		        if (yOffs < firstMsgLayY - yPuffer){
		        	break;
		        }
			}
    	}
    }
    
    private void render(){
    	render(0d, false);
    }

    public void setVValueBD(int layYPos){
    	curLayoutY = layYPos;
    	render();
    }

    private void closeCurrDispMsgs(){
    	if (scrPneGrp != null){
	    	for(int i=0; i < scrPneGrp.getChildren().size(); i++){
	    		((MessageTD)scrPneGrp.getChildren().get(i)).close();
	    	}
    	}
    }

    public void setSearchResults(List<Integer> treffer){
        this.searchedMsgs = treffer;
        curSearchIndx = 0;
        focusSearch();
    }

    public void focusNextSearchResult(){
        if (searchedMsgs != null && searchedMsgs.size() > 0){
            if (++curSearchIndx >= searchedMsgs.size()) curSearchIndx = 0;
            focusSearch();
        }
    }
    public void focusPrevSearchResult(){
        if (searchedMsgs != null && searchedMsgs.size() > 0){
                if (--curSearchIndx < 0) curSearchIndx = searchedMsgs.size() -1;
                focusSearch();
        }
    }
    private void focusSearch(){
        if (messages != null && searchedMsgs != null &&
        		curSearchIndx >= 0 
                && curSearchIndx < searchedMsgs.size()
                && searchedMsgs.get(curSearchIndx) >= 0
                && searchedMsgs.get(curSearchIndx) < messages.size()) {        	
        	render(messages.get( searchedMsgs.get(curSearchIndx)) );
        }
    }

    public void close() {
        messageDims = null;
        messages = null;
        curDipsMsgs = null;
        searchedMsgs = null;
        closeCurrDispMsgs();
        scrPneGrp.getChildren().clear();
        scene = null;
//        scrBarValSetter = null;
    }
}
