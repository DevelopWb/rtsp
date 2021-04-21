package org.easydarwin.push;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.orhanobut.hawk.Hawk;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;

import org.easydarwin.audio.AudioStream;
import org.easydarwin.homepage.BackgroundCameraService;
import org.easydarwin.easypusher.BuildConfig;
import org.easydarwin.MyApp;
import org.easydarwin.homepage.StreamActivity;
import org.easydarwin.muxer.EasyMuxer;
import org.easydarwin.sw.JNIUtil;
import org.easydarwin.util.SPUtil;
import org.easydarwin.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar;

public class MediaStream {
    private static final boolean VERBOSE = BuildConfig.DEBUG;
    private static final int SWITCH_CAMERA = 11;
    private final boolean enanleVideo;
    Pusher mEasyPusher;
    static final String TAG = "MediaStream";
    public static int nativeWidth = 1920, nativeHeight = 1080;
    public static int uvcWidth = 1920, uvcHeight = 1080;//uvcCamera的宽高
    int framerate, bitrate;
    MediaCodec mMediaCodec;
    WeakReference<SurfaceTexture> mSurfaceHolderRef;
    Camera mCamera;
    boolean pushStream = false;//是否要推送数据
    final AudioStream audioStream = AudioStream.getInstance();
    private int mDgree;
    private Context mContext;
    private boolean mSWCodec;
    private VideoConsumer mVideoC, mRecordVC;
    private EasyMuxer mMuxer;
    private final HandlerThread mCameraThread;
    private final Handler mCameraHandler;
    //    private int previewFormat;
    public static CodecInfo info = new CodecInfo();
    private byte[] i420_buffer;
    private int frameWidth;
    private int frameHeight;
    private Camera.CameraInfo camInfo;
    private int mCameraId;
    public static final int CAMERA_FACING_BACK = 0;//后置
    public static final int CAMERA_FACING_FRONT = 1;
    public static final int CAMERA_FACING_BACK_UVC = 2;
    private UVCCamera uvcCamera;

    public MediaStream(Context context, SurfaceTexture texture) {
        this(context, texture, true);
    }

