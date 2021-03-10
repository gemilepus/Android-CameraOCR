/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.user.tesseracttess_twoocr.Camera2API;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.tesseracttess_twoocr.Camera2API.MessageView;
import com.example.user.tesseracttess_twoocr.R;

import com.example.user.tesseracttess_twoocr.VariableEditor;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.otaliastudios.cameraview.AspectRatio;
import com.otaliastudios.cameraview.CameraUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.MORPH_GRADIENT;
import static org.opencv.imgproc.Imgproc.MORPH_OPEN;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.photo.Photo.INPAINT_NS;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2BasicFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback , View.OnTouchListener {

    private ImageButton imageview ;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    //private static final int STATE_PICTURE_TAKEN = 4;  // original
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private DialogFragment newFragment;
    private boolean runThreadFlag = false;
    private ArrayList<String> ArrayList_jpg;
    private boolean Click_takepicture = false;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(final ImageReader reader) {
            try {
                final Image image = reader.acquireNextImage();
                if(!Click_takepicture ){
                    image.close();
                }

                if(!runThreadFlag  &&  Click_takepicture ){
                    runThreadFlag = true;
                    Click_takepicture = false;

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            // zoom
//                            Bitmap resultBmp = Bitmap.createBitmap(zoom.right-zoom.left, zoom.bottom-zoom.top, Bitmap.Config.ARGB_8888);
//                            Canvas cv = new Canvas( resultBmp );
//                            cv.drawBitmap(bitmap, -zoom.left, -zoom.top, null);
//                            cv.setBitmap(bitmap);

                            // show_data_Dialog(bytes);
                            Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false);
                            imageview.setImageBitmap(thumbnailBitmap);

                            image.close();
                            //reader.close();

                            // set File Name
                            String time_str = android.text.format.DateFormat.format("yyyyMMdd hhmmss", new java.util.Date()).toString();
                            String str_file = time_str +".jpg";
                            mFile = new File(getActivity().getExternalFilesDir(null), str_file);
                            // Save File
                            //mBackgroundHandler.post(new ImageSaver( bytes , mFile));
                            FileOutputStream output = null;
                            try {
                                output = new FileOutputStream(mFile);
                                output.write(bytes);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                //  mImage.close();
                                if (null != output) {
                                    try {
                                        output.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            String [] list = getActivity().getExternalFilesDir(null).list();
                            // 順序 小 ~ 大
                            ArrayList_jpg = new ArrayList<String>();
                            for (String s : list) {
                                if (s.endsWith(".jpg")) {  // 過濾出jpg檔案
                                    ArrayList_jpg.add(s);
                                }
                            }

                            // DialogFragment.show() will take care of adding the fragment
                            // in a transaction.  We also want to remove any currently showing
                            // dialog, so make our own transaction and take care of that here.
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                            if (prev != null) {
                                ft.remove(prev);
                            }
                            ft.addToBackStack(null);
                            //Create and show the dialog.
                            //DialogFragment newFragment = Output_DialogFragment.newInstance(bytes);
                            newFragment = Output_DialogFragment.newInstance(bytes , ArrayList_jpg  );
                            newFragment.show(ft, "dialog");

                            runThreadFlag = false;
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private  class ImageSaver implements Runnable {
        /** The JPEG image */
        //private final Image mImage;
        /** The file we save the image into.*/
        private final File mFile;

        private final byte[] bytes;

        ImageSaver( byte[] mbytes, File file) {
            // ImageSaver(Image image, File file) {
            bytes = mbytes;
            // mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
//            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //  mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    private Button btn;
    private  SeekBar seekBar;
    private  TextView text_room , text_v;
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);

        Button delete_btn =  view.findViewById(R.id.delete_btn);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Delete All JPG file
                String [] list;
                list =  getActivity().getExternalFilesDir(null).list();
                // 順序 小 ~ 大
                for(int i = 0; i <  list.length; i++){
                    if(list[i].endsWith(".jpg")){  // 過濾出jpg檔案
                        File file = new File(  getActivity().getExternalFilesDir(null), list[i] );
                        file.delete();
                    }
                }
            }});




        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        view.findViewById(R.id.buttonTest).setOnClickListener(this);

        text_v =  getActivity().findViewById(R.id.text_v);
        text_room =  getActivity().findViewById(R.id.text_room);
        btn =  getActivity().findViewById(R.id.buttonTest);
        imageview = getActivity().findViewById(R.id.info);

        ImageButton imageButtonSearch = view.findViewById(R.id.re);
        imageButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.container, Camera2VideoFragment.newInstance()).commit();
            }
        });

        seekBar =  view.findViewById(R.id.seekBar);
        seekBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }

        });;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser ) {
                    text_v.setText(String.valueOf(progress) );
                    int value = progress;

                    final CaptureRequest.Builder captureBuilder;
                    try {
                        captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        //captureBuilder.addTarget(mImageReader.getSurface());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//                   mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value);
                    //mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)value);
                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)10000);
                    //mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, value);
                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2);//设置 ISO，感光度
                    //mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);
                    try {
                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                        //mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        seekBar.setMax( 3000 );
    }

    //***************************************** OnClick******************************************
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonTest: {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)1);
                mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 50);
                //mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2);//设置 ISO，感光度
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);

                try {
                    // Finally, we start displaying the camera preview.
                    mPreviewRequest = mPreviewRequestBuilder.build();
                    mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    //mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
            case R.id.picture: {

                takePicture();
                Click_takepicture = true;

                final ImageButton imageButton = getActivity().findViewById(R.id.picture);
                imageButton.setEnabled(false);

                Toast.makeText( getActivity(),    String.valueOf(mPreviewRequest.get(CaptureRequest.SENSOR_EXPOSURE_TIME) ) , Toast.LENGTH_SHORT).show();
                //Toast.makeText( getActivity(),    String.valueOf(mPreviewRequestBuilder.get(CaptureRequest.SENSOR_EXPOSURE_TIME) ) , Toast.LENGTH_SHORT).show();

//                try {
////                    mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
////                    mPreviewRequestBuilder.addTarget(surface);
//                  //  requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//
//                     This is how to tell the camera to lock focus.
//                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)1);
//                    //mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400);
//                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2);//设置 ISO，感光度
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
//                    mPreviewRequest = mPreviewRequestBuilder.build();
//                    mCaptureSession.setRepeatingRequest(mPreviewRequest,mCaptureCallback, mBackgroundHandler);

//                     Tell #mCaptureCallback to wait for the lock.
//                    mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//                    mState = STATE_PREVIEW ; mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
//                     Reset the auto-focus trigger
//                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//                    setAutoFlash(mPreviewRequestBuilder);
//                    mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
//                     After this, the camera will go back to the normal state of preview.
//                    mState = STATE_PREVIEW ; mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//                builder.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue);
//                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeValueInMilliseconds);
//                CaptureSession.setRepeatingRequest

                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                imageButton.setEnabled(true);
                            }});
                    }}, 1500);

                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
