package vn.com.nms.facedetectionopencv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_CAMERA = 1;
    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;
    private Mat mGray;
    //    private Mat mRgbaR;
    private JavaCamera2View mCameraView;
    private File mCascadeFile;
    private CascadeClassifier mCascadeClassifier;
    private static final String TAG = "FaceDetection";
    private BaseLoaderCallback mBaseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    Log.d(TAG, "Loaded OpenCv successfully!");
                    mCameraView.enableView();
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", MODE_PRIVATE);
                    mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.html");
                    FileOutputStream fos = new FileOutputStream(mCascadeFile);
                    byte[] buffer = new byte[4096];
                    int byteRead;
                    while ((byteRead = is.read(buffer)) != -1){
                        fos.write(buffer, 0, byteRead);
                    }
                    is.close();
                    fos.close();
                    mCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    if (mCascadeClassifier.empty()){
                        mCascadeClassifier = null;
                    }
                    cascadeDir.delete();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCameraView = findViewById(R.id.cameraView);
        mCameraView.setVisibility(View.VISIBLE);
        mCameraView.setCvCameraViewListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mBaseLoaderCallback);
        } else {
            try {
                mBaseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "cameraview start");
        Log.d(TAG, "width: " + String.valueOf(width));
        Log.d(TAG, "height: " + String.valueOf(height));
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mGray = new Mat();
//        mRgbaR = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "cameraview stop");
        mRgba.release();
        mRgbaF.release();
        mRgbaT.release();
        mGray.release();
//        mRgbaR.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // code rotation camera
        /*Log.d(TAG, "cameraview frame");
        mRgba = inputFrame.rgba();
        Log.d(TAG, "mRgba: width: " + String.valueOf(mRgba.size().width) + ", height: " + String.valueOf(mRgba.size().height));
        Core.transpose(mRgba, mRgbaT);
        Log.d(TAG, "mRgbaT: width: " + String.valueOf(mRgbaT.size().width) + ", height: " + String.valueOf(mRgbaT.size().height));
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Log.d(TAG, "mRgbaF: width: " + String.valueOf(mRgbaF.size().width) + ", height: " + String.valueOf(mRgbaF.size().height));
        Core.flip(mRgbaF, mRgba, 1);
        Log.d(TAG, "mRgba: width: " + String.valueOf(mRgba.size().width) + ", height: " + String.valueOf(mRgba.size().height));*/

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        MatOfRect faces = new MatOfRect();
        if (mCascadeClassifier == null){
            Log.d(TAG, "mCascadeClassifier is null");
        } else {
            mCascadeClassifier.detectMultiScale(mGray, faces, 1.5, 1);
        }

        Rect[] faceArray = faces.toArray();
        for (Rect face: faceArray){
            Imgproc.rectangle(mRgba, face.tl(), face.br(), new Scalar(0, 255, 0, 255), 3);
        }


        return mRgba;
    }
}
