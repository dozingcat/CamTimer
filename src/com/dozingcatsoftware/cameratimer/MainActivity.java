package com.dozingcatsoftware.cameratimer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dozingcatsoftware.cameratimer.R;
import com.dozingcatsoftware.cameratimer.AboutActivity;

import com.dozingcatsoftware.util.ARManager;
import com.dozingcatsoftware.util.AndroidUtils;
import com.dozingcatsoftware.util.CameraUtils;
import com.dozingcatsoftware.util.ShutterButton;
import com.dozingcatsoftware.util.ShutterButton.OnShutterButtonListener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Camera;

public class MainActivity extends Activity implements Camera.PictureCallback, Camera.AutoFocusCallback, OnShutterButtonListener {
	
	static final List<Integer> DELAY_DURATIONS = Arrays.asList(0, 5, 15, 30);
	static final int DEFAULT_DELAY = 5;
	static final String DELAY_PREFERENCES_KEY = "delay";
	int pictureDelay = DEFAULT_DELAY;
	
	static final String FLASH_MODE_AUTO = "auto";
	static final String FLASH_MODE_ON = "on";
	static final String FLASH_MODE_OFF = "off";
	
	Map<String, String> flashButtonLabels = new HashMap<String, String>();
	
	ARManager arManager;
	SurfaceView cameraView;
	int[] maxCameraViewSize;
	
	ShutterButton shutterButton;
	Button pictureDelayButton;
	Button cancelPictureButton;
	Button switchCameraButton;
	Button flashButton;
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
    	
		flashButtonLabels.put(FLASH_MODE_AUTO, getString(R.string.flashButtonAutoLabel));
		flashButtonLabels.put(FLASH_MODE_ON, getString(R.string.flashButtonOnLabel));
		flashButtonLabels.put(FLASH_MODE_OFF, getString(R.string.flashButtonOffLabel));
    	
    	cameraView = (SurfaceView)findViewById(R.id.cameraView);
    	arManager = ARManager.createAndSetupCameraView(this, cameraView, null);
    	arManager.setCameraOpenedCallback(new Runnable() {public void run() {cameraOpened();}});
    	arManager.setCameraStartedCallback(new Runnable() {public void run() {cameraPreviewStarted();}});
    	
    	shutterButton = (ShutterButton)findViewById(R.id.shutterButton);
    	shutterButton.setOnShutterButtonListener(this);
    	pictureDelayButton = (Button)findViewById(R.id.pictureDelayButton);
    	
    	cancelPictureButton = (Button)findViewById(R.id.cancelPictureButton);
    	flashButton = (Button)findViewById(R.id.flashButton);
    	numberOfPicturesButton = (Button)findViewById(R.id.numberOfPicturesButton);
    	
    	switchCameraButton = (Button)findViewById(R.id.switchCameraButton);
    	hasMultipleCameras = (CameraUtils.numberOfCameras() > 1);
    	switchCameraButton.setVisibility(hasMultipleCameras ? View.VISIBLE : View.GONE);
    	
    	statusTextField = (TextView)findViewById(R.id.statusText);
    	
    	AndroidUtils.bindOnClickListener(this, pictureDelayButton, "cycleDelay");
    	AndroidUtils.bindOnClickListener(this, cancelPictureButton, "cancelSavePicture");
    	AndroidUtils.bindOnClickListener(this, switchCameraButton, "switchCamera");
    	AndroidUtils.bindOnClickListener(this, flashButton, "cycleFlashMode");
    	AndroidUtils.bindOnClickListener(this, numberOfPicturesButton, "toggleNumberOfPictures");
    	AndroidUtils.bindOnClickListener(this, findViewById(R.id.helpButton), "doHelp");
    	AndroidUtils.bindOnClickListener(this, findViewById(R.id.libraryButton), "openLibrary");
    	
