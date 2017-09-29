package com.example.rangedownloadts.downloadtool;

import com.google.gson.Gson;


/**
 * 下载进度信息
 * @author mars_zhang
 *
 */
public class LoadInfo {

	 public long fileSize;// 文件大小  
	    private long complete;// 完成度  
	    private String urlstring;// 下载器标识  
	  
	    public LoadInfo(long fileSize, long complete, String urlstring) {  
	        this.fileSize = fileSize;  
	        this.complete = complete;  
	        this.urlstring = urlstring;  
	    }  
	  
	   
	  
	    public long getFileSize() {
			return fileSize;
		}



		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}



		public long getComplete() {
			return complete;
		}



		public void setComplete(long complete) {
			this.complete = complete;
		}



		public String getUrlstring() {
			return urlstring;
		}



		public void setUrlstring(String urlstring) {
			this.urlstring = urlstring;
		}



		@Override  
	    public String toString() {  
	        return new Gson().toJson(this);  
	    }  
	
}
