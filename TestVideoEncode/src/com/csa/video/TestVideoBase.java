package com.csa.video;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.avcodec.AVCodec;
import com.googlecode.javacv.cpp.avcodec.AVCodecContext;
import com.googlecode.javacv.cpp.avcodec.AVFrame;
import com.googlecode.javacv.cpp.avcodec.AVPacket;
import com.googlecode.javacv.cpp.avcodec.AVPicture;
import com.googlecode.javacv.cpp.avformat;
import com.googlecode.javacv.cpp.avformat.AVFormatContext;
import com.googlecode.javacv.cpp.avformat.AVOutputFormat;
import com.googlecode.javacv.cpp.avformat.AVStream;
import com.googlecode.javacv.cpp.avutil;
import com.googlecode.javacv.cpp.avutil.AVRational;
import com.googlecode.javacv.cpp.swscale.SwsContext;

import android.app.Activity;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
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
    private Pointer video_outbuf;
    private int video_outbuf_size;
    //private AVOutputFormat fmt;
    private AVFormatContext oc;
    private AVCodecContext c;
    private AVFrame coded_frame;
    private AVOutputFormat oformat;
    private AVStream video_st;
    private SwsContext img_convert_ctx;
    private AVPacket pkt = new AVPacket();
    //private PointerByReference p = new PointerByReference();
    public static final int DEFAULT_FRAME_RATE_BASE = 1001000;

	public TestVideoBase() throws Exception
	{
		avformat.av_register_all();
		
//		fmt = avformat.av_guess_format(null, "h.3gp", null);
//		if (fmt == null) throw new Exception("Couldn't find format");

		oc = avformat.avformat_alloc_context();
		if (oc == null) throw new Exception("Couldn't alloc context.");
		//oc.oformat(fmt);
		
		video_st = avformat.av_new_stream(oc, 0);
		if (video_st == null) throw new Exception("Couldn't alloc stream.");

		c = new AVCodecContext(video_st.codec());
		if (c == null) throw new Exception("Couldn't get context.");
		c.codec_id(avcodec.CODEC_ID_H263);
		c.codec_type(avcodec.CODEC_TYPE_VIDEO);
		c.bit_rate(400000);
		c.width(352);
		c.height(288);
		c.time_base(avutil.av_d2q(1/30, DEFAULT_FRAME_RATE_BASE));
		c.gop_size(12);
		c.pix_fmt(avutil.PIX_FMT_YUV420P);
		c.flags(c.flags() | avcodec.CODEC_FLAG_GLOBAL_HEADER);
		
		if (avformat.av_set_parameters(oc, null) < 0) throw new Exception("Invalid output format parameters");
		
		AVCodec codec = avcodec.avcodec_find_encoder(c.codec_id());
		if (codec == null) throw new Exception("Codec not found!");
		if (avcodec.avcodec_open(c, codec) < 0) throw new Exception("Could not open codec.");
		
		coded_frame = new AVFrame(c.coded_frame());
		
		picture = avcodec.avcodec_alloc_frame();
		if (picture == null)
		{
			avcodec.avcodec_close(c);
			avutil.av_freep(video_st);
			video_st = null;
			throw new Exception("Could not allocate picture");
		}
		int size = avcodec.avpicture_get_size(c.pix_fmt(), c.width(), c.height());
		Pointer picture_buf = avutil.av_malloc(size);
		if (picture_buf == null)
		{
			avutil.av_free(picture);
			picture = null;
		}
		else
		{
			avcodec.avpicture_fill((AVPicture)picture, (BytePointer)picture_buf, c.pix_fmt(), c.width(), c.height());
		}
		avformat.av_write_header(oc);
		
		
		return;
//		//Instantiate encoders/decoders, set up screen for surface
//		avcodec.avcodec_register_all();
//		avcodec.avcodec_init();
//		avformat.av_register_all();
//		//avformat.av_set_parameters(mCodecCtx, null);
//		
//		mCodec = avcodec.avcodec_find_encoder(avcodec.CODEC_ID_H263);
//		if (mCodec == null) return;
//		
//		mCodecCtx = avcodec.avcodec_alloc_context();
//		mCodecCtx.bit_rate(300000);
//		mCodecCtx.codec(mCodec);
//		mCodecCtx.width(VIDEO_WIDTH);
//		mCodecCtx.height(VIDEO_HEIGHT);
//		mCodecCtx.pix_fmt(avutil.PIX_FMT_YUV420P);
//		mCodecCtx.codec_id(avcodec.CODEC_ID_H263);
//		mCodecCtx.codec_type(avcodec.CODEC_TYPE_VIDEO);
//
//		mRatio = new AVRational();
//		mRatio.num(1);
//		mRatio.den(VIDEO_FPS);
//		mCodecCtx.time_base(mRatio);
//		mCodecCtx.coder_type(1);
//		mCodecCtx.flags(mCodecCtx.flags() | avcodec.CODEC_FLAG_LOOP_FILTER);
//		mCodecCtx.me_cmp(avcodec.FF_LOSS_CHROMA);
//		mCodecCtx.me_method(avcodec.ME_HEX);
//		mCodecCtx.me_subpel_quality(6);
//		mCodecCtx.me_range(16);
//		mCodecCtx.gop_size(30);
//		mCodecCtx.keyint_min(10);
//		mCodecCtx.scenechange_threshold(40);
//		mCodecCtx.i_quant_factor((float) 0.71);
//		mCodecCtx.b_frame_strategy(1);
//		mCodecCtx.qcompress((float) 0.6);
//		mCodecCtx.qmin(10);
//		mCodecCtx.qmax(51);
//		mCodecCtx.max_qdiff(4);
//		mCodecCtx.max_b_frames(1);
//		mCodecCtx.refs(2);
//		mCodecCtx.directpred(3);
//		mCodecCtx.trellis(1);
//		mCodecCtx.flags2(mCodecCtx.flags2() | avcodec.CODEC_FLAG2_BPYRAMID | avcodec.CODEC_FLAG2_WPRED | avcodec.CODEC_FLAG2_8X8DCT | avcodec.CODEC_FLAG2_FASTPSKIP);
//
//		if (avcodec.avcodec_open(mCodecCtx, mCodec) == 0) return;
//
//		mFrameSize = avcodec.avpicture_get_size(avutil.PIX_FMT_YUV420P, VIDEO_WIDTH, VIDEO_HEIGHT);
//		mFrame = avcodec.avcodec_alloc_frame();
//		mPacket = new AVPacket();
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
		if (!firstTime) return;
		firstTime = false;

		if (data == null)
		{
			Log.d("TEST_VIDEO", "No data");
			return;
		}
		
		Camera.Parameters params = camera.getParameters();
		Size previewSize = params.getPreviewSize();
		byte[] data420 = new byte[data.length];
		convert_yuv422_to_yuv420(data, data420, previewSize.width, previewSize.height);
		
		if (video_st == null)
		{
			try {
				throw new Exception ("No video output stream.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (picture == null)
		{
			Log.d("TEST_VIDEO", "No picture.");
			return;
		}
		
		picture.data(0, new BytePointer(data420));
		picture.linesize(0, previewSize.width * 3);
		
		int out_size = avcodec.avcodec_encode_video(c, (BytePointer) video_outbuf, video_outbuf_size, picture);
		if (out_size > 0)
		{
			Log.d("TEST_VIDEO", "out_size = '" + out_size + "'");
			avcodec.av_init_packet(pkt);
			if (coded_frame.pts() != avcodec.AV_NOPTS_VALUE)
			{
				pkt.pts(avutil.av_rescale_q(coded_frame.pts(), c.time_base(), video_st.time_base()));
			}
			if (coded_frame.key_frame() != 0)
			{
				pkt.flags(pkt.flags() | avcodec.PKT_FLAG_KEY);
			}
			pkt.stream_index(video_st.index());
			pkt.data((BytePointer) video_outbuf);
			pkt.size(out_size);
			Log.d("TEST_VIDEO", "write: " + avformat.av_write_frame(oc, pkt));
		}
		Log.d("TEST_VIDEO", "no out_size");
//		int bOutBuffSize = VIDEO_WIDTH * VIDEO_HEIGHT * 4;
//		BytePointer picPointer = new BytePointer(data);
//		BytePointer bBuffer = new BytePointer(avutil.av_malloc(bOutBuffSize));
//
//		if (avcodec.avpicture_fill((AVPicture)mFrame, picPointer, avutil.PIX_FMT_YUV420P, VIDEO_WIDTH, VIDEO_HEIGHT) <= 0)
//			return;
//
//		//encode the image
//		int size = avcodec.avcodec_encode_video(mCodecCtx, bBuffer, bOutBuffSize, mFrame);
//		
//		//Image was buffered
//		if (size == 0)
//			return;
//		
//		if (size > 0)
//		{
//			avcodec.av_init_packet(mPacket);
//			AVFrame coded_frame = mCodecCtx.coded_frame();
//			long pts = coded_frame.pts();
//			if (coded_frame.pts() != avcodec.AV_NOPTS_VALUE)
//				mPacket.pts(avutil.av_rescale_q(pts, mCodecCtx.time_base(), mCodecCtx.time_base()));
//			if (coded_frame.key_frame() != 0)
//				mPacket.flags(mPacket.flags() | avcodec.PKT_FLAG_KEY);
//			mPacket.stream_index(mPacket.stream_index() + 1);
//			mPacket.data(bBuffer);
//			mPacket.size(size);
//		}
//		Log.d("TEST_VIDEO", "Size encoded: '" + size + "'.");
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
