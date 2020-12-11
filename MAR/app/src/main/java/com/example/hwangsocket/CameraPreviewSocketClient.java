package com.example.hwangsocket;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.lang.String;
import java.io.BufferedWriter;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.io.OutputStream;
import android.media.Image.Plane;
import android.os.Build;
import android.graphics.YuvImage;
import android.graphics.Rect;
import android.graphics.BitmapFactory;

import java.io.OutputStreamWriter;
import java.io.IOException;




public class CameraPreviewSocketClient extends AppCompatActivity {

    private TextureView HwangTextureView;

    private String mCameraId;
    private Size mPreviewSize;
    private Size mCaptureSize;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;

    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;

    private byte[][] yuvBytes = new byte[3][];
    private Bitmap rgbFrameBitmap = null;

    private static Socket socket;
    private static final int SERVERPORT = 8809;
    private static final String SERVER_IP = "192.168.1.100";
    private static final String TAG = "HaoxinCameraMsg"; // Debug filter

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private int[] rgbBytes = null;
    //private boolean isProcessingFrame = false;
    //private Runnable postInferenceCallback;
    private static boolean SAVE_PREVIEW_BITMAP = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera_preview_socket_client);

        /* Thread */
        //new Thread(new CameraPreviewSocketClient.HwangImageThread()).start();

        HwangTextureView = (TextureView) findViewById(R.id.HwangTextureView);
    }



    @Override
    protected void onResume() {
        super.onResume();

        startCameraThread();//

        if (!HwangTextureView.isAvailable()){  //Returns true if the Surface Texture associated with this TextureView is available for rendering

            HwangTextureView.setSurfaceTextureListener(HwangTextureListener); // Set up a surface texture listener
        }
        else {
            startPreview();//
        }

    }

    /* Start camera Thread */
    private void startCameraThread(){
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    /* SurfaceTextureListener */
    private TextureView.SurfaceTextureListener HwangTextureListener = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            /* When surface texture is available, setup camera and open it */
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
        }
    };

    /*Set up Camera*/
    private void setupCamera(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            //getCameraIdList
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //open the back camera
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                int previewWidth = mPreviewSize.getWidth();
                int previewHeight = mPreviewSize.getHeight();
                rgbBytes = new int[previewWidth * previewHeight];
                rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

                /* 获取相机支持的最大拍照尺寸*/
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });

                setupImageReader();

                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    /* Open the camera */
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // Check the permission of using camera
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /* Camera State callback */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback(){

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };


    /* Start camera preview*/
    private void startPreview(){
        SurfaceTexture mSurfaceTexture = HwangTextureView.getSurfaceTexture(); // Returns the surface texture of the HwangTextureView
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface previewSurface = new Surface(mSurfaceTexture);
        try{
            //创建CaptureRequestBuilder，TEMPLATE_PREVIEW表示预览请求
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置previewSurface作为预览数据的显示界面
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCaptureRequestBuilder.addTarget(mImageReader.getSurface()); // new Code

            //Create camera capture session，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口,当它创建好后会回调onConfigured方法,第三个参数用来确定Callback在哪个线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback(){

            //mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        // Create a capture request
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                        //yuvBytes = new byte[3][];

                    }catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            },mCameraHandler);

        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /* Using ImageReader to obtain camera preview frames*/

    private void setupImageReader(){
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),ImageFormat.YUV_420_888, 2);
        Log.i(TAG,"Setup ImageReader");

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener(){
            @Override
            public void onImageAvailable(final ImageReader reader) {
                Log.i(TAG,"1");

                /* Test 1 Run in Main Thread
                * (Remove "final" before ImageReader )*/

                //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                //java.io.File mfile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "IMG_" + timeStamp + ".jpg");
                //myImageUtils.compressToJpeg(mfile,reader.acquireLatestImage());

                /* Test 2 Run in Background*/
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        java.io.File mfile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "IMG_" + timeStamp + ".jpg");

                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            myImageUtils.compressToJpeg(mfile, image);
                        }
                        else{
                            Log.i(TAG,"no image");
                        }
                    }
                });

            }

        }, mCameraHandler);
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (mCameraHandler != null) {
            mCameraHandler.post(r);
        }
    }


/*
    public static class imageSaver implements Runnable {
        private Bitmap mbitmap;
        //private Image mImage;

        public imageSaver(Bitmap bitmap) {
            mbitmap = bitmap;
            //mImage = image;
            Log.i(TAG, "1");
        }
        @Override
        public void run() {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            java.io.File mfile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "IMG_" + timeStamp + ".jpg");
            Log.i(TAG, "4");
            try {
                Log.i(TAG, "6");
                FileOutputStream out = new FileOutputStream(mfile);
                Log.i(TAG, "7");
                //out.write(data, 0, data.length);
                mbitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Log.i(TAG, "8");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

/*
    public static class imageSaver implements Runnable {

        private Image mImage;

        public imageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            java.io.File mfile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "IMG_" + timeStamp + ".jpg");
            Log.i(TAG, "4");
            mImage.close();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mfile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
*/
    /* Define the Thread */
/*
    class HwangImageThread implements Runnable{
        @Override
        public void run() {
            try{
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Log.i(TAG,"New Socket");
                socket = new Socket(serverAddr, SERVERPORT);

            }catch (UnknownHostException e1){
                e1.printStackTrace();
            }catch (IOException e1){
                e1.printStackTrace();
            }
        }
    }
*/


}
