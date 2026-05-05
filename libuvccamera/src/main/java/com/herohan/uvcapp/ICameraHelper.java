package com.herohan.uvcapp;

import android.hardware.usb.UsbDevice;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

import com.libuvccamera.usb.Format;
import com.libuvccamera.usb.IButtonCallback;
import com.libuvccamera.usb.IFrameCallback;
import com.libuvccamera.usb.Size;
import com.libuvccamera.usb.UVCControl;
import com.libuvccamera.usb.UVCParam;

import java.util.List;

public interface ICameraHelper {

    void setStateCallback(StateCallback callback);

    @Nullable
    List<UsbDevice> getDeviceList();

    void selectDevice(UsbDevice device);

    List<Format> getSupportedFormatList();

    List<Size> getSupportedSizeList();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    void addSurface(Object surface, boolean isRecordable);

    void removeSurface(Object surface);

    void setButtonCallback(IButtonCallback callback);

    void setFrameCallback(IFrameCallback callback, int pixelFormat);

    void openCamera();

    void openCamera(Size size);

    void openCamera(UVCParam param);

    void closeCamera();

    void startPreview();

    void stopPreview();

    UVCControl getUVCControl();

    void takePicture(
            @FloatRange(from = 1) float zoom,
            ImageCapture.OutputFileOptions options,
            ImageCapture.OnImageCaptureCallback callback);

    boolean isRecording();

    void startRecording(VideoCapture.OutputFileOptions options,
                        VideoCapture.OnVideoCaptureCallback callback);

    void stopRecording();

    boolean isCameraOpened();

    void release();

    void releaseAll();

    /**
     * Returns the current preview settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setPreviewConfig(CameraPreviewConfig)} to take effect.
     */
    CameraPreviewConfig getPreviewConfig();

    /**
     * Changes the preview  settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setPreviewConfig(CameraPreviewConfig config);

    /**
     * Returns the current ImageCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setImageCaptureConfig(ImageCaptureConfig)} to take effect.
     */
    ImageCaptureConfig getImageCaptureConfig();

    /**
     * Changes the ImageCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setImageCaptureConfig(ImageCaptureConfig config);

    /**
     * Returns the current VideoCapture settings for this Camera.
     * If modifications are made to the returned Config, they must be passed
     * to {@link #setVideoCaptureConfig(VideoCaptureConfig)} to take effect.
     */
    VideoCaptureConfig getVideoCaptureConfig();

    /**
     * Changes the VideoCapture settings for this Camera.
     *
     * @param config the Parameters to use for this Camera
     */
    void setVideoCaptureConfig(VideoCaptureConfig config);

    interface StateCallback {
        void onAttach(UsbDevice device);

        void onDeviceOpen(UsbDevice device, boolean isFirstOpen);

        void onCameraOpen(UsbDevice device);

        void onCameraClose(UsbDevice device);

        void onDeviceClose(UsbDevice device);

        void onDetach(UsbDevice device);

        void onCancel(UsbDevice device);
    }
}