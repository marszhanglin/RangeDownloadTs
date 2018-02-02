package com.example.rangedownloadts.downloadtool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.example.rangedownloadts.common.Const;
import com.example.rangedownloadts.db.DownloadInfo;
import com.example.rangedownloadts.db.RangeDownLoadDao;
import com.example.rangedownloadts.mtms.HttpFileDownloadListener;
import com.example.rangedownloadts.mtms.HttpFileStatus;
import com.example.rangedownloadts.utils.DebugUtils;
import com.google.gson.Gson;

/**
 * 下载器
 * 
 * @author mars_zhang
 * 
 */
public class RangeDownLoader {
	private String urlstr;// 下载的地址
	private String localfile;// 保存路径
	private int threadcount;// 线程数
	private int downloadPresent=0;//下载进度
	private boolean isdownloadFail= false;//是否下载失败
	private HttpFileDownloadListener httpFileDownloadListener;// 消息处理器

	private RangeDownLoadDao dao;// 工具类
	private long fileSize;// 所要下载的文件的大小
	private long thresholdCount=0;//阈值计数0-100   当达到这个值时回调进度监听
	
	private List<DownloadInfo> infos;// 存放下载信息类的集合 分断点
	private static final int INIT = 1;// 定义三种下载的状态：初始化状态，正在下载状态，暂停状态
	private static final int DOWNLOADING = 2;
	private static final int PAUSE = 3;
	private int state = INIT;
	
	
	
	public RangeDownLoader(String urlstr, String localfile, int threadcount,
			HttpFileDownloadListener httpFileDownloadListener, RangeDownLoadDao dao) {
		this.urlstr = urlstr;
		this.localfile = localfile;
		this.threadcount = threadcount;
		this.httpFileDownloadListener = httpFileDownloadListener;
		this.dao = dao;
	}

	/** 是否在下载 */
	public boolean isdownloading() {
		return state == DOWNLOADING;
	}

	/**
	 * 得到downloader里的信息 首先进行判断是否是第一次下载，如果是第一次就要进行初始化，并将下载器的信息保存到数据库中
	 * 如果不是第一次下载，那就要从数据库中读出之前下载的信息（起始位置，结束为止，文件大小等），并将下载信息返回给下载器
	 */
	public LoadInfo init() {

		if (isfirst(urlstr)) {
			DebugUtils.d("第一次加载");
			firstInit();
			infos = new ArrayList<DownloadInfo>();
			if(threadcount>=fileSize){
				threadcount = 1;
			}
			long threadRange = fileSize / threadcount; // 每段长度   这里有个很坑的地方  就是小数点后的数据丢了  所以在A处加了个判断
			for (int i = 0; i < threadcount; i++) {
				DownloadInfo downloadInfo =null;
				if(i==(threadcount-1)){//最后一段数据
					downloadInfo = new DownloadInfo(i, threadRange * i,
							fileSize-1, 0, urlstr);
				}else{//前几段数据
					downloadInfo = new DownloadInfo(i, threadRange * i,
							threadRange * (i + 1)-1, 0, urlstr);
				} 
				infos.add(downloadInfo);
				DebugUtils.d(downloadInfo.toString());
			}
			dao.saveInfos(infos);
			LoadInfo loadInfo = new LoadInfo(fileSize, 0, urlstr); 
			return loadInfo;
		} else {
			DebugUtils.d("--->已经加载过");

			infos = dao.getInfos(urlstr);
			long tempsize = 0;
			long tempcompletesize = 0;
			for (DownloadInfo itemDownloadInfo : infos) {
				tempsize = tempsize
						+ (itemDownloadInfo.getEndPos() - itemDownloadInfo
								.getStartPos()) + 1;//
				tempcompletesize = tempcompletesize
						+ itemDownloadInfo.getCompeleteSize();
				DebugUtils.d(itemDownloadInfo.toString());
			}
			LoadInfo loadInfo = new LoadInfo(tempsize, tempcompletesize, urlstr); 
			return loadInfo;

		}

	}

