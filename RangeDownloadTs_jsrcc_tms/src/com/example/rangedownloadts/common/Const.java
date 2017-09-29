package com.example.rangedownloadts.common;

import android.os.Environment;


/**
 * rm  -rf   /mnt/shell/emu/0/RangeDown*
 * @author mars_zhang
 *
 */
public class Const {
	public static String MSG_TAG="com.example.rangedownloadts";
	
	public static final class RangeDownloadInfo {
		//public static final String BASE_HTTP_URL="http://172.27.35.1:8081/zweb/getFile/nmc_android_debug_mars/apk"; 
		public static final String BASE_HTTP_URL="http://58.213.149.86:8850/fusepay/servlet/DownLoadServlet?id=261&type=T002&size=2363"; 
		
		/** 存放位置  */
		public static final String SD_PATH=Environment.getExternalStorageDirectory().getAbsolutePath()+"/RangeDownload";
		
		/** filename */ 
//		public static final String filename="201609230903_abc.log";
		
		public static final String filename="nmc_android_debug_mars.apk";
		
	}
}
