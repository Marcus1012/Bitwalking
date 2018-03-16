package bitwalking.bitwalking;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by Marcus on 11/1/15.
 */
public class MyAddressFragment extends MyFragment implements View.OnClickListener {

    String TAG = "MyAddress";
    View _view;

    // Camera
    CameraManager _cameraManager;

    // UI
    Button _scanButton, _myAddrButton;
    ImageView _myAddrImage;
    SurfaceView _cameraSurface;

    public MyAddressFragment() {
        setFragmentTag("My Address");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.my_address_layout, container, false);

        // Buttons
        _scanButton = (Button)_view.findViewById(R.id.scan_code);
        _myAddrButton = (Button)_view.findViewById(R.id.my_address);

        _scanButton.setOnClickListener(this);
        _myAddrButton.setOnClickListener(this);

        // My address and Camera
        _cameraSurface = (SurfaceView)_view.findViewById(R.id.camera_surface);
        _myAddrImage = (ImageView)_view.findViewById(R.id.my_address_image);

        return _view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_code: handleScan(); break;
            case R.id.my_address: handleMyAddress(); break;
            default: break;
        }
    }

    private void handleScan() {
        // Buttons
        _myAddrButton.setEnabled(true);
        _scanButton.setEnabled(false);
        // Image and Camera
        _myAddrImage.setVisibility(View.INVISIBLE);
        _cameraSurface.setVisibility(View.VISIBLE);

        /*
        try {

            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

            getActivity().startActivityForResult(intent, 0);

        } catch (Exception e) {

            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);

        }
        */


    }

    private void handleMyAddress() {
        //Buttons
        _myAddrButton.setEnabled(false);
        _scanButton.setEnabled(true);
        // Image and Camera
        _myAddrImage.setVisibility(View.VISIBLE);
        _cameraSurface.setVisibility(View.INVISIBLE);

    }
/*
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            String cameraId = getFrontFacingCameraId(_cameraManager);

            _cameraManager.openCamera(cameraId, cameraCallback, null);
        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }


    CameraDevice.StateCallback deviceCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Logger.instance().Log(Logger.DEBUG, TAG, "deviceCallback.onOpened() startRecording");

            Surface surface = mSurfaceView.getHolder().getSurface();
            Logger.instance().Log(Logger.DEBUG, TAG, "surface: " + surface);

            List<Surface> surfaceList = Collections.singletonList(surface);

            try {
                camera.createCaptureSession(surfaceList, sessionCallback, null);
            } catch (CameraAccessException e) {
                Logger.instance().Log(Logger.ERROR, TAG, "couldn't create capture session for camera: " + camera.getId(), e);
                return;
            }

        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Logger.instance().Log(Logger.DEBUG, TAG, "deviceCallback.onDisconnected() startRecording");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Logger.instance().Log(Logger.DEBUG, TAG, "deviceCallback.onError() startRecording");
        }

    };

    CameraCaptureSession.StateCallback sessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Logger.instance().Log(Logger.INFO, TAG, "capture session configured: " + session);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Logger.instance().Log(Logger.ERROR, TAG, "capture session configure failed: " + session);
        }
    };

    String getFrontFacingCameraId(CameraManager cManager){
        try {
            for(final String cameraId : cManager.getCameraIdList()){
                CameraCharacteristics characteristics = _cameraManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
            }
        } catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return null;
    }

    private void releaseCameraAndPreview() {
        mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }*/
}