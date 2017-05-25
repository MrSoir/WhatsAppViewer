package gui_objects;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ScrollBarBD extends Pane {
	private Rectangle circ = new Rectangle();
	private double mouseOffsY = 0d;
	private boolean circIsDragged = true;
	
	private TaskBD task;
	
	public interface BarListener{
		public void setValue(double value);
	}
		
	private DoubleProperty value = new SimpleDoubleProperty(this, "value", 0d);
	public DoubleProperty valueProperty(){return value;}
	public void setValue(double value){
		if (this.value.get() != value){
			this.value.set(value);
		}
	}
	public double getValue(){return value.get();}
	
	private static class TaskBD extends Task<Double>{
		final private AtomicBoolean interrupted = new AtomicBoolean(false);
		final private BarListener listener;
		final private DoubleProperty value;
		
		TaskBD(BarListener listener, DoubleProperty value){
			this.listener = listener;
			this.value = value;
		}
		
		@Override
		protected Double call() throws Exception {
			while(!interrupted.get()){
				Platform.runLater(() -> listener.setValue(value.get()) );
				Thread.sleep(30);
			}
			return null;
		}
	}
	
	private void startDragging(BarListener listener, double mouseY){
		circIsDragged = true;
		mouseOffsY = mouseY - circ.getLayoutY();
		task = new TaskBD(listener, value);
		task.setOnCancelled(e->{
		});
		task.setOnFailed(e->{
		});
		Thread thrd = new Thread(task);
		thrd.setDaemon(true);
		thrd.start();
	}
	private void onDragged(double mouseY){
		double layY = mouseY - mouseOffsY;
		if (layY < 0)
			layY = 0;
		if (layY > this.getHeight()-circ.getHeight()){
			layY = this.getHeight()-circ.getHeight();
		}
		
		circ.setLayoutY(layY);
		
		double value = ((circ.getLayoutY()+circ.getHeight() * 0.5) - this.getHeight() * 0.5) / (this.getHeight() * 0.5);
		this.value.set(value);
	}
	private void onReleased(){
		if (task != null){
			task.interrupted.set(true);
		}
		if (circIsDragged){
			if (value.get() != 0d){
				value.set(0d);
			}
			
			circIsDragged = false;
			
			final Timeline timeline = new Timeline();
//			timeline.setCycleCount(Timeline.INDEFINITE);
//			timeline.setAutoReverse(true);
			final KeyValue kv = new KeyValue(circ.layoutYProperty(), getCenterY());
			final KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
			timeline.getKeyFrames().add(kf);
			timeline.play();
		}
	}
	public ScrollBarBD(BarListener listener){
		super();
		clipChildren(this);
		
        Rectangle backgrdRect = new Rectangle();
        backgrdRect.setLayoutY(0);
        backgrdRect.setLayoutX(0);
        backgrdRect.widthProperty().bind(this.widthProperty());
        backgrdRect.heightProperty().bind(this.heightProperty());
        Stop[] stops = new Stop[] { new Stop(0, Color.LIGHTGRAY), new Stop(1, Color.WHITE)};//new Color(0.8,0.8,1.0, 1.0))};
        LinearGradient lg = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
        backgrdRect.setFill(lg);
        this.getChildren().add(backgrdRect);
        
        backgrdRect.setOnMousePressed(t->{
//        	System.out.printf("getY:			%s%n", t.getY());
//        	System.out.printf("sceneY:			%s%n", t.getSceneY());
//        	System.out.printf("screenY:			%s%n%n", t.getScreenY());
//        	double circY = t.getY() - circ.getHeight()*0.5;
//        	if (circY < 0)
//        		circY = 0;
//        	if (circY > this.getHeight()-circ.getHeight()){
//        		circY = this.getHeight()-circ.getHeight();
//        	}
//        	circ.setLayoutY(circY);
//        	startDragging(listener, 0d);
        });
        backgrdRect.setOnMouseDragged(t->{
//        	onDragged(t.getY());
        });
        backgrdRect.setOnMouseReleased(t->{
//        	onReleased();
        });
		
		circ.setWidth(20);
		circ.setHeight(20);
		circ.arcHeightProperty().bind(circ.heightProperty().multiply(0.5));
		circ.arcWidthProperty().bind(circ.widthProperty().multiply(0.5));
		this.widthProperty().addListener(t->{
			circ.setWidth(this.getWidth());
		});

		this.heightProperty().addListener(t->{
			circ.setLayoutY( getCenterY() );
		});
		circ.setOnMousePressed(t->{
			startDragging(listener, t.getSceneY());
		});
		circ.setOnMouseDragged(t->{
			onDragged(t.getSceneY());
		});
		circ.setOnMouseReleased(t->{
			onReleased();
		});
		
		Rectangle centLine = new Rectangle();
		centLine.widthProperty().bind(this.widthProperty());
		centLine.setHeight(3);
		centLine.setFill(Color.DARKGRAY);
		centLine.layoutYProperty().bind( this.heightProperty().subtract(centLine.getHeight()).multiply(0.5) ); 
		
		this.getChildren().addAll(centLine, circ);
		circ.setFill(new Color(0.4,0.4,1.0, 1.0));
	}
	
	private double getCenterY(){
		return (this.getHeight() - circ.getWidth())*0.5;
	}
	
    private void clipChildren(Region region) {
        final Rectangle outputClip = new Rectangle();
        region.setClip(outputClip);

        region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });        
    }
}