//                    new AlertDialog.Builder(activity)
//                            //.setMessage(R.string.intro_message)
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show();
                }

                // DialogFragment.show() will take care of adding the fragment
                // in a transaction.  We also want to remove any currently showing
                // dialog, so make our own transaction and take care of that here.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                // Create and show the dialog.
                //DialogFragment newFragment = Output_DialogFragment.newInstance(bytes);
                newFragment =  new Album_DialogFragment();
                newFragment.show(ft, "dialog");

                break;
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                // Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                Boolean available = false; // 當成沒有
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
        }
    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // ++
            //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion); //O
            //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);
            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    // showToast("Saved: " + mFile);
                    //Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // This is how to tell the camera to lock focus.
//            mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)1);
//            //mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400);
//            mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, (10000 - 100) / 2); // ISO
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
//            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion);
            //setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);

//            final CaptureRequest.Builder captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean Flashonflag = true;
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported && Flashonflag) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            if (Flashonflag == true) {
//                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            }
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*********************************************** DialogFragment *********************************************/
    public static class Output_DialogFragment extends DialogFragment {
        byte[] mbyte;
        ArrayList<String> mArrayList_jpg;
        static Output_DialogFragment newInstance( byte[] bytes , ArrayList<String> ArrayList_jpg) {
            Output_DialogFragment f = new Output_DialogFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putByteArray("num", bytes);
            args.putStringArrayList("ArrayList_jpg" , ArrayList_jpg);
            f.setArguments(args);

            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mbyte = getArguments().getByteArray("num");
            mArrayList_jpg = getArguments().getStringArrayList("ArrayList_jpg");
            Bitmap bitmap = BitmapFactory.decodeByteArray(  mbyte, 0 , mbyte.length);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.activity_picture_preview, null);

            final ImageView dialog_imageView = view.findViewById(R.id.image);
            final MessageView nativeCaptureResolution =  view.findViewById(R.id.nativeCaptureResolution);
            // final MessageView actualResolution = findViewById(R.id.actualResolution);
            // final MessageView approxUncompressedSize = findViewById(R.id.approxUncompressedSize);
            // final MessageView captureLatency =  view.findViewById(R.id.captureLatency);

            final Button change_btn = view.findViewById(R.id.change_btn);
            change_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if( VariableEditor.Picture_type.equals("1")){
                        VariableEditor.Picture_type =  "2" ;
                        change_btn.setText(" Original");
                    }else{
                        VariableEditor.Picture_type =  "1" ;
                        change_btn.setText(" CV");
                    }
                }
            });
            if( VariableEditor.Picture_type.equals("1")){
                change_btn.setText(" CV");
            }else{
                change_btn.setText(" Original");
            }

            //final long delay = getIntent().getLongExtra("delay", 0);
            final int nativeWidth  = bitmap.getWidth();
            final int nativeHeight = bitmap.getHeight();

            // resizedBitmap
            int maxSize = 500;
            int outWidth;
            int outHeight;
            int inWidth = bitmap.getWidth();
            int inHeight = bitmap.getHeight();
            if(inWidth > inHeight){
                outWidth = maxSize;
                outHeight = (inHeight * maxSize) / inWidth;
            } else {
                outHeight = maxSize;
                outWidth = (inWidth * maxSize) / inHeight;
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);

            //Bitmap resizedBitmap = bitmap;

            Bitmap dstBmp;
            if(VariableEditor.Picture_type.equals("1")){

                // Original Image
                Mat mat = new Mat();
                Bitmap bmp32 = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);

                //Change color space from BGR to Gray.
                Mat gray = new Mat();
                Imgproc.cvtColor( mat, gray, Imgproc.COLOR_BGR2GRAY);

                //Apply Morphological Gradient.
                Mat gradient = new Mat();
                Mat morphStructure = Imgproc.getStructuringElement(MORPH_ELLIPSE, new org.opencv.core.Size(3, 3));
                // Imgproc.morphologyEx(gray, gradient, MORPH_GRADIENT, morphStructure);
                Imgproc.morphologyEx(gray, gradient, MORPH_OPEN, morphStructure);

                //Opening
                // opening = cv.morphologyEx(img, cv.MORPH_OPEN, kernel)