	/**
	 * 第一次下载初始化 这次不是真正的下载只不过是获取下文件的信息 比如大小等
	 */
	private void firstInit() {
		try {
			URL url = new URL(urlstr);
			DebugUtils.d("--->创建url:" + urlstr);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			DebugUtils.d("--->打开连接:" + httpURLConnection.toString());
			httpURLConnection.setConnectTimeout(5000);
			DebugUtils.d("--->设置延迟时长5000");
			httpURLConnection.setRequestMethod("GET");
			DebugUtils.d("--->GET方式请求");
			//old: fileSize = httpURLConnection.getContentLength();// 获取文件长度；
			//new-s:
			String contentRange = httpURLConnection.getHeaderField("Content-Range");
			DebugUtils.d("--->Content-Range：" + contentRange);
			if(null!=contentRange&&contentRange.length()>0){
				String[] items=contentRange.split("/");
				if(items.length==2){
					fileSize = Integer.parseInt(items[1]);
				}
			}
			DebugUtils.d("--->获取长度：Content-Range:" + httpURLConnection.getHeaderField("Content-Range") +"---ContentLength:"+httpURLConnection.getContentLength());
			//new-e:
			DebugUtils.d("--->获取长度：" + fileSize);
			
			
			String fileNameDesc=httpURLConnection.getHeaderField("content-disposition");
			// 创建空文件
			File file = new File(localfile); 
			if(fileSize<=0){
				httpFileDownloadListener.onStageChanged(HttpFileStatus.FAILED, 1);
				state = DOWNLOADING;
				return ;
			}
			if (!file.exists()) { 
				DebugUtils.d("文件不存在："+localfile);
				if(!file.getParentFile().exists()){
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
			}else{
				DebugUtils.d("文件存在："+localfile); 
				file.delete();
				file.createNewFile();
			}

			// 断点续传特有的文件访问类 理解为：做拼接用
			RandomAccessFile randomAccessFile = new RandomAccessFile(file,
					"rwd");
			randomAccessFile.setLength(fileSize);
			randomAccessFile.close();
			httpURLConnection.disconnect();
			
		} catch (MalformedURLException e) {
			DebugUtils.d( "URL 错误" + e.getMessage());
		} catch (IOException e) {
			DebugUtils.d( "url.openConnection 错误" + e.getMessage());
		}

	}

	/**
	 * 是否第一次下载
	 * 
	 * @param urlstr
	 * @return
	 */
	private boolean isfirst(String urlstr) {
		return dao.isHasInfors(urlstr);
	}

	
	/**  
     * 利用线程开始下载数据  
     */  
    public void download() {  
    	if(fileSize==0){
    		httpFileDownloadListener.onFileDownloadFailed(new Exception("下载失败，Content-Range:0"));
    		isdownloadFail = true;
			pause();
			delete(urlstr);
    		return ;
    	}
        if (infos != null) {  
            if (state == DOWNLOADING)  
                return;  
            state = DOWNLOADING;  
            for (DownloadInfo info : infos) {  
            	DebugUtils.d("----------1:download---"+new Gson().toJson(info));
                new MyTheard(info.getThreadid(), info.getStartPos(),  
                        info.getEndPos(), info.getCompeleteSize(),  
                        info.getUrl()).start();  
            }  
        }  
    } 
	
    
    
    public class MyTheard extends Thread {
		private int threadid;// 下载器id
		private long startPos;// 开始点
		private long endPos;// 结束点
		private long compeleteSize;// 完成度
		private String url;

		public MyTheard(int threadid, long startPos, long endPos,
				long compeleteSize, String url) {
			this.threadid = threadid;
			this.startPos = startPos;
			this.endPos = endPos+1;
			this.compeleteSize = compeleteSize;
			this.url = url;
		}

		@Override
		public void run() {

			URL url = null;
			HttpURLConnection connection = null;
			InputStream fileIs = null; 
			RandomAccessFile randomAccessFile = null; 
			
			try { 
				// 断点续传特有的文件访问类 理解为：做拼接用
				randomAccessFile = new RandomAccessFile(localfile, "rwd");
				long start_4 =  startPos + compeleteSize;
				long size_4 =4*1024;
				long queue_4 = (endPos+1 -start_4)/size_4+1;
				
				for(int i=0;i<queue_4;i++){
					url = new URL(this.url);
					connection = (HttpURLConnection) url.openConnection();
					connection.setConnectTimeout(5000);
					connection.setRequestMethod("GET");
					if((startPos+(i+1)*size_4-1)>fileSize){
						connection.setRequestProperty("Range", "bytes="
								+ (startPos + compeleteSize) + "-" + (fileSize-1));
						DebugUtils.d(threadid+"threadid设置断点续传参数Range:" + "bytes="
								+ (startPos + compeleteSize) + "-" + (fileSize-1));
					}else{
						connection.setRequestProperty("Range", "bytes="
								+ (startPos + compeleteSize) + "-" + (startPos+(i+1)*size_4-1));
						DebugUtils.d(threadid+"threadid设置断点续传参数Range:" + "bytes="
								+ (startPos + compeleteSize) + "-" + (startPos+(i+1)*size_4-1));
					}
					
					randomAccessFile.seek(startPos + compeleteSize);
					DebugUtils.d(threadid+"threadid拼接数据前移动坐标到指定位置:"
							+ (startPos + compeleteSize));
					
					fileIs = connection.getInputStream();
					
					byte[] buffer  = new byte[4096];
					int count = 0;
					while ((count = fileIs.read(buffer)) > 0) { 
						randomAccessFile.write(buffer, 0, count);
						this.compeleteSize += count; 
						if (state == PAUSE) {// 停止
							dao.updataInfos(threadid, (int) compeleteSize, urlstr); 
							DebugUtils.d("暂停");
							return;
						}
					}
					
					downloadPresent++;
					if(downloadPresent == threadcount*queue_4){
						httpFileDownloadListener.onStageChanged(HttpFileStatus.SUCCESS, (double)1);
						// 下载成功就清掉记录
						delete(urlstr);
					}else{
						if((double)((double)thresholdCount*0.1)<(double)((double)downloadPresent/(double)(threadcount*queue_4))){
							thresholdCount++;
							httpFileDownloadListener.onStageChanged(HttpFileStatus.DOWNLOADING, (double)((double)downloadPresent/(double)(threadcount*queue_4)));
						}
					}
				}
				

			} catch (Exception e) {
				isdownloadFail = true;
				pause();
				delete(urlstr);
				httpFileDownloadListener.onFileDownloadFailed(e);
				Log.e(Const.MSG_TAG, "下载失败1" + e.getMessage());
			} finally {
				try { 
					dao.updataInfos(threadid, (int) compeleteSize, urlstr); 
					connection.disconnect();
					if(null!=fileIs){
						fileIs.close(); 
					}
					randomAccessFile.close(); 
				} catch (IOException e) { 
					Log.e(Const.MSG_TAG, "下载失败2" + e.getMessage());
				}
			}

		}
	}
    
	
	/**
	 * 
	 * @author zhanglin
	 * 
	 */
//	public class MyTheard extends Thread {
//		private int threadid;// 下载器id
//		private long startPos;// 开始点
//		private long endPos;// 结束点
//		private long compeleteSize;// 完成度
//		private String url;
//
//		public MyTheard(int threadid, long startPos, long endPos,
//				long compeleteSize, String url) {
//			this.threadid = threadid;
//			this.startPos = startPos;
//			this.endPos = endPos+1;
//			this.compeleteSize = compeleteSize;
//			this.url = url;
//		}
//
//		@Override
//		public void run() {
//			URL url = null;
//			HttpURLConnection connection = null;
//			InputStream fileIs = null;
//
//			RandomAccessFile randomAccessFile = null;
//			try {
//				System.out.println(this.url);
//				url = new URL(this.url);
//				connection = (HttpURLConnection) url.openConnection();
//				connection.setConnectTimeout(500);
//				connection.setRequestMethod("GET");
//				connection.setRequestProperty("Range", "bytes="
//						+ (startPos + compeleteSize) + "-" + endPos);
//				DebugUtils.d(threadid+"threadid设置断点续传参数Range:" + "bytes="
//						+ (startPos + compeleteSize) + "-" + endPos);
//				
//				// 断点续传特有的文件访问类 理解为：做拼接用
//				randomAccessFile = new RandomAccessFile(localfile, "rwd");
//				randomAccessFile.seek(startPos + compeleteSize);
//				DebugUtils.d(threadid+"threadid拼接数据前移动坐标到指定位置:"
//						+ (startPos + compeleteSize));
//				
//				fileIs = connection.getInputStream();
//				int range = (int) (endPos -(startPos + compeleteSize));
//				DebugUtils.d(threadid+"threadid--->"+range+"="+endPos+"-("+startPos+"+"+compeleteSize+")");
//				
//				byte[] buffer  = new byte[4096];
//				int count = 0;
//				while ((count = fileIs.read(buffer)) > 0) {
////					DebugUtils.d(new String(buffer));
//					randomAccessFile.write(buffer, 0, count);
//					this.compeleteSize += count;
//					dao.updataInfos(threadid, (int) compeleteSize, urlstr);
////					DebugUtils.d("更新数据库进度");
//					/*Message message = mHandler.obtainMessage();
//					message.what = 1;
////					message.obj = getDownInfo();
//					message.arg1 = count;
//					mHandler.sendMessage(message);*/
//					
//					if (state == PAUSE) {// 停止
//						DebugUtils.d("暂停");
//						return;
//					}
//				}
//
//			} catch (Exception e) {
//				isdownloadFail = true;
//				pause();
//				delete(urlstr);
//				httpFileDownloadListener.onFileDownloadFailed(e);
//				Log.e(Const.MSG_TAG, "下载失败" + e.getMessage());
//			} finally {
//				try {
//					//DebugUtils.d("-->线程："+threadid+"threadid下载完成"+getDownInfo().toString());
//					connection.disconnect();
//					if(null!=fileIs){
//						fileIs.close(); 
//					}
//					randomAccessFile.close();
//					downloadPresent++;
//					if(isdownloadFail){
//					}else{
//						if(downloadPresent == threadcount){
//							httpFileDownloadListener.onStageChanged(HttpFileStatus.SUCCESS, (double)1);
//							// 下载成功就清掉记录
//							delete(urlstr);
//						}else{
//							httpFileDownloadListener.onStageChanged(HttpFileStatus.DOWNLOADING, (double)((double)downloadPresent/(double)threadcount));
//						}
//					}
////					dao.closeDb();
//				} catch (IOException e) { 
//					Log.e(Const.MSG_TAG, "下载失败" + e.getMessage());
//				}
//			}
//
//		}
//	}
	
	//删除数据库中urlstr对应的下载器信息  
    public void delete(String urlstr) {  
        dao.delete(urlstr);
        //dao.closeDb();
    }  
    //设置暂停  
    public void pause() {  
        state = PAUSE;  
    }  
    //重置下载状态  
    public void reset() {  
       state = INIT;  
    }  

}
