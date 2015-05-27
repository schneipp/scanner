package com.moovaa.plugins.scanner;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.*;

public class MoovaaScanVideo extends Activity {


    public static String VOICEOVER = ""; //cordova param
    public static String FILENAME = "";  //cordova param

    public static String VIDEO_URI = ""; //return param
    public static String ERROR_MESSAGE = "ErrorMessage";
    public static int RESULT_ERROR = 2;


    //internal vars
    File capturedVideoFilename = null;
    String external_dir = "";

    private Camera mCamera;
    MediaPlayer mpscansound;
    private SurfaceView surface_view;
    //SurfaceHolder.Callback sh_ob = null;
    SurfaceHolder surface_holder        = null;
    SurfaceHolder.Callback sh_callback  = null;
    MediaRecorder recorder = null;
    private boolean isRecording = true;
    int screen_width=0;
    int screen_height=0;
    boolean mPreviewRunning = false;
    List<Camera.Size> mSupportedPreviewSizes = null;

    //scanline stuff
    int scanline_framesPerSecond = 60;
    long scanline_animationDuration = 10000; // 10 seconds
    SurfaceHolder surface_holder_scanline        = null;
    SurfaceHolder.Callback sh_callback_scanline  = null;
    Matrix scanline_matrix = new Matrix(); // transformation matrix
    long scanline_startTime;
    boolean is_pause = false;



