package com.herohan.uvcapp;

import androidx.annotation.FloatRange;

import com.libuvccamera.usb.Format;
import com.libuvccamera.usb.IButtonCallback;
import com.libuvccamera.usb.IFrameCallback;
import com.libuvccamera.usb.Size;
import com.libuvccamera.usb.UVCControl;
import com.libuvccamera.usb.UVCParam;

import java.util.List;

interface ICameraInternal {

    void registerCallback(StateCallback callback);

    void unregisterCallback(final StateCallback callback);

    void clearCallbacks();

    List<Format> getSupportedFormatList();

    List<Size> getSupportedSizeList();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    void addSurface(final Object surface, final boolean isRecordable);

    void removeSurface(final Object surface);

    void setButtonCallback(final IButtonCallback callback);

    void setFrameCallback(final IFrameCallback callback, final int pixelFormat);

    void openCamera(UVCParam param,
                    CameraPreviewConfig previewConfig,
                    ImageCaptureConfig imageCaptureConfig,
                    VideoCaptureConfig videoCaptureConfig);

    void closeCamera();

    void startPreview();

    void stopPreview();

    UVCControl getUVCControl();

    void takePicture(@FloatRange(from = 1) float zoom,
                     ImageCapture.OutputFileOptions options,
                     ImageCapture.OnImageCaptureCallback callback);

    boolean isRecording();

    void startRecording(VideoCapture.OutputFileOptions options,
                        VideoCapture.OnVideoCaptureCallback callback);

    void stopRecording();

    boolean isCameraOpened();

    void release();

    void setPreviewConfig(CameraPreviewConfig config);

    void setImageCaptureConfig(ImageCaptureConfig config);

    void setVideoCaptureConfig(VideoCaptureConfig config);

    interface StateCallback {
        void onCameraOpen();

        void onCameraClose();
    }
}