    public MediaStream(Context context, SurfaceTexture texture, boolean enableVideo) {
        mContext = context;
        mCameraId = SPUtil.getScreenPushingCameraIndex(context);
        mSurfaceHolderRef = new WeakReference(texture);

        mCameraThread = new HandlerThread("CAMERA") {
            @Override
            public void run() {
                try {
                    super.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                    Intent intent = new Intent(mContext, BackgroundCameraService.class);
                    mContext.stopService(intent);
                } finally {
                    stopStream();
                    stopPreview();
                    destroyCamera();
                }
            }
        };
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == SWITCH_CAMERA) {
                    switchCameraTask.run();
                }
            }
        };
        this.enanleVideo = enableVideo;

        if (enableVideo) {
            previewCallback = new Camera.PreviewCallback() {

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (data == null)
                        return;

                    int oritation =0;
                    if (!StreamActivity.IS_VERTICAL_SCREEN) {
                        oritation = 0;
                    } else {
                        if (mCameraId ==CAMERA_FACING_FRONT) {
                            oritation = 270;
                        }else {
                            oritation = 90;
                        }
                    }
                    if (i420_buffer == null || i420_buffer.length != data.length) {
                        i420_buffer = new byte[data.length];
                    }

                    JNIUtil.ConvertToI420(data, i420_buffer, nativeWidth, nativeHeight, 0, 0, nativeWidth,
                            nativeHeight, oritation, 2);
                    System.arraycopy(i420_buffer, 0, data, 0, data.length);

                    if (mRecordVC != null) {
                        mRecordVC.onVideo(i420_buffer, 0);
                    }

                    mVideoC.onVideo(data, 0);
                    mCamera.addCallbackBuffer(data);
                }
            };
        }
    }

    public void startStream(String url, InitCallback callback) {
        if (SPUtil.getEnableVideo(MyApp.getEasyApplication())) {
            mEasyPusher.initPush(url, mContext, callback);
        } else {
            mEasyPusher.initPush(url, mContext, callback, ~0);
        }
        pushStream = true;
    }

    public void startStream(String ip, String port, String id, InitCallback callback) {
        mEasyPusher.initPush(mContext, callback);
        mEasyPusher.setMediaInfo(Pusher.Codec.EASY_SDK_VIDEO_CODEC_H264, 25, Pusher.Codec.EASY_SDK_AUDIO_CODEC_AAC, 1
                , 8000, 16);
        mEasyPusher.start(ip, port, String.format("%s.sdp", id), Pusher.TransType.EASY_RTP_OVER_TCP);
        pushStream = true;
    }

    public void setDgree(int dgree) {
        mDgree = dgree;
    }

    /**
     * 更新分辨率
     */
    public void updateResolution() {
        if (mCamera == null) {
            return;
        }
        stopPreview();
        destroyCamera();
        createCamera();
        startPreview();
    }


    public static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();
            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        return maxFps;
    }

    public void createCamera() {

        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                    createCamera();
                }
            });
            return;
        }
        mEasyPusher = new EasyPusher();
        if (!enanleVideo) {
            return;
        }
        Log.d(UVCCameraService.TAG, "createCamera+++"+mCameraId);
        if (mCameraId == CAMERA_FACING_BACK_UVC) {
            createUvcCamera();
        } else {
            createNativeCamera();
        }
    }

    /**
     * uvc 第一步是创建camera
     */
    private void createUvcCamera() {
        boolean mHevc = false;//是否265编码  默认不能
        ArrayList<CodecInfo> infos = listEncoders(mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC :
                MediaFormat.MIMETYPE_VIDEO_AVC);

        if (!infos.isEmpty()) {
            CodecInfo ci = infos.get(0);
            info.mName = ci.mName;
            info.mColorFormat = ci.mColorFormat;
        } else {
            mSWCodec = true;
        }

        uvcWidth = Hawk.get(HawkProperty.KEY_UVC_WIDTH, uvcWidth);
        uvcHeight = Hawk.get(HawkProperty.KEY_UVC_HEIGHT, uvcHeight);
        Log.d(UVCCameraService.TAG, "createuvc"+"otg宽" + uvcWidth + "otg高" + uvcHeight);
        uvcCamera = UVCCameraService.liveData.getValue();
        if (uvcCamera != null) {
            try {
                //                uvcCamera.setPreviewSize(DisplayUtil.dp2px(context,300), DisplayUtil.dp2px(context,
                //                300), 1, 30, UVCCamera.FRAME_FORMAT_MJPEG, 1.0f);
                uvcCamera.setPreviewSize(uvcWidth, uvcHeight, 1, 30, UVCCamera.FRAME_FORMAT_MJPEG, 1.0f);
            } catch (final IllegalArgumentException e) {
                try {
                    // fallback to YUV mode
                    uvcCamera.setPreviewSize(uvcWidth, uvcHeight, 1, 30, UVCCamera.DEFAULT_PREVIEW_MODE, 1.0f);
                } catch (final IllegalArgumentException e1) {
                    if (uvcCamera != null) {
                        uvcCamera.destroy();
                        uvcCamera = null;
                    }
                }
            }
        }

        if (uvcCamera == null) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            createNativeCamera();
        }
    }

    /**
     * uvc 第二步 开始预览
     */
    private void startUvcPreview() {

        SurfaceTexture holder = mSurfaceHolderRef.get();
        if (holder != null) {
            uvcCamera.setPreviewTexture(holder);
        }
        Log.d(UVCCameraService.TAG, "startUvcPreview"+"otg宽" + uvcWidth + "otg高" + uvcHeight);

        try {
            uvcCamera.setFrameCallback(uvcFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP/*UVCCamera.PIXEL_FORMAT_NV21
               之前选的4*/);
            uvcCamera.startPreview();
            //            frameWidth = StreamActivity.IS_VERTICAL_SCREEN ? uvcHeight : uvcWidth;
            //            frameHeight = StreamActivity.IS_VERTICAL_SCREEN ? uvcWidth/2 : uvcHeight;
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private void createNativeCamera() {
        try {
            mCamera = Camera.open(mCameraId);// 初始化创建Camera实例对象
            mCamera.setErrorCallback((i, camera) -> {
                throw new IllegalStateException("Camera Error:" + i);
            });
            Log.i(TAG, "open Camera");

            Log.i(TAG, "setDisplayOrientation");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            //            String stack = sw.toString();
            destroyCamera();
            e.printStackTrace();
        }
    }


    // 根据Unicode编码完美的判断中文汉字和符号
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }


    public synchronized void startRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    startRecord();
                }
            });
            return;
        }
        boolean rotate = false;
        if (mCamera == null&& uvcCamera == null) {
            return;
        }
        long millis = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("record_interval",
                300000);
        mMuxer =
                new EasyMuxer(new File(recordPath, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toString(), millis);
        mRecordVC = new RecordVideoConsumer(mContext, mSWCodec, mMuxer);
        //        mRecordVC.onVideoStart(frameWidth, frameHeight);
        if (uvcCamera != null) {
            mRecordVC.onVideoStart(uvcWidth, uvcHeight);
        } else {
            mRecordVC.onVideoStart(StreamActivity.IS_VERTICAL_SCREEN ? nativeHeight : nativeWidth,
                    StreamActivity.IS_VERTICAL_SCREEN ? nativeWidth : nativeHeight);
        }
        if (audioStream != null) {
            audioStream.setMuxer(mMuxer);
        }

    }


    public synchronized void stopRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopRecord();
                }
            });
            return;
        }
        if (mRecordVC == null || audioStream == null) {
            //            nothing
        } else {
            audioStream.setMuxer(null);
            mRecordVC.onVideoStop();
            mRecordVC = null;
        }
        if (mMuxer != null) {
            mMuxer.release();
        }
        mMuxer = null;
    }

    /**
     * 第二步 开启预览
     */
    public synchronized void startPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            });
            return;
        }

        if (uvcCamera != null) {
            startUvcPreview();
            initConsumer(uvcWidth, uvcHeight);
        } else if (mCamera != null) {

            startCameraPreview();
            initConsumer(frameWidth, frameHeight);
        }
        audioStream.addPusher(mEasyPusher);
    }

    private void startCameraPreview() {
        Camera.Parameters parameters = mCamera.getParameters();

        if (Util.getSupportResolution(mContext).size() == 0) {
            StringBuilder stringBuilder = new StringBuilder();

            // 查看支持的预览尺寸
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

            for (Camera.Size str : supportedPreviewSizes) {
                stringBuilder.append(str.width + "x" + str.height).append(";");
            }

            Util.saveSupportResolution(mContext, stringBuilder.toString());
        }

        //        BUSUtil.BUS.post(new SupportResolution());

        camInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, camInfo);
        int cameraRotationOffset = camInfo.orientation;

        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
            cameraRotationOffset += 180;

        int rotate = (360 + cameraRotationOffset - mDgree) % 360;
        parameters.setRotation(rotate); // 设置Camera预览方向
        //            parameters.setRecordingHint(true);
        boolean mHevc = false;
        ArrayList<CodecInfo> infos = listEncoders(mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC :
                MediaFormat.MIMETYPE_VIDEO_AVC);

        if (!infos.isEmpty()) {
            CodecInfo ci = infos.get(0);
            info.mName = ci.mName;
            info.mColorFormat = ci.mColorFormat;
        } else {
            mSWCodec = true;
        }
        nativeWidth = Hawk.get(com.juntai.wisdom.basecomponent.utils.HawkProperty.KEY_NATIVE_WIDTH, nativeWidth);
        nativeHeight = Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT, nativeHeight);
        //            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        parameters.setPreviewSize(nativeWidth, nativeHeight);// 设置预览尺寸

        int[] ints = determineMaximumSupportedFramerate(parameters);
        parameters.setPreviewFpsRange(ints[0], ints[1]);

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();

        if (supportedFocusModes == null)
            supportedFocusModes = new ArrayList<>();

        // 自动对焦
        if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }


        mCamera.setParameters(parameters);
        Log.i(TAG, "setParameters");

        int displayRotation;
        displayRotation = (cameraRotationOffset - mDgree + 360) % 360;
        mCamera.setDisplayOrientation(displayRotation);

        int previewFormat = parameters.getPreviewFormat();

        Camera.Size previewSize = parameters.getPreviewSize();
        int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.setPreviewCallbackWithBuffer(previewCallback);

        Log.i(TAG, "setPreviewCallbackWithBuffer");

        try {
            // TextureView的
            SurfaceTexture holder = mSurfaceHolderRef.get();

            // SurfaceView传入上面创建的Camera对象
            if (holder != null) {
                mCamera.setPreviewTexture(holder);
                Log.i(TAG, "setPreviewTexture");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        if (!StreamActivity.IS_VERTICAL_SCREEN) {
            mCamera.setDisplayOrientation(0);
        } else {
            mCamera.setDisplayOrientation(90);
        }
        frameWidth = StreamActivity.IS_VERTICAL_SCREEN ? nativeHeight : nativeWidth;
        frameHeight = StreamActivity.IS_VERTICAL_SCREEN ? nativeWidth : nativeHeight;
    }


    private void initConsumer(int width, int height) {
        //        mSWCodec = Hawk.get(HawkProperty.KEY_SW_CODEC, true);
        mSWCodec = false;
        if (mSWCodec) {
            mVideoC = new ClippableVideoConsumer(mContext, new SWConsumer(mContext,
                    mEasyPusher), width, height);
        } else {
            mVideoC = new ClippableVideoConsumer(mContext, new HWConsumer(mContext,
                    mEasyPusher), width, height);
        }
        mVideoC.onVideoStart(width, height);

    }

    @Nullable
    public EasyMuxer getMuxer() {
        return mMuxer;
    }


    Camera.PreviewCallback previewCallback;


    /**
     * 旋转YUV格式数据
     *
     * @param src    YUV数据
     * @param format 0，420P；1，420SP
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    private static void yuvRotate(byte[] src, int format, int width, int height, int degree) {
        int offset = 0;
        if (format == 0) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += (width * height);
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
            offset += width * height / 4;
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
        } else if (format == 1) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += width * height;
            JNIUtil.rotateShortMatrix(src, offset, width / 2, height / 2, degree);
        }
    }

    /**
     * 停止预览
     */
    public synchronized void stopPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopPreview();
                }
            });
            return;
        }
        Log.d(UVCCameraService.TAG, "mediaCamera++++++++stopPreview:");
        if (uvcCamera != null) {
            uvcCamera.stopPreview();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            Log.i(TAG, "StopPreview");
        }
        if (audioStream != null) {
            audioStream.removePusher(mEasyPusher);
            Log.i(TAG, "Stop AudioStream");
            audioStream.setMuxer(null);
        }
        if (mVideoC != null) {
            mVideoC.onVideoStop();

            Log.i(TAG, "Stop VC");
        }
        if (mRecordVC != null) {
            mRecordVC.onVideoStop();
        }