    class EndVideoTask extends AsyncTask<String, Void, Boolean > {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected Boolean doInBackground(String... stuff) {
            if(recorder!=null) {
                try {
                    recorder.stop();  // stop the recording
                }catch(Exception e) {
                    e.printStackTrace();
                }
                try {
                    releaseMediaRecorder();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            if(mCamera!=null) {
                try {
                    releaseCamera();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
/*
            if(d!=null) {
                if(media!=null) {
                    try {
                        if(!is_pause) {
//TODO: RepaceWithCordova                            d.insertRoomMedia(media.getAbsolutePath(), 0);
                        }
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Log.d("moovaa","No db object! crap!");
            }
*/
            try {
                my_callback().surfaceDestroyed(surface_holder);
            }catch(Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        @Override
        protected void onPostExecute(Boolean result) {
            if(result){ //everything haltet
                try {
                    surface_view.setEnabled(false);
                }catch(Exception e){
                    e.printStackTrace();
                }
                if(mpscansound!=null) {
                    if(mpscansound.isPlaying()) {
                        mpscansound.stop();
                    }
                }
                surface_view = null;
                surface_holder = null;
/*
                try {
                    String check = d.getSingleResult("SELECT ID FROM userMedia WHERE ID=(SELECT ID FROM userRoom WHERE roomlist_ID=11) ");
                    if(check.equals("")){
                        Helpers.SetCurrentView("moovaa_hasObjectsThatStay.html");
                    }else {
                        if(d.getKeyVal("done").equals("1")){
                            Helpers.SetCurrentView("moovaa_overview.html");
                        }else{
                            Helpers.SetCurrentView("moovaa_gobackinside.html");
                        }
                    }

                    d.release();
                }catch(Exception e) {
                    e.printStackTrace();
                }
*/
                Intent data = new Intent();
                data.putExtra(VIDEO_URI, Uri.fromFile(capturedVideoFilename).toString());
                setResult(RESULT_OK, data);
                finish();

                /*
                    if(!is_pause) {
                        Intent whatactivity;
                        whatactivity = new Intent(getBaseContext(), MainActivity.class); // SCAN WAS OK, MOVE ON!
                        whatactivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        whatactivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(whatactivity);
                    }else{
                        is_pause = false;
                    }
                */
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moovaa_scan_video);

        initCamera();

        Button captureButton = (Button)findViewById(R.id.buttonStopScan);
        captureButton.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 if (isRecording) {
                                                     isRecording=false;
                                                     new EndVideoTask().execute("");
                                                 }
                                             }
                                         }
        );



    }

    private void initCamera(){

        this.surface_view = (SurfaceView)findViewById(R.id.surfaceView);
        this.scanline_startTime = System.currentTimeMillis();


   //     MainActivity.dbpath = getExternalFilesDir(null).getPath()+ "/moovaa/moovaadb";
        external_dir = getExternalFilesDir(null).getPath();



        if(surface_holder==null){
            surface_holder = this.surface_view.getHolder();
        }
        sh_callback = my_callback();
        surface_holder.addCallback(sh_callback);
        surface_holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


   //     String userRoom_ID = d.getKeyVal("currentRoom");
   //     String roomList_ID = d.getSingleResult("SELECT roomlist_ID FROM userRoom WHERE ID='"+userRoom_ID+"'");
        File voiceover = new File(VOICEOVER);
        if(voiceover.exists()){
            if(!mpscansound.isPlaying()) {
                mpscansound.start();
            }
        }


        capturedVideoFilename = new File(getExternalFilesDir(null).getPath()+ "/moovaa/", "media");
        capturedVideoFilename.mkdirs();
        Date date = new Date();
        long unique_identifier = date.getTime();
        capturedVideoFilename = new File(capturedVideoFilename, "MOV_MOOVAA_" + unique_identifier + ".mp4");


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screen_width = size.x;
        screen_height = size.y;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private boolean prepareVideoRecorder(){

        recorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        recorder.setCamera(mCamera);
        // Step 2: Set sources
//        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
/*
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
*/
        /*
        try {
            recorder.setVideoSize(800, 600);
        }catch(Exception e){
            e.printStackTrace();
        }
        */

        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        int w = 800;
        int h = 600;

        for(Camera.Size size : mSupportedPreviewSizes){
            if(size.width>639 && size.width<1025){
                w = size.width;
                h = size.height;
            }
        }
        recorder.setVideoSize(w, h);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);



        // Step 4: Set output file
        String path = capturedVideoFilename.getAbsolutePath();
        Log.d("moovaa", path);
        recorder.setOutputFile(path);

        // Step 5: Set the preview output
        recorder.setPreviewDisplay(surface_view.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("moovaa", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            recorder.release();
            recorder = null;
            return false;
        } catch (IOException e) {
            Log.d("moovaa", "IOException preparing MediaRecorder: " + e.getMessage());
            recorder.release();
            recorder = null;
            return false;
        }
        return true;
    }



    @Override
    protected void onPause() {
        super.onPause();
        if(mpscansound!=null){
            if(mpscansound.isPlaying()) {
                mpscansound.stop();
            }
        }
        is_pause = true;
        new EndVideoTask().execute("");
        if(recorder!=null){
            try {
                recorder.stop();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        if(mCamera != null) {
            try {
                mCamera.stopPreview();
            }catch (Exception e) {
                e.printStackTrace();
            }
            try {
                recorder.setCamera(null);
            }catch(Exception e){
                e.printStackTrace();
            }
            try{
                mCamera.release();
            }catch(Exception e){
                e.printStackTrace();
            }
            mCamera = null;
        }
        if(mpscansound!=null){
            try {
                mpscansound.pause();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        finish();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("moovaa","restart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("moovaa","Resume Camera view..");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == getRequestedOrientation()) {
            Log.d("moovaa","NOW IN LANDSCAPE!");
//            initCamera();

        }
        if(mpscansound!=null){
            try {
                mpscansound.start();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
//        prepareVideoRecorder();
        /*
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                releaseCamera();
                mCamera = Camera.open();
                mCamera.startPreview();
//                recorder.setCamera(mCamera);
            } catch (RuntimeException ex){
                ex.printStackTrace();
             //   Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
        */
    }



    boolean startContinuousAutoFocus() {

        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();




        String CAF_PICTURE = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                CAF_VIDEO = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                supportedMode = focusModes
                        .contains(CAF_PICTURE) ? CAF_PICTURE : focusModes
                        .contains(CAF_VIDEO) ? CAF_VIDEO : "";

        if (!supportedMode.equals("")) {

            params.setFocusMode(supportedMode);
            mCamera.setParameters(params);
            return true;
        }

        return false;
    }


    SurfaceHolder.Callback my_scanlinecallback(){
        SurfaceHolder.Callback ob1 = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                tryDrawing(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
                tryDrawing(surfaceHolder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
            private void tryDrawing(SurfaceHolder holder) {
                Log.d("moovaa", "Trying to draw...");

                Canvas canvas = holder.lockCanvas();
                if (canvas == null) {
                    Log.e("moovaa", "Cannot draw onto the canvas as it's null");
                } else {
                    drawMyStuff(canvas);
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            private void drawMyStuff(final Canvas canvas) {
                Random random = new Random();
                Log.d("moovaa", "Drawing...");
                if(canvas!=null) {
                    final Paint p = new Paint();
                    p.setColor(Color.BLUE);
                    p.setStyle(Paint.Style.STROKE);

//                    canvas.drawLine(100,100,0,1000,p);
                    long elapsedTime = System.currentTimeMillis() - scanline_startTime;
                    scanline_matrix.postRotate(30 * elapsedTime/1000);        // rotate 30° every second
                    scanline_matrix.postTranslate(100 * elapsedTime/1000, 0); // move 100 pixels to the right
                    // other transformations...
                    canvas.concat(scanline_matrix);        // call this before drawing on the canvas!!
                    canvas.drawLine(100,100,0,1000,p);
                }




            }

        };
        return ob1;
    }


    SurfaceHolder.Callback my_callback() {
        SurfaceHolder.Callback ob1 = new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                    mCamera.release();
                    mCamera = null;

                }
                if(recorder!=null){
                    recorder.reset();
                    recorder.release();
                    recorder = null;
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if(mCamera==null) {

                    mCamera = Camera.open();
                    if(startContinuousAutoFocus()){
                        Log.d("moovaa","Scanning with continous auto focus.. ");
                    }else{
                        Log.d("moovaa","No auto focus! ");
                    }


                    mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

                    //start recording
                    if(prepareVideoRecorder()){
                        try {
                            recorder.start();
                        }catch(Exception e){
                            Log.d("moovaa","Can't init video recorder");
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException exception) {
                    exception.printStackTrace();
                    mCamera.release();
                    mCamera = null;
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                /*
                try {
                    mCamera.startPreview();
                }catch(Exception e){
                    if(mCamera!=null){
                        if(recorder!=null) {
                            recorder.release();
                        }
                        mCamera.release();
                        mCamera=null;
                    }
                    e.printStackTrace();
                }*/
// XXX stopPreview() will crash if preview is not running
                if (mPreviewRunning) {
                    mCamera.stopPreview();
                }

                Camera.Parameters p = mCamera.getParameters();
                mCamera.setParameters(p);
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mCamera.startPreview();
                mPreviewRunning = true;
            }

        };
        return ob1;
    }

    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.reset();   // clear recorder configuration
            recorder.release(); // release the recorder object
            recorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
            if(mpscansound!=null){
                mpscansound.stop();
                mpscansound = null;
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_moovaa_scan_video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
