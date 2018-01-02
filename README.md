# CapWindow

Android截屏

最近在开发的过程中，遇到了一个需要截取屏幕保存为图片的需求，具体为截取webview的视图保存图片。


----------


**方法1：** 首先想到的思路是利用SDK提供的View.getDrawingCache()方法：

```
  public void printScreen(View view) {
        String imgPath = "/sdcard/test.png";
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        if (bitmap != null) {
            try {
                FileOutputStream out = new FileOutputStream(imgPath);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                        out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
```
这个方法在很多情况下都是没有问题的，比如说截取imageview，TextView，甚至otherview.getRootView();都没问题，但在WebView上就会出现webview的部分截取完缺少页面里的一些内容的情况，比如说用webview打开[这个](https://miqt.github.io/jellyfish/)界面，截取的图片就会有问题，具体表现为网页中游动的水母没有显示在截取的图片上。


----------


**方法2：**使用Android系统提供的服务Context.MEDIA_PROJECTION_SERVICE，进行截图操作。


关键部分代码解析：↓

发送截图请求
```
 final MediaProjectionManager projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
 Intent intent = projectionManager.createScreenCaptureIntent();
 startActivityForResult(intent, REQUEST_CODE);
```

接收返回的结果：

```

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handleScreenShotIntent(resultCode, data);
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

```
这个方法类似使用手机的系统截屏（音量下键+电源键），能够完美的吧当前原模原样的屏幕截取下来，并且修改保存方法的话甚至可以屏幕录像，但相比于第一种方法，它的缺点是完全和界面上的view没有关系，并且在调用这个服务的时候，会弹出一个权限确认的弹框。另外需要注意，这一方法只能在Android 5.0的系统设备上适用。

**总结：**

总而言之，这两种方法各有利弊，使用的时候要根据自己的实际需求做出选择。