//        if (mMuxer != null) {
//            mMuxer.release();
//            mMuxer = null;
//        }
    }

    public Camera getCamera() {
        return mCamera;
    }


    /**
     * 切换前后摄像头
     * CAMERA_FACING_BACK_LOOP                 循环切换摄像头
     * Camera.CameraInfo.CAMERA_FACING_BACK    后置摄像头
     * Camera.CameraInfo.CAMERA_FACING_FRONT   前置摄像头
     * CAMERA_FACING_BACK_UVC                  UVC摄像头
     */
    public void switchCamera(int cameraId) {
        mCameraId = cameraId;
        if (mCameraHandler.hasMessages(SWITCH_CAMERA)) {
            return;
        } else {
            mCameraHandler.sendEmptyMessage(SWITCH_CAMERA);
        }
    }


    private Runnable switchCameraTask = new Runnable() {
        @Override
        public void run() {
            Log.d(UVCCameraService.TAG, "mediaStream====run==switchCameraTask:");
            int cameraCount = 0;
            if (!enanleVideo) {
                return;
            }
            if (mCameraId == CAMERA_FACING_BACK_UVC) {
                if (uvcCamera != null) {
                    return;
                }
            }
            stopPreview();
            destroyCamera();
            createCamera();
            startPreview();
        }
    };

    private String recordPath = Environment.getExternalStorageDirectory().getPath();

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    /**
     * 销毁Camera
     */
    public synchronized void destroyCamera() {

        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    destroyCamera();
                }
            });
            return;
        }
        Log.d(UVCCameraService.TAG, "destroyCamera:");
        if (mCamera != null) {
            mCamera.stopPreview();
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "release Camera");
            mCamera = null;
        }
        if (uvcCamera != null) {
            uvcCamera.destroy();
            uvcCamera = null;
        }

        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    public boolean isStreaming() {
        return pushStream;
    }


    public void stopStream() {
        mEasyPusher.stop();
        pushStream = false;
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        mSurfaceHolderRef = new WeakReference<SurfaceTexture>(texture);
    }

    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mCameraThread.quitSafely();
        } else {
            if (!mCameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraThread.quit();
                }
            })) {
                mCameraThread.quit();
            }
        }
        try {
            mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return mMuxer != null;
    }


    public static class CodecInfo {
        public String mName;
        public int mColorFormat;
    }

    public static ArrayList<CodecInfo> listEncoders(String mime) {
        // 可能有多个编码库，都获取一下。。。
        ArrayList<CodecInfo> codecInfos = new ArrayList<CodecInfo>();
        int numCodecs = MediaCodecList.getCodecCount();
        // int colorFormat = 0;
        // String name = null;
        for (int i1 = 0; i1 < numCodecs; i1++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i1);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            if (codecMatch(mime, codecInfo)) {
                String name = codecInfo.getName();
                int colorFormat = getColorFormat(codecInfo, mime);
                if (colorFormat != 0) {
                    CodecInfo ci = new CodecInfo();
                    ci.mName = name;
                    ci.mColorFormat = colorFormat;
                    codecInfos.add(ci);
                }
            }
        }
        return codecInfos;
    }

    public static boolean codecMatch(String mimeType, MediaCodecInfo codecInfo) {
        String[] types = codecInfo.getSupportedTypes();
        for (String type : types) {
            if (type.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    public static int getColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int[] cf = new int[capabilities.colorFormats.length];
        System.arraycopy(capabilities.colorFormats, 0, cf, 0, cf.length);
        List<Integer> sets = new ArrayList<>();
        for (int i = 0; i < cf.length; i++) {
            sets.add(cf[i]);
        }
        if (sets.contains(COLOR_FormatYUV420SemiPlanar)) {
            return COLOR_FormatYUV420SemiPlanar;
        } else if (sets.contains(COLOR_FormatYUV420Planar)) {
            return COLOR_FormatYUV420Planar;
        } else if (sets.contains(COLOR_FormatYUV420PackedPlanar)) {
            return COLOR_FormatYUV420PackedPlanar;
        } else if (sets.contains(COLOR_TI_FormatYUV420PackedSemiPlanar)) {
            return COLOR_TI_FormatYUV420PackedSemiPlanar;
        }
        return 0;
    }


    /* ============================== UVC Camera ============================== */


    BlockingQueue<byte[]> cache = new ArrayBlockingQueue<byte[]>(100);

    final IFrameCallback uvcFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            if (uvcCamera == null)
                return;

            Thread.currentThread().setName("UVCCamera");
            frame.clear();

            byte[] data = cache.poll();
            if (data == null) {
                data = new byte[frame.capacity()];
            }

            frame.get(data);

            //            bufferQueue.offer(data);
            //            mCameraHandler.post(dequeueRunnable);

            onUvcCameraPreviewFrame(data, uvcCamera);
        }
    };

    public void onUvcCameraPreviewFrame(byte[] data, Object camera) {
        if (data == null)
            return;

        if (i420_buffer == null || i420_buffer.length != data.length) {
            i420_buffer = new byte[data.length];
        }

        JNIUtil.ConvertToI420(data, i420_buffer, uvcWidth, uvcHeight, 0, 0, uvcWidth, uvcHeight, 0, 2);
        System.arraycopy(i420_buffer, 0, data, 0, data.length);

        if (mRecordVC != null) {
            mRecordVC.onVideo(i420_buffer, 0);
        }
        mVideoC.onVideo(data, 0);

    }
}