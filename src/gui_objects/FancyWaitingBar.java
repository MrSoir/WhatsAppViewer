package gui_objects;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class FancyWaitingBar extends Group{
	
	private AtomicBoolean waitBarIsInterrupted = new AtomicBoolean(true);
	private Task<Integer> task;
	private double[] colors;
	private Rectangle[] rects;
	
	private static String state = "";
	Text stateLbl = new Text();
	AtomicBoolean stateHasChanged = new AtomicBoolean(false);
	
	private final int ANZRECTS;
	private double RECTWIDTH;
	private double RECTHEIGHT;
	private double ARCWIDTH = 0.5;
//	private final double RADIUS = 100d;
//	private final double XSTART = 200d;
//	private final double YSTART = 100d;
	
	public FancyWaitingBar(final int anzRects, final double rectWidth){
				
		ANZRECTS = anzRects;
		RECTWIDTH = rectWidth;
		RECTHEIGHT = RECTWIDTH;
		
		double degree = 0;
		rects = new Rectangle[ANZRECTS];
				
		for (int i=0; i < ANZRECTS; i++){
			rects[i] = new Rectangle();
			rects[i].setHeight(RECTHEIGHT);
			rects[i].setWidth(RECTWIDTH);
			rects[i].setLayoutX(i*RECTWIDTH);
//			degree = 360d/ANZRECTS *i;
//			rects[i].setLayoutX( XSTART + Math.cos(Math.toRadians(degree))*RADIUS );
//			rects[i].setLayoutY( YSTART + Math.sin(Math.toRadians(degree))*RADIUS );
			rects[i].setArcWidth(RECTWIDTH*ARCWIDTH);
			rects[i].setArcHeight(RECTWIDTH*ARCWIDTH);
			rects[i].setFill(new Color(0.5, 0.5, 0.99, 1.0));
			getChildren().add(rects[i]);
		}
		
		alignLabel();
		
		colors = new double[3];
		for (int i=0; i < colors.length; i++){
			colors[i] = 0d;
		}
		
		stateLbl.setFill(Color.WHITE);
		stateLbl.setFont(Font.font("Arial",FontWeight.BOLD, 12));
		getChildren().add(stateLbl);
	}
	
	private Integer taskMethod(double[] colors, Task<Integer> task){
		int iterations = 0;
            	
    	int colID = 1; 
    	    	
    	Color color = new Color(1.0, 0.0, 0.0, 1.0);
    	ColColPair refColPair = new ColColPair(color, colID);
    	
    	state = "0%";
    	
    	
        while(!waitBarIsInterrupted.get()){
        	
        	synchronized(state){
        		if (this.stateHasChanged.get()){
        			stateHasChanged.set(false);
		        	stateLbl.setText(state);
		    		alignLabel();
        		}
        	}
        	        	
        	ColColPair colColPair = new ColColPair(refColPair.getColor(), refColPair.getColID());
        	
        	double colOff = 0.02;
        	
        	for (int i=0; i < ANZRECTS; i++){
        		
        		rects[i].setFill(colColPair.getColor());
        		
        		colColPair = getColorII(colColPair.getColor(), colColPair.getColID(), colOff*2);
        			            		
        	}	            	
        	
        	refColPair = getColorII(refColPair.getColor(), refColPair.getColID(), colOff);
        	
        	try {
				Thread.sleep(33);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
        	
        	if (task.isCancelled()) {
            	break;
            }
        }
        return iterations;
	}
	
	public void setRectanglesHeight(double height){
		RECTHEIGHT = height;
		alignChilds();
	}

	public void setWidth(double width){
		this.RECTWIDTH = width / this.ANZRECTS;
		alignChilds();
	}
	public void setArcFactor(double fctr){
		ARCWIDTH = fctr;
		alignChilds();
	}
	private void alignChilds(){
		alignRects();
		alignLabel();
	}
	private void alignRects(){
		double xOffs = 0d;
		for(int i=0; i < rects.length; i++){
			double width = i == rects.length-1 ? RECTWIDTH : RECTWIDTH+1;
			rects[i].setWidth(width);
			rects[i].setHeight(RECTHEIGHT);
			rects[i].setArcWidth (RECTWIDTH * ARCWIDTH);
			rects[i].setArcHeight(RECTWIDTH * ARCWIDTH);
			rects[i].setLayoutX(xOffs);
			xOffs += RECTWIDTH;
		}
	}
	private void alignLabel(){
		if (stateLbl!=null){ // kein plan wieso aber hier hats mal ne Nullpointerexception gegeben...
			stateLbl.setLayoutX( (ANZRECTS * RECTWIDTH - stateLbl.getBoundsInLocal().getWidth()) * 0.5);
			stateLbl.setLayoutY( (RECTHEIGHT - stateLbl.getBoundsInLocal().getHeight()) * 0.5 + stateLbl.getBaselineOffset());
		}
	}
	
	public void updateProgress(String state){
		synchronized(state){
			FancyWaitingBar.state = state;
			stateHasChanged.set(true);
		}
	}
	
	public void run(){
		if (!waitBarIsInterrupted.get()){
			interruptBar();
		}
		task = new Task<Integer>() {
		    @Override protected Integer call() throws Exception {
		    	waitBarIsInterrupted.set(false);
		    	return taskMethod(colors, this);
		    }
		};
		task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
			  if(newValue != null) {
			    Exception ex = (Exception) newValue;
			    ex.printStackTrace();
			  }
			});
		Thread t = new Thread(task);
		t.setDaemon(true);
		t.start();
	}
	public void interruptBar(){
		waitBarIsInterrupted.set(true);
//		if(task.isRunning()){
//			task.cancel();
//		}
	}
	
	private class ColColPair{
		private int colID;
		private Color color;
		private ColColPair(Color color, int colID){
			this.colID = colID;
			this.color = color;
		}
		private int getColID(){return colID;}
		private Color getColor(){return color;}
	}
	
	private ColColPair getColorII(Color color, int colID, double colOffs){
		int prevCol = getPrevCol(colID);
		int follCol = getFollowCol(colID);
		
//		System.out.printf("%nin getColorII - colID: %s - prevCol: %s - follCol: %s  -> colOffs: %s%n",
//				colID, prevCol, follCol, colOffs);
		
		double[] colors = new double[3];
		colors[0] = color.getRed();
		colors[1] = color.getGreen();
		colors[2] = color.getBlue();
				
		if (colors[colID] >= 1){
			colors[colID] = 1;
			
			if (colors[prevCol] > 0){
				colors[prevCol] -= colOffs;
				if (colors[prevCol] <= 0){
					colors[prevCol] = 0;
					colID = follCol;
				}
			}
		}
		else if (colors[colID] +colOffs > 1){
			colors[colID] = 1;
		}
		else{
			colors[colID] = colors[colID]+colOffs > 1 ? 1 : colors[colID]+colOffs;
		}
		
		if (colors[0] < 0){ colors[0] = 0;} else if (colors[0] > 1){colors[0] = 1;}
		if (colors[1] < 0){ colors[1] = 0;} else if (colors[1] > 1){colors[1] = 1;}
		if (colors[2] < 0){ colors[2] = 0;} else if (colors[2] > 1){colors[2] = 1;}
		
		return new ColColPair(new Color(colors[0], colors[1], colors[2], 1.0), colID);
	}
	
	private int getPrevCol(int curCol){
		return curCol-1 < 0 ? 2 : curCol-1;
	}
	private int getFollowCol(int curCol){
		return curCol+1 > 2 ? 0 : curCol+1;
	}
}