    	this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	this.readDelayPreference();
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
    	AndroidUtils.setSystemUiLowProfile(cameraView);
    }
    
    // callback from ARManager
    public void cameraOpened() {
    	if (maxCameraViewSize==null) {
    		maxCameraViewSize = new int[] {cameraView.getWidth(), cameraView.getHeight()};
    	}
    	arManager.setPreferredPreviewSize(maxCameraViewSize[0], maxCameraViewSize[1]);
    	CameraUtils.setLargestCameraSize(arManager.getCamera());
    	//statusTextField.setText(arManager.getCamera().getParameters().getPictureSize().width+"");
    	if (!flashButtonConfigured) {
    		configureFlashButton();
    		flashButtonConfigured = true;
    	}
    }
    
    public void cameraPreviewStarted() {
    	// resize camera view to scaled size of preview image
    	Camera.Size size = arManager.getCamera().getParameters().getPreviewSize();
    	int[] scaledWH = AndroidUtils.scaledWidthAndHeightToMaximum(
    			size.width, size.height, maxCameraViewSize[0], maxCameraViewSize[1]);
    	cameraView.setLayoutParams(new FrameLayout.LayoutParams(scaledWH[0], scaledWH[1], Gravity.CENTER));
    }
    
    void updateButtons(boolean allowSave) {
    	this.findViewById(R.id.miscButtonBar).setVisibility(allowSave ? View.VISIBLE : View.GONE);
    	this.findViewById(R.id.optionsButtonBar).setVisibility(allowSave ? View.VISIBLE : View.GONE);
    	shutterButton.setVisibility(allowSave ? View.VISIBLE : View.GONE);
		cancelPictureButton.setVisibility(allowSave ? View.GONE : View.VISIBLE);
    }
    
    public void cancelSavePicture() {
    	pictureTimer = 0;
    	++currentPictureID;
    	statusTextField.setText("");
    	Toast.makeText(this, getString(R.string.canceledPictureMessage), Toast.LENGTH_SHORT).show();
    	updateButtons(true);
    }
    
    void updateTimerMessage() {
    	String messageFormat = getString(R.string.timerCountdownMessageFormat);
    	statusTextField.setText(String.format(messageFormat, pictureTimer));
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
    
    public void savePicture() {
    	if (this.pictureDelay==0) {
    		savePictureNow();
    	}
    	else {
        	savePictureAfterDelay(this.pictureDelay);
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
    			flashModes.add(FLASH_MODE_AUTO);
    		}
    		flashModes.add(FLASH_MODE_OFF);
    		flashModes.add(FLASH_MODE_ON);
    	}
    	
    	if (flashModes.size() > 0) {
    		flashButton.setVisibility(View.VISIBLE);
    		updateFlashMode(0);
    		String mode = flashModes.get(selectedFlashMode);
    		flashButton.setText(flashButtonLabels.get(mode));
    		CameraUtils.setFlashMode(arManager.getCamera(), mode);
    	}
    	else {
    		flashButton.setVisibility(View.GONE);
    	}
    }
    
    public void cycleDelay() {
    	int index = DELAY_DURATIONS.indexOf(this.pictureDelay);
    	if (index<0) {
    		this.pictureDelay = DEFAULT_DELAY;
    	}
    	else {
    		this.pictureDelay = DELAY_DURATIONS.get((index+1) % DELAY_DURATIONS.size());
    	}
    	writeDelayPreference();
    	updateDelayButton();
    }
    
    void writeDelayPreference() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(DELAY_PREFERENCES_KEY, this.pictureDelay);
    	editor.commit();
    }
    
    void readDelayPreference() {
    	// reads picture delay from preferences, updates this.pictureDelay and delay button text
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	int delay = prefs.getInt(DELAY_PREFERENCES_KEY, -1);
    	if (!DELAY_DURATIONS.contains(delay)) {
    		delay = DEFAULT_DELAY;
    	}
    	this.pictureDelay = delay;
    	updateDelayButton();
    }
    
    void updateDelayButton() {
    	if (pictureDelay==0) {
    		pictureDelayButton.setText(getString(R.string.delayButtonLabelNone));
    	}
    	else {
        	String labelFormat = getString(R.string.delayButtonLabelSecondsFormat);
        	pictureDelayButton.setText(String.format(labelFormat, this.pictureDelay));
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
	public void onPictureTaken(byte[] data, final Camera camera) {
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
					ViewImageGridActivity.startActivityWithImageURIs(this, pictureURIs);
				}
			}
			else {
				// OG Droid and possibly other phones often hang if we call takePicture directly instead of autoFocus
				handler.postDelayed(new Runnable() {
					public void run() {
						camera.autoFocus(MainActivity.this);
					}
				}, 100);
			}
			// send the same NEW_PICTURE broadcast that the standard camera app does
			try {
			    Intent newPictureIntent = new Intent("android.hardware.action.NEW_PICTURE");
			    newPictureIntent.setDataAndType(pictureURI, "image/jpeg");
			    this.sendBroadcast(newPictureIntent);
			}
			catch(Exception ex) {
			    Log.e("CamTimer", "Error broadcasting new picture", ex);
			}
		}
	}
	
	String savedImageDirectory = Environment.getExternalStorageDirectory() + File.separator + "CamTimer";
	Format dateInFilename = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
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
			Toast.makeText(this, getString(R.string.savedPictureMessage), Toast.LENGTH_SHORT).show();
			
			return Uri.fromFile(new File(path));
		}
		catch(Exception ex) {
			Toast.makeText(this, "Error saving picture: " + ex.getClass().getName(), Toast.LENGTH_LONG).show();
			return null;
		}
	}

	public void openLibrary() {
		startActivity(LibraryActivity.intentWithImageDirectory(this, savedImageDirectory));
	}
	
    @Override
    public void onShutterButtonFocus(boolean pressed) {
        shutterButton.setImageResource(pressed ? R.drawable.btn_camera_shutter_pressed_holo : 
                                                 R.drawable.btn_camera_shutter_holo);
    }

    @Override
    public void onShutterButtonClick() {
        savePicture();
    }
}