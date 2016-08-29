package com.miqt.tlchat.capwindow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final int CAP_WINDOW = 1;
    TextView iv_start;
    final String URL = "http://192.168.9.16:9090/examples/dsmviewer/app" +
            ".html?modelinfo_id=33832B1C57834E569EBC8BF31946E458&model_type=Model";
    MediaProjectionManager manager;
    private Handler threehandler;
    private Surface mSurface;
    private ImageReader.OnImageAvailableListener imageAvaListener = new ImageReader
            .OnImageAvailableListener() {


        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            threehandler.post(new ImageSaver(image));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_start = (TextView) findViewById(R.id.start);
        iv_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandlerThread handlerThread = new HandlerThread("caphandler");
                handlerThread.start();
                threehandler = new Handler(handlerThread.getLooper());
                ImageReader reader = ImageReader.newInstance(100, 100, ImageFormat.JPEG, 5);
                reader.setOnImageAvailableListener(imageAvaListener, threehandler);
                mSurface = reader.getSurface();
                manager = (MediaProjectionManager) getSystemService
                        (MEDIA_PROJECTION_SERVICE);
                Intent intent = manager.createScreenCaptureIntent();
                startActivityForResult(intent, CAP_WINDOW);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAP_WINDOW: {
                MediaProjection projection = manager.getMediaProjection(resultCode, data);
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int mScreenDensity = metrics.densityDpi;
                projection.createVirtualDisplay("capwindow", 100, 100, mScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mSurface, new
                                VirtualDisplay.Callback() {


                                    @Override
                                    public void onPaused() {
                                        super.onPaused();
                                    }

                                    @Override
                                    public void onResumed() {
                                        super.onResumed();
                                    }

                                    @Override
                                    public void onStopped() {
                                        super.onStopped();
                                    }
                                }, threehandler);
                break;
            }
        }
    }

    private class ImageSaver implements Runnable {
        Image reader;

        public ImageSaver(Image reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .getAbsoluteFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                ByteBuffer buffer = reader.getPlanes()[0].getBuffer();
                byte[] buff = new byte[buffer.remaining()];
                buffer.get(buff);
                BitmapFactory.Options ontain = new BitmapFactory.Options();
                ontain.inSampleSize = 50;
                outputStream.write(buff);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
