package com.csa.video;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.avcodec.AVCodec;
import com.googlecode.javacv.cpp.avcodec.AVCodecContext;
import com.googlecode.javacv.cpp.avcodec.AVFrame;
import com.googlecode.javacv.cpp.avcodec.AVPacket;
import com.googlecode.javacv.cpp.avformat;
import com.googlecode.javacv.cpp.avformat.AVOutputFormat;
import com.googlecode.javacv.cpp.avformat.AVStream;
import com.googlecode.javacv.cpp.avutil;
import com.googlecode.javacv.cpp.avutil.AVRational;
import com.googlecode.javacv.cpp.swscale.SwsContext;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class TestVideoBase extends Activity implements Camera.PreviewCallback
{
	private final static int VIDEO_WIDTH = 352;
	private final static int VIDEO_HEIGHT = 288;
	private final static int VIDEO_FPS = 30;
	
	public enum VideoChannel {
		MainVideo(1), PresentationVideo(2);

		private final int channelId;

		VideoChannel(int id) {
			this.channelId = id;
		}

		public int getChannelNumber() {
			return channelId;
		}
	}


	TestVideoPreview mVideoPreview = null;
	TestVideoView mVideoView = null;
	
	AVCodec mCodec = null;
	AVCodecContext mCodecCtx = null;
	AVPacket mPacket = null;
	AVFrame mFrame = null;
	int mFrameSize = 0;
	AVRational mRatio = null;
	boolean firstTime = true;
	
    private AVFrame picture;
    private AVCodecContext c;
    private AVFrame coded_frame;
    private AVOutputFormat oformat;
    private AVStream video_st;
    private SwsContext img_convert_ctx;
    private AVPacket pkt = new AVPacket();
    //private PointerByReference p = new PointerByReference();
    public static final int DEFAULT_FRAME_RATE_BASE = 1001000;

    int i, out_size, size, x, y, output_buffer_size;
	//FILE            *file;
    Pointer output_buffer;
    Pointer picture_buffer;
    
	public TestVideoBase() throws Exception
	{
		avcodec.avcodec_register_all();
		avcodec.avcodec_init();
		avformat.av_register_all();

		/* Manual Variables */
		int             l;
		int             fps = 30;
		int             videoLength = 5;

		/* find the H263 video encoder */
		mCodec = avcodec.avcodec_find_encoder(avcodec.CODEC_ID_H263);
		if (mCodec == null) {
		    Log.d("TEST_VIDEO", "avcodec_find_encoder() run fail.");
		}

		mCodecCtx = avcodec.avcodec_alloc_context();
		picture = avcodec.avcodec_alloc_frame();

		/* put sample parameters */
		mCodecCtx.bit_rate(400000);
		/* resolution must be a multiple of two */
		mCodecCtx.width(VIDEO_WIDTH);
		mCodecCtx.height(VIDEO_HEIGHT);
		/* frames per second */
		AVRational avFPS = new AVRational();
		avFPS.num(1);
		avFPS.den(VIDEO_FPS);
		mCodecCtx.time_base(avFPS);
		mCodecCtx.pix_fmt(avutil.PIX_FMT_YUV420P);
		mCodecCtx.codec_id(avcodec.CODEC_ID_H263);
		mCodecCtx.codec_type(avutil.AVMEDIA_TYPE_VIDEO);

		/* open it */
		if (avcodec.avcodec_open(mCodecCtx, mCodec) < 0) {
		    Log.d("TEST_VIDEO", "avcodec_open() run fail.");
		}

//		const char* mfileName = (*env)->GetStringUTFChars(env, filename, 0);
//
//		file = fopen(mfileName, "wb");
//		if (!file) {
//		    LOGI("fopen() run fail.");
//		}
//
//		(*env)->ReleaseStringUTFChars(env, filename, mfileName);
//
		/* alloc image and output buffer */
		output_buffer_size = 100000;
		output_buffer = avutil.av_malloc(output_buffer_size);

		size = mCodecCtx.width() * mCodecCtx.height();
		picture_buffer = avutil.av_malloc((size * 3) / 2); /* size for YUV 420 */

		picture.data(0, new BytePointer(picture_buffer));
		picture.data(1, picture.data(0).position(size));
		picture.data(2, picture.data(1).position(size / 4));
		picture.linesize(0, mCodecCtx.width());
		picture.linesize(1, mCodecCtx.width() / 2);
		picture.linesize(2, mCodecCtx.width() / 2);

		for(l=0;l<videoLength;l++){
		    //encode 1 second of video
		    for(i=0;i<fps;i++) {
		        //prepare a dummy image YCbCr
		        //Y
		        for(y=0;y<mCodecCtx.height();y++) {
		            for(x=0;x<mCodecCtx.width();x++) {
		                picture.data(0).put((y * picture.linesize(0) + x), (byte)(x + y + i * 3));
		            }
		        }

		        //Cb and Cr
		        for(y=0;y<mCodecCtx.height()/2;y++) {
		            for(x=0;x<mCodecCtx.width()/2;x++) {
		                picture.data(1).put((y * picture.linesize(1) + x), (byte)(128 + y + i * 2));
		                picture.data(2).put((y * picture.linesize(2) + x), (byte)(64 + x + i * 5));
		            }
		        }

		        //encode the image
		        out_size = avcodec.avcodec_encode_video(mCodecCtx, new BytePointer(output_buffer), output_buffer_size, picture);
		        Log.d("TEST_VIDEO", "Encoded '" + out_size + "' bytes");
		        //fwrite(output_buffer, 1, out_size, file);
		    }

		    //get the delayed frames
		    for(; out_size > 0; i++) {
		        out_size = avcodec.avcodec_encode_video(mCodecCtx, new BytePointer(output_buffer), output_buffer_size, null);
		        Log.d("TEST_VIDEO", "Encoded '" + out_size + "' bytes");
		        //fwrite(output_buffer, 1, out_size, file);
		    }
		}
		avcodec.avcodec_close(mCodecCtx);
		avutil.av_free(mCodecCtx);
		avutil.av_free(picture);

		return;
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    	try 
    	{
        	mVideoPreview = new TestVideoPreview(this, this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	@Override
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		//convert byte[] first, then encode, then packetize
	}

	void convert_yuv422_to_yuv420(byte[] inBuff, byte[] outBuff, int width, int height)
	{
	    int i = 0, j = 0, k = 0;
	    int UOffset = width * height;
	    int VOffset = (width * height) * 5/4;
	    int line1 = 0, line2 = 0;
	    int m = 0, n = 0;
	    int y = 0, u = 0, v = 0;
	
	    u = UOffset;
	    v = VOffset;

	    for (i = 0, j = 1; i < height; i += 2, j += 2)
	    {
	        /* Input Buffer Pointer Indexes */
	        line1 = i * width * 2;
	        line2 = j * width * 2;

	        /* Output Buffer Pointer Indexes */
	        m = width * y;
	        y = y + 1;
	        n = width * y;
	        y = y + 1;

	        /* Scan two lines at a time */
	        for (k = 0; k < width*2; k += 4)
	        {
	            byte Y1, Y2, U, V;
	            byte Y3, Y4, U2, V2;

	            /* Read Input Buffer */
	            Y1 = inBuff[line1++];
	            U  = inBuff[line1++];
	            Y2 = inBuff[line1++];
	            V  = inBuff[line1++];

	            Y3 = inBuff[line2++];
	            U2 = inBuff[line2++];
	            Y4 = inBuff[line2++];
	            V2 = inBuff[line2++];

	            /* Write Output Buffer */
	            outBuff[m++] = Y1;
	            outBuff[m++] = Y2;

	            outBuff[n++] = Y3;
	            outBuff[n++] = Y4;

	            outBuff[u++] = (byte) ((U + U2)/2);
	            outBuff[v++] = (byte) ((V + V2)/2);
	        }
	    }

	}
}
