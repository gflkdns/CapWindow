package com.miqt.catwindow;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class CatActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 212;
    static AtomicInteger oneScreenshot = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cata);
        final MediaProjectionManager projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        initView();

        findViewById(R.id.textView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = projectionManager.createScreenCaptureIntent();
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    private void initView() {
        WebView webview = (WebView) findViewById(R.id.webview);
        //启用支持javascript
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webview.loadUrl("https://miqt.github.io/jellyfish/");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handleScreenShotIntent(resultCode, data);
    }

    private synchronized void onScreenshotTaskBegan() {
        oneScreenshot.set(1);
    }

    private void handleScreenShotIntent(int resultCode, Intent data) {

        onScreenshotTaskBegan();
        final MediaProjectionManager projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final MediaProjection mProjection = projectionManager.getMediaProjection(resultCode, data);
        Point size = Utils.getScreenSize(this);
        final int mWidth = size.x;
        final int mHeight = size.y;
        final ImageReader mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat
                .RGBA_8888, 2);
        final VirtualDisplay display = mProjection.createVirtualDisplay("screen-mirror", mWidth,
                mHeight, DisplayMetrics.DENSITY_MEDIUM,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mImageReader.getSurface(),
                null, null);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader mImageReader) {

                Image image = null;
                try {
                    image = mImageReader.acquireLatestImage();
                    if (image != null) {
                        final Image.Plane[] planes = image.getPlanes();
                        if (planes.length > 0) {
                            final ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * mWidth;


                            // create bitmap
                            Bitmap bmp = Bitmap.createBitmap(mWidth + rowPadding / pixelStride,
                                    mHeight, Bitmap.Config.ARGB_8888);
                            bmp.copyPixelsFromBuffer(buffer);

                            Bitmap croppedBitmap = Bitmap.createBitmap(bmp, 0, 0, mWidth, mHeight);

                            saveBitmap(croppedBitmap);//保存图片

                            if (croppedBitmap != null) {
                                croppedBitmap.recycle();
                            }
                            if (bmp != null) {
                                bmp.recycle();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    if (mImageReader != null) {
                        mImageReader.close();
                    }
                    if (display != null) {
                        display.release();
                    }

                    mImageReader.setOnImageAvailableListener(null, null);
                    mProjection.stop();

                    onScreenshotTaskOver();
                }

            }
        }, getBackgroundHandler());
    }

    //在后台线程里保存文件
    Handler backgroundHandler;

    private Handler getBackgroundHandler() {
        if (backgroundHandler == null) {
            HandlerThread backgroundThread =
                    new HandlerThread("catwindow", android.os.Process
                            .THREAD_PRIORITY_BACKGROUND);
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
        return backgroundHandler;
    }

    private void saveBitmap(Bitmap bmp) throws IOException {
        File childFolder = Environment.getExternalStoragePublicDirectory(Environment
                .DIRECTORY_PICTURES);
        File imageFile = new File(childFolder.getAbsolutePath() + "/" + System.currentTimeMillis
                () + ".jpg");
        OutputStream fOut = new FileOutputStream(imageFile);
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, fOut);//将bg输出至文件
        fOut.flush();
        fOut.close(); // do not forget to close the stream
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile
                (imageFile)));
    }

    private synchronized void onScreenshotTaskOver() {
        oneScreenshot.set(0);
    }
}
