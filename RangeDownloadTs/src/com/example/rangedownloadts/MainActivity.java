package com.example.rangedownloadts;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.rangedownloadts.common.Const;
import com.example.rangedownloadts.common.Const.RangeDownloadInfo;
import com.example.rangedownloadts.db.RangeDownLoadDao;
import com.example.rangedownloadts.downloadtool.DownLoader;

/**
 * 断点续传测试 地址
 * 
 * @author mars_zhang
 * 
 */
public class MainActivity extends Activity {

	private String fileurl;

	private TextView tvdownprogress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tvdownprogress = (TextView) findViewById(R.id.tv_downprogress);
		/** 3.0之后的断点续传要加这个 */
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork()
				.penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
				.penaltyLog().penaltyDeath().build());

		init();
	}

	/** 线程下载完成回调 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
//				LoadInfo loadInfo= (LoadInfo) msg.obj;
//				tvdownprogress.setText(loadInfo.toString());
			}
		};
	};

	private DownLoader downLoader;

	/** 初始化下载准备 */
	private void init() {
		Log.v(Const.MSG_TAG, "--->下载地址:" + Const.RangeDownloadInfo.BASE_HTTP_URL);
		// 判断文件夹是否存在
		File dirfile = new File(RangeDownloadInfo.SD_PATH);
		if (!dirfile.exists()) {
			Log.v(Const.MSG_TAG, "--->创建文件夹:" + dirfile);
			dirfile.mkdir();
		}

		downLoader = new DownLoader(Const.RangeDownloadInfo.BASE_HTTP_URL,
				dirfile.getAbsolutePath() + "/"+Const.RangeDownloadInfo.filename, 10,
				handler, new RangeDownLoadDao(getApplicationContext()));

		downLoader.getDownInfo();
		
		
	}

	public void getmessage(View view) {
		new Thread(new Runnable() { 
			@Override
			public void run() { 
				Log.v("time", downLoader.getDownInfo().toString());
			}
		}).start();
	}

	public void download(View view) {
		if(downLoader.isdownloading()){
			return ;
		}
		downLoader.download();
	}

	public void stop(View view) {
		downLoader.pause();
	}

}
