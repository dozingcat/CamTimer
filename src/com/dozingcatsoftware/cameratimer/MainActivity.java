package com.dozingcatsoftware.cameratimer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.dozingcatsoftware.cameratimer.R;
import com.dozingcatsoftware.cameratimer.AboutActivity;

import com.dozingcatsoftware.util.ARManager;
import com.dozingcatsoftware.util.AndroidUtils;
import com.dozingcatsoftware.util.CameraUtils;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera;

public class MainActivity extends Activity implements Camera.PictureCallback, Camera.AutoFocusCallback {
	
	ARManager arManager;
	SurfaceView cameraView;
	
	Button pictureNowButton, picture5SecondsButton, picture15SecondsButton, picture30SecondsButton;
	Button cancelPictureButton;
	Button switchCameraButton;
	Button flashButton;
	Button helpButton;
	Button numberOfPicturesButton;
	TextView statusTextField;
	
	// only one beep type for now
	//int beepType;
	//int numBeepTypes = 3;
	//Random RAND = new Random();
	
	// assign ID when we start a timed picture, used in makeDecrementTimerFunction callback. If the ID changes, the countdown will stop.
	int currentPictureID = 0;
	
	// PictureView works, but for timed pictures it makes more sense to always go to ViewImageActivity
	// since the user may not be by the camera when it takes the picture
	//PictureView pictureView;
	Uri pictureURI;
	
	Handler handler = new Handler();
	int pictureTimer = 0;
	boolean hasMultipleCameras;
	
	List<String> flashModes = new ArrayList<String>();
	int selectedFlashMode;
	boolean flashButtonConfigured = false;
	
