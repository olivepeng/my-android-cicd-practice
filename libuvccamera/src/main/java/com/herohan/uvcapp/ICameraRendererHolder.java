package com.herohan.uvcapp;

import com.libuvccamera.opengl.renderer.IRendererHolder;

interface ICameraRendererHolder extends IRendererHolder {

    /**
     * capture still picture
     * blocking for capture to complete
     */
    void captureImage(OnImageCapturedCallback callback);

    interface OnImageCapturedCallback{
        void onCaptureSuccess(ImageRawData image);
    }
}
