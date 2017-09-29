package com.example.rangedownloadts.db;

import com.google.gson.Gson;

/**
 * 下载分段信息
 * 
 * @author mars_zhang
 * 
 */
public class DownloadInfo {
	private int threadid;// 下载器id
	private long startPos;// 开始点
	private long endPos;// 结束点
	/** 每段的完成长度 */
	private long compeleteSize;
	private String url;

	
	
	
	public DownloadInfo(int threadid, long startPos, long endPos,
			long compeleteSize, String url) {
		super();
		this.threadid = threadid;
		this.startPos = startPos;
		this.endPos = endPos;
		this.compeleteSize = compeleteSize;
		this.url = url;
	}




	public int getThreadid() {
		return threadid;
	}




	public void setThreadid(int threadid) {
		this.threadid = threadid;
	}




	public long getStartPos() {
		return startPos;
	}




	public void setStartPos(long startPos) {
		this.startPos = startPos;
	}




	public long getEndPos() {
		return endPos;
	}




	public void setEndPos(long endPos) {
		this.endPos = endPos;
	}




	public long getCompeleteSize() {
		return compeleteSize;
	}




	public void setCompeleteSize(long compeleteSize) {
		this.compeleteSize = compeleteSize;
	}




	public String getUrl() {
		return url;
	}




	public void setUrl(String url) {
		this.url = url;
	}

	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	 

}
