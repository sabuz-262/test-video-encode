package com.csa.video;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.avcodec.AVCodec;
import com.googlecode.javacv.cpp.avcodec.AVCodecContext;
import com.googlecode.javacv.cpp.avcodec.AVFrame;
import com.googlecode.javacv.cpp.avcodec.AVPacket;
import com.googlecode.javacv.cpp.avcodec.AVPicture;
import com.googlecode.javacv.cpp.avformat;
import com.googlecode.javacv.cpp.avutil;
import com.googlecode.javacv.cpp.avutil.AVRational;

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
	
	public TestVideoBase()
	{
		//Instantiate encoders/decoders, set up screen for surface
		avcodec.avcodec_register_all();
		avcodec.avcodec_init();
		avformat.av_register_all();
		//avformat.av_set_parameters(mCodecCtx, null);
		
		mCodec = avcodec.avcodec_find_encoder(avcodec.CODEC_ID_H263);
		if (mCodec == null) return;
		
		mCodecCtx = avcodec.avcodec_alloc_context();
		mCodecCtx.bit_rate(300000);
		mCodecCtx.codec(mCodec);
		mCodecCtx.width(VIDEO_WIDTH);
		mCodecCtx.height(VIDEO_HEIGHT);
		mCodecCtx.pix_fmt(avutil.PIX_FMT_YUV420P);
		mCodecCtx.codec_id(avcodec.CODEC_ID_H263);
		mCodecCtx.codec_type(avcodec.CODEC_TYPE_VIDEO);

		mRatio = new AVRational();
		mRatio.num(1);
		mRatio.den(VIDEO_FPS);
		mCodecCtx.time_base(mRatio);
		mCodecCtx.coder_type(1);
		mCodecCtx.flags(mCodecCtx.flags() | avcodec.CODEC_FLAG_LOOP_FILTER);
		mCodecCtx.me_cmp(avcodec.FF_LOSS_CHROMA);
		mCodecCtx.me_method(avcodec.ME_HEX);
		mCodecCtx.me_subpel_quality(6);
		mCodecCtx.me_range(16);
		mCodecCtx.gop_size(30);
		mCodecCtx.keyint_min(10);
		mCodecCtx.scenechange_threshold(40);
		mCodecCtx.i_quant_factor((float) 0.71);
		mCodecCtx.b_frame_strategy(1);
		mCodecCtx.qcompress((float) 0.6);
		mCodecCtx.qmin(10);
		mCodecCtx.qmax(51);
		mCodecCtx.max_qdiff(4);
		mCodecCtx.max_b_frames(1);
		mCodecCtx.refs(2);
		mCodecCtx.directpred(3);
		mCodecCtx.trellis(1);
		mCodecCtx.flags2(mCodecCtx.flags2() | avcodec.CODEC_FLAG2_BPYRAMID | avcodec.CODEC_FLAG2_WPRED | avcodec.CODEC_FLAG2_8X8DCT | avcodec.CODEC_FLAG2_FASTPSKIP);

		if (avcodec.avcodec_open(mCodecCtx, mCodec) == 0) return;

		mFrameSize = avcodec.avpicture_get_size(avutil.PIX_FMT_YUV420P, VIDEO_WIDTH, VIDEO_HEIGHT);
		mFrame = avcodec.avcodec_alloc_frame();
		mPacket = new AVPacket();
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

		int bOutBuffSize = VIDEO_WIDTH * VIDEO_HEIGHT * 4;
		BytePointer picPointer = new BytePointer(data);
		BytePointer bBuffer = new BytePointer(avutil.av_malloc(bOutBuffSize));

		if (avcodec.avpicture_fill((AVPicture)mFrame, picPointer, avutil.PIX_FMT_YUV420P, VIDEO_WIDTH, VIDEO_HEIGHT) <= 0)
			return;

		//encode the image
		int size = avcodec.avcodec_encode_video(mCodecCtx, bBuffer, bOutBuffSize, mFrame);
		
		//Image was buffered
		if (size == 0)
			return;
		
		if (size > 0)
		{
			avcodec.av_init_packet(mPacket);
			AVFrame coded_frame = mCodecCtx.coded_frame();
			long pts = coded_frame.pts();
			if (coded_frame.pts() != avcodec.AV_NOPTS_VALUE)
				mPacket.pts(avutil.av_rescale_q(pts, mCodecCtx.time_base(), mCodecCtx.time_base()));
			if (coded_frame.key_frame() != 0)
				mPacket.flags(mPacket.flags() | avcodec.PKT_FLAG_KEY);
			mPacket.stream_index(mPacket.stream_index() + 1);
			mPacket.data(bBuffer);
			mPacket.size(size);
		}
		Log.d("TEST_VIDEO", "Size encoded: '" + size + "'.");
	}
}