//            Mat gradientOpening  = new Mat();
//            Mat morphStructureOpening = Imgproc.getStructuringElement(MORPH_OPEN, new org.opencv.core.Size(5, 5));
//
//            Imgproc.morphologyEx( gradient, gradientOpening , MORPH_GRADIENT, morphStructureOpening );

                //Apply threshold to convert to binary image  using Otsu algorithm to choose the optimal threshold value to convert the processed image to binary image.
                Mat binary = new Mat();
                //Imgproc.threshold(gradient, binary, 20, 200, THRESH_BINARY | THRESH_OTSU);

                Imgproc.threshold(gradient, binary, 80, 255, THRESH_BINARY );

                final Bitmap bmp = Bitmap.createBitmap(  binary.cols(),  binary.rows() , Bitmap.Config.ARGB_8888);
                Utils.matToBitmap( binary, bmp);
                dstBmp = Bitmap.createBitmap( bmp);

            }else{
                // Original Image
                Mat mat = new Mat();
                Bitmap bmp32 = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);

                // 降噪
                Mat mMat = new Mat();
                Photo.fastNlMeansDenoisingColored( mat , mMat,10,10,7,21);

                final Bitmap bmp = Bitmap.createBitmap( mMat.cols(),  mMat.rows() , Bitmap.Config.ARGB_8888);
                Utils.matToBitmap( mMat, bmp);
                dstBmp = Bitmap.createBitmap( bmp);
            }

            final ImageView OutpusImageView = view.findViewById(R.id.img);
            //OutpusImageView.setImageBitmap(dstBmp);
            OutpusImageView.setVisibility(View.GONE);

            final TextView textView =  view.findViewById(R.id.textView);
            // TextView mtext = view.findViewById(R.id.text_o);

            final Button btnOCR = view.findViewById(R.id.btnOCR);
            btnOCR .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if( VariableEditor.ORC_type.equals("eng")){
                        VariableEditor.ORC_type =  "chi_tra" ;
                        btnOCR.setText("chi_tra");
                    }else{
                        VariableEditor.ORC_type =  "eng" ;
                        btnOCR.setText("eng");
                    }
                }
            });

            if( VariableEditor.ORC_type.equals("eng")){
                btnOCR.setText("eng");
            }else{
                btnOCR.setText("chi_tra");
            }

            String SD_PATH= Environment.getExternalStorageDirectory().getPath();
            TessBaseAPI baseApi = new TessBaseAPI();
            // 指定語言集，sd卡根目錄下放置Tesseract的tessdata資料夾
            baseApi.init(SD_PATH, VariableEditor.ORC_type );
            // 設置psm模式
            //baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
            // 設置圖片
            baseApi.setImage(  dstBmp );
            // baseApi.setImage(new File(SD_PATH + img));
            // 獲取結果
            final String result = baseApi.getUTF8Text();
            textView.setText(result);
            // 釋放記憶體
            baseApi.clear();
            baseApi.end();

            CameraUtils.decodeBitmap(mbyte, 1000, 1000, new CameraUtils.BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    dialog_imageView.setImageBitmap(bitmap);

                    // captureLatency.setTitle("Approx. capture latency");
                    // captureLatency.setMessage(delay + " milliseconds");

                    // ncr and ar might be different when cropOutput is true.
                    AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                    nativeCaptureResolution.setTitle("原始解析度");
                    nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

                    // AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                    //actualResolution.setTitle("Actual resolution");
                    // actualResolution.setMessage(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
                }
            });

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view);
            // Add action buttons
            builder.setNeutralButton("刪除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    Toast.makeText( getActivity(),   "delete" +  mArrayList_jpg.get( mArrayList_jpg.size() -1 )  , Toast.LENGTH_SHORT).show();
                    File file = new File(getActivity().getExternalFilesDir(null),  mArrayList_jpg.get( mArrayList_jpg.size() -1 ));
                    boolean deleted = file.delete();
                }});
            builder.setNegativeButton("繼續", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {

                }});

            return builder.create();
        }
    }
    /***********************************************D ialogFragment *********************************************/

    /*********************************************** AlbumDialogFragment *********************************************/
    public static class Album_DialogFragment extends DialogFragment {
        private ArrayList<String> ArrayList_jpg;
        private int show_image_index = 0;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.album_picture_preview, null);
            final TextView text_no = view.findViewById(R.id.text_no);
            final ImageView dialog_imageView = view.findViewById(R.id.image);
            final MessageView nativeCaptureResolution =  view.findViewById(R.id.nativeCaptureResolution);

            // read JPG file
            String [] list;
            list =  getActivity().getExternalFilesDir(null).list();
            ArrayList_jpg = new ArrayList<String>();
            for(int i = 0; i <  list.length; i++){
                if(list[i].endsWith(".jpg")){  // 過濾出jpg檔案
                    ArrayList_jpg.add(list[i]);
                    //Toast.makeText( getActivity(),   list[i] , Toast.LENGTH_SHORT).show();
                }
            }

            text_no.setText( (show_image_index + 1) + " / " + ArrayList_jpg.size());
            File file = new File(getActivity().getExternalFilesDir(null),  ArrayList_jpg.get(0));
            String filePath = file.getPath();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            int nativeWidth  = bitmap.getWidth();
            int nativeHeight = bitmap.getHeight();
            AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
            nativeCaptureResolution.setTitle("原始解析度");
            nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

            final int maxSize = 800;
            int outWidth;
            int outHeight;
            int inWidth = bitmap.getWidth();
            int inHeight = bitmap.getHeight();
            if(inWidth > inHeight){
                outWidth = maxSize;
                outHeight = (inHeight * maxSize) / inWidth;
            } else {
                outHeight = maxSize;
                outWidth = (inWidth * maxSize) / inHeight;
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
            dialog_imageView.setImageBitmap(resizedBitmap);

            final ImageButton rightbtn = view.findViewById(R.id.rightbtn);
            final ImageButton leftbtn = view.findViewById(R.id.leftbtn);
            leftbtn.setVisibility(View.GONE);
            leftbtn.setScaleY((float) 1.5); // 縮小
            leftbtn.setScaleX((float) 1.5); // 縮小
            leftbtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if(show_image_index > 0){
                        show_image_index --;

                        File file = new File(getActivity().getExternalFilesDir(null),  ArrayList_jpg.get( show_image_index));
                        //Toast.makeText( getActivity(),  "read" + ArrayList_jpg.get( show_image_index)  , Toast.LENGTH_SHORT).show();

                        String filePath = file.getPath();
                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                        if(bitmap == null){
                            nativeCaptureResolution.setTitle("已刪除");
                            nativeCaptureResolution.setMessage("");
                            dialog_imageView.setImageBitmap(bitmap);
                        }
                        else{
                            int nativeWidth  = bitmap.getWidth();
                            int nativeHeight = bitmap.getHeight();
                            AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                            nativeCaptureResolution.setTitle("原始解析度");
                            nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

                            // final int maxSize = 960;
                            int outWidth;
                            int outHeight;
                            int inWidth = bitmap.getWidth();
                            int inHeight = bitmap.getHeight();
                            if(inWidth > inHeight){
                                outWidth = maxSize;
                                outHeight = (inHeight * maxSize) / inWidth;
                            } else {
                                outHeight = maxSize;
                                outWidth = (inWidth * maxSize) / inHeight;
                            }
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                            dialog_imageView.setImageBitmap(resizedBitmap);
                        }

                        if(show_image_index == 0){
                            leftbtn.setVisibility(View.GONE);
                        }
                        text_no.setText( (show_image_index + 1) + " / " + ArrayList_jpg.size());
                        rightbtn.setVisibility(View.VISIBLE);
                    }else{
                        leftbtn.setVisibility(View.GONE);

                    }
                }
            });

            rightbtn.setScaleY((float) 1.5); // 縮小
            rightbtn.setScaleX((float) 1.5); // 縮小
            rightbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(show_image_index < ArrayList_jpg.size() -1){
                        show_image_index ++;

                        File file = new File(getActivity().getExternalFilesDir(null),  ArrayList_jpg.get( show_image_index));
                        //Toast.makeText( getActivity(),  "read" + ArrayList_jpg.get( show_image_index)  , Toast.LENGTH_SHORT).show();

                        String filePath = file.getPath();
                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                        if(bitmap == null){
                            nativeCaptureResolution.setTitle("已刪除");
                            nativeCaptureResolution.setMessage("");
                            dialog_imageView.setImageBitmap(bitmap);
                        }
                        else{
                            int nativeWidth  = bitmap.getWidth();
                            int nativeHeight = bitmap.getHeight();
                            AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                            nativeCaptureResolution.setTitle("原始解析度");
                            nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

                            // final int maxSize = 960;
                            int outWidth;
                            int outHeight;
                            int inWidth = bitmap.getWidth();
                            int inHeight = bitmap.getHeight();
                            if(inWidth > inHeight){
                                outWidth = maxSize;
                                outHeight = (inHeight * maxSize) / inWidth;
                            } else {
                                outHeight = maxSize;
                                outWidth = (inWidth * maxSize) / inHeight;
                            }
                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
                            dialog_imageView.setImageBitmap(resizedBitmap);
                        }

                        text_no.setText( (show_image_index + 1) + " / " + ArrayList_jpg.size());
                        leftbtn.setVisibility(View.VISIBLE);
                        if(show_image_index == ArrayList_jpg.size() -1){
                            rightbtn.setVisibility(View.GONE);
                        }
                    }
                    else{
                        rightbtn.setVisibility(View.GONE);
                    }
                }
            });

            Button delbtn = view.findViewById(R.id.deletebtn);
            delbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText( getActivity(),   "delete " +  ArrayList_jpg.get( show_image_index )  , Toast.LENGTH_SHORT).show();
                    File file = new File(getActivity().getExternalFilesDir(null),  ArrayList_jpg.get( show_image_index ));
                    boolean deleted = file.delete();

                    nativeCaptureResolution.setTitle("已刪除");
                    nativeCaptureResolution.setMessage("");

                    File filed = new File(getActivity().getExternalFilesDir(null),  ArrayList_jpg.get( show_image_index));
                    //Toast.makeText( getActivity(),  "read" + ArrayList_jpg.get( show_image_index)  , Toast.LENGTH_SHORT).show();

                    String filePath = filed.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    dialog_imageView.setImageBitmap(bitmap);
                }
            });

            Button closebtn = view.findViewById(R.id.closebtn);
            closebtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();

                }
            });
            Button upbtn = view.findViewById(R.id.upbtn);
            upbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view);

            return builder.create();
        }
    }
    /********************************************** AlbumDialogFragment *********************************************/

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback( getActivity() ) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    public void onStart() {
        super.onStart();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this.getActivity() , mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            mTextureView.setOnTouchListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        startBackgroundThread();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();

        // dismiss Fragment Dailog  (onEndOfErrorDumpThread)
        if(newFragment != null){
            newFragment.dismiss();
        }
    }

    public Rect zoom ;
    public float finger_spacing = 0;
    public float zoom_level = 1;  //public int zoom_level = 1;
    public boolean onTouch(View v, MotionEvent event) {
        try {
            Activity activity = getActivity();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*10;
            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float current_finger_spacing;
            if (event.getPointerCount() > 1) { // Multi touch logic
                current_finger_spacing = getFingerSpacing(event);
                if(finger_spacing != 0){
                    if(current_finger_spacing > finger_spacing && maxzoom > zoom_level){   // zoom_level++;
                        zoom_level+=0.5;
                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1){ // zoom_level--;
                        zoom_level-=0.5;
                    }
                    int minW = (int) (m.width() / maxzoom);
                    int minH = (int) (m.height() / maxzoom);
                    int difW = m.width() - minW;
                    int difH = m.height() - minH;
                    int cropW = difW /100 *(int)zoom_level;
                    int cropH = difH /100 *(int)zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                    mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = current_finger_spacing;
            } else{
                if (action == MotionEvent.ACTION_UP) { //single touch logic

                }
            }
            //btn.setText(  String.valueOf(zoom_level) );
            text_room.setText( "Room:" + String.valueOf(zoom_level) );
            //Toast.makeText( getActivity(),    String.valueOf(zoom_level) , Toast.LENGTH_SHORT).show();
            try {
                //mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);

                // Finally, we start displaying the camera preview.
                mPreviewRequest = mPreviewRequestBuilder.build();// ++ TEST
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);// ++ TEST
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        }
        return true;
    }
    //Determine the space between the first two fingers
    @SuppressWarnings("deprecation")
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /***********************************************   TEST   ***********************************************/
    // 棄用
    private Rect mCropRegion;
    double mCurrentZoomLevel = 1;
    public void changeZoomLevel(double level){
        // cameraId = 対象のカメラID
        CameraManager cameraMgr = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics;
        try {
            characteristics = cameraMgr.getCameraCharacteristics( mCameraId);
            //characteristics = cameraMgr.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return;
        }

        // 最大ズームサイズ
        float max = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        seekBar.setMax( (int)max*10 );

        if ( max < level || mCurrentZoomLevel == level) {
            return;
        }
        //Toast.makeText( getActivity(),    String.valueOf( max ) , Toast.LENGTH_SHORT).show();

        mCurrentZoomLevel = level;
        final Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        if (mCurrentZoomLevel == 1) {
            mCropRegion.set(activeArraySize);
        } else {
            //noinspection ConstantConditions
            int cx = activeArraySize.centerX();
            int cy = activeArraySize.centerY();
//            int hw = (activeArraySize.width() >> 1) / mCurrentZoomLevel;
//            int hh = (activeArraySize.height() >> 1) / mCurrentZoomLevel;
            int hw = (int)((double)activeArraySize.width()   / mCurrentZoomLevel);
            int hh = (int)((double)activeArraySize.height()  / mCurrentZoomLevel);
            mCropRegion = new Rect(cx - hw, cy - hh, cx + hw, cy + hh);
        }
    }

    private AlertDialog dialog_show;
    private void show_data_Dialog(byte[] bytes  ) {   //  onEndOfErrorDumpThread
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        // android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.DialogTheme); //style
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_picture_preview, null);

        final ImageView dialog_imageView = view.findViewById(R.id.image);
        final MessageView nativeCaptureResolution =  view.findViewById(R.id.nativeCaptureResolution);
//        final MessageView actualResolution = findViewById(R.id.actualResolution);
//        final MessageView approxUncompressedSize = findViewById(R.id.approxUncompressedSize);
//        final MessageView captureLatency =  view.findViewById(R.id.captureLatency);

//        final long delay = getIntent().getLongExtra("delay", 0);
        final int nativeWidth  = bitmap.getWidth();
        final int nativeHeight = bitmap.getHeight();
//        byte[] b = image == null ? null : image.get();
//        if (b == null) {
//            finish();
//            return;
//        }
        CameraUtils.decodeBitmap(bytes, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                dialog_imageView.setImageBitmap(bitmap);

                // approxUncompressedSize.setTitle("Approx. uncompressed size");
                // approxUncompressedSize.setMessage(getApproximateFileMegabytes(bitmap) + "MB");

                // captureLatency.setTitle("Approx. capture latency");
                // captureLatency.setMessage(delay + " milliseconds");

                // ncr and ar might be different when cropOutput is true.
                AspectRatio nativeRatio = AspectRatio.of(nativeWidth, nativeHeight);
                nativeCaptureResolution.setTitle("原始解析度");
                nativeCaptureResolution.setMessage(nativeWidth + "x" + nativeHeight + " (" + nativeRatio + ")");

                // AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                //actualResolution.setTitle("Actual resolution");
                // actualResolution.setMessage(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
            }
        });

        builder.setView(view);
        builder.setPositiveButton("繼續", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton("上傳", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog_show = builder.create();
        dialog_show.show();
        dialog_show.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_show.dismiss();
            }
        });
    }
}