	int picturesToTake = 1;
	List<Uri> pictureURIs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.main);
    	
    	cameraView = (SurfaceView)findViewById(R.id.cameraView);
    	arManager = ARManager.createAndSetupCameraView(this, cameraView, null);
    	arManager.setCameraOpenedCallback(new Runnable() {public void run() {cameraOpened();}});
    	arManager.setCameraStartedCallback(new Runnable() {public void run() {cameraPreviewStarted();}});
    	
    	pictureNowButton = (Button)findViewById(R.id.savePictureNowButton);
    	picture5SecondsButton = (Button)findViewById(R.id.savePicture5SecondsButton);
    	picture15SecondsButton = (Button)findViewById(R.id.savePicture15SecondsButton);
    	picture30SecondsButton = (Button)findViewById(R.id.savePicture30SecondsButton);
    	
    	cancelPictureButton = (Button)findViewById(R.id.cancelPictureButton);
    	flashButton = (Button)findViewById(R.id.flashButton);
    	helpButton = (Button)findViewById(R.id.helpButton);
    	numberOfPicturesButton = (Button)findViewById(R.id.numberOfPicturesButton);
    	
    	switchCameraButton = (Button)findViewById(R.id.switchCameraButton);
    	hasMultipleCameras = (CameraUtils.numberOfCameras() > 1);
    	switchCameraButton.setVisibility(hasMultipleCameras ? View.VISIBLE : View.GONE);
    	
    	statusTextField = (TextView)findViewById(R.id.statusText);
    	
    	//pictureView = (PictureView)findViewById(R.id.pictureView);
    	
    	AndroidUtils.bindOnClickListener(this, pictureNowButton, "savePictureNow");
    	AndroidUtils.bindOnClickListener(this, picture5SecondsButton, "savePicture5Seconds");
    	AndroidUtils.bindOnClickListener(this, picture15SecondsButton, "savePicture15Seconds");
    	AndroidUtils.bindOnClickListener(this, picture30SecondsButton, "savePicture30Seconds");
    	AndroidUtils.bindOnClickListener(this, cancelPictureButton, "cancelSavePicture");
    	AndroidUtils.bindOnClickListener(this, switchCameraButton, "switchCamera");
    	AndroidUtils.bindOnClickListener(this, flashButton, "cycleFlashMode");
    	AndroidUtils.bindOnClickListener(this, helpButton, "doHelp");
    	AndroidUtils.bindOnClickListener(this, numberOfPicturesButton, "toggleNumberOfPictures");
    	
    	this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	/*
    	pictureView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction()==MotionEvent.ACTION_DOWN && pictureView.isPointInCornerImage(event.getX(), event.getY())) {
					showPicture();
				}
				return true;
			}
    	});
    	*/
    }

    @Override
    public void onPause() {
    	if (pictureTimer > 0) {
    		this.cancelSavePicture();
    	}
    	arManager.stopCamera();
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	arManager.startCameraIfVisible();
    }
    
    // callback from ARManager
    public void cameraOpened() {
    	arManager.setPreferredPreviewSize(cameraView.getWidth(), cameraView.getHeight());
    	CameraUtils.setLargestCameraSize(arManager.getCamera());
    	//statusTextField.setText(arManager.getCamera().getParameters().getPictureSize().width+"");
    	if (!flashButtonConfigured) {
    		configureFlashButton();
    		flashButtonConfigured = true;
    	}
    }
    
    public void cameraPreviewStarted() {
    	// resize camera view to actual size of preview image (TODO: scale if needed)
    	Camera.Size size = arManager.getCamera().getParameters().getPreviewSize();
    	cameraView.setLayoutParams(new FrameLayout.LayoutParams(size.width, size.height, Gravity.CENTER));
    }
    
    void updateButtons(boolean allowSave) {
    	this.findViewById(R.id.pictureButtonBar).setVisibility(allowSave ? View.VISIBLE : View.GONE);
    	this.findViewById(R.id.optionsButtonBar).setVisibility(allowSave ? View.VISIBLE : View.GONE);
		cancelPictureButton.setVisibility(allowSave ? View.GONE : View.VISIBLE);
    }
    
    public void cancelSavePicture() {
    	pictureTimer = 0;
    	++currentPictureID;
    	statusTextField.setText("");
    	Toast.makeText(this, "Canceled picture", Toast.LENGTH_SHORT).show();
    	updateButtons(true);
    }
    
    void updateTimerMessage() {
    	statusTextField.setText("Taking picture in " + pictureTimer);
    }
    
    Runnable makeDecrementTimerFunction(final int pictureID) {
    	return new Runnable() {
    		public void run() {decrementTimer(pictureID);}
    	};
    }
    
    MediaPlayer.OnCompletionListener releaseMediaPlayerFunction = new MediaPlayer.OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			mp.release();
		}
	};
    
    void playTimerBeep() {
    	int soundResource = R.raw.beep_sound0;
    	MediaPlayer mp = MediaPlayer.create(this, soundResource);
    	mp.start();
    	mp.setOnCompletionListener(releaseMediaPlayerFunction);
    }
    
    public void decrementTimer(final int pictureID) {
    	if (pictureID!=this.currentPictureID) {
    		return;
    	}
    	boolean takePicture = (pictureTimer==1);
    	--pictureTimer;
    	if (takePicture) {
    		savePictureNow();
    		playTimerBeep();
    	}
    	else if (pictureTimer>0) {
    		updateTimerMessage();
    		handler.postDelayed(makeDecrementTimerFunction(pictureID), 1000);
    		if (pictureTimer<3) playTimerBeep();
    	}
    }
    
    void savePictureAfterDelay(int delay) {
    	pictureTimer = delay;
    	updateTimerMessage();
    	currentPictureID++;
		handler.postDelayed(makeDecrementTimerFunction(currentPictureID), 1000);
		//beepType = RAND.nextInt(numBeepTypes);
		
		updateButtons(false);
    }
    
    public void savePicture5Seconds() {
    	savePictureAfterDelay(5);
    }
    
    public void savePicture15Seconds() {
    	savePictureAfterDelay(15);
    }
    
    public void savePicture30Seconds() {
    	savePictureAfterDelay(30);
    }
    
    public void savePictureNow() {
    	pictureURIs = new ArrayList<Uri>();
		statusTextField.setText("Taking picture...");
		arManager.getCamera().autoFocus(this);
    }
    
    public void switchCamera() {
    	flashButtonConfigured = false;
    	arManager.switchToNextCamera();
    }
    
    void configureFlashButton() {
    	flashModes.clear();
    	if (CameraUtils.cameraSupportsFlash(arManager.getCamera())) {
    		if (CameraUtils.cameraSupportsAutoFlash(arManager.getCamera())) {
    			flashModes.add("auto");
    		}
    		flashModes.add("off");
    		flashModes.add("on");
    	}
    	
    	if (flashModes.size() > 0) {
    		flashButton.setVisibility(View.VISIBLE);
    		updateFlashMode(0);
    		String mode = flashModes.get(selectedFlashMode);
    		flashButton.setText("Flash: " + mode.substring(0,1).toUpperCase() + mode.substring(1));
    		CameraUtils.setFlashMode(arManager.getCamera(), mode);
    	}
    	else {
    		flashButton.setVisibility(View.GONE);
    	}
    }
    
    public void cycleFlashMode() {
    	if (flashModes.size() > 0) {
    		selectedFlashMode = (selectedFlashMode + 1) % flashModes.size();
    		updateFlashMode(selectedFlashMode);
    	}
    }
    
    void updateFlashMode(int mode) {
    	selectedFlashMode = mode;
    	String modeString = flashModes.get(selectedFlashMode);
		flashButton.setText("Flash: " + modeString.substring(0,1).toUpperCase() + modeString.substring(1));
		CameraUtils.setFlashMode(arManager.getCamera(), modeString);
    }
    
    public void toggleNumberOfPictures() {
    	picturesToTake = (picturesToTake==1) ? 4 : 1;
    	numberOfPicturesButton.setText(picturesToTake==1 ? R.string.singleImageButtonLabel : R.string.multiImageButtonLabel);
    }
    
    public void doHelp() {
    	AboutActivity.startIntent(this);
    }
    
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		camera.takePicture(null, null, this);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		int pictureNum = (picturesToTake > 1) ? pictureURIs.size() + 1 : 0;
		pictureURI = saveImageData(data, pictureNum);
		statusTextField.setText("");
		updateButtons(true);
		camera.startPreview();
		
		if (pictureURI!=null) {
			pictureURIs.add(pictureURI);
			if (pictureURIs.size() >= picturesToTake) {
				if (picturesToTake==1) {
					ViewImageActivity.startActivityWithImageURI(this, pictureURI, "image/jpeg");
				}
				else {
					// todo: start image grid activity with pictureURIs
					ViewImageGridActivity.startActivityWithImageURIs(this, pictureURIs);
				}
			}
			else {
				camera.takePicture(null, null, this);
			}
		}
	}
	
	String savedImageDirectory = Environment.getExternalStorageDirectory() + File.separator + "CamTimer";
	Format dateInFilename = new SimpleDateFormat("yyyyMMDD_HHmmss");
	
	Uri saveImageData(byte[] data, int pictureNum) {
		try {
    		File dir = new File(savedImageDirectory);
    		if (!dir.exists()) {
   				dir.mkdirs();
    		}
    		if (!dir.isDirectory()) {
    			Toast.makeText(this, "Error saving picture: can't create directory " + dir.getPath(), Toast.LENGTH_LONG).show();
    			return null;
    		}
			String filename = String.format("IMG_" + dateInFilename.format(new Date()));
			if (pictureNum > 0) filename += ("-" + pictureNum);
			filename += ".jpg";
			
			String path = savedImageDirectory + File.separator + filename;
			FileOutputStream out = new FileOutputStream(path);
			out.write(data);
			out.close();
			
			AndroidUtils.scanSavedMediaFile(this, path);
			String toastMessage = "Saved picture";
			Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
			
			return Uri.fromFile(new File(path));
		}
		catch(Exception ex) {
			Toast.makeText(this, "Error saving picture: " + ex.getClass().getName(), Toast.LENGTH_LONG).show();
			return null;
		}
	}
/*
	public void openLibrary(View view) {
		startActivity(LibraryActivity.intentWithImageDirectory(this, savedImageDirectory));
	}
	*/
}