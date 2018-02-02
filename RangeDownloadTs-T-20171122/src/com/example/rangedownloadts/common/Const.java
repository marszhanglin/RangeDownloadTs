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
		//public static final String BASE_HTTP_URL="http://192.168.1.30:8080/zweb/getFile/1/txt"; 
		public static final String BASE_HTTP_URL="http://192.168.1.30:8080/zweb/getFile/nmc_android_debug_mars/apk"; 
		//public static final String BASE_HTTP_URL="http://58.213.149.86:8850/fusepay/servlet/DownLoadServlet?id=261&type=T002&size=4"; 
		//public static final String BASE_HTTP_URL="http://192.168.1.38:10086/file/rcc-tms-1.0.11_test_demo_standard_sign.apk"; 
		//public static final String BASE_HTTP_URL="http://120.24.18.21:3006/mtms/downloads/APP/0E01ECB6724494E688209FEBC39758417D5EFE96/com.newland.allinpay/1002.apk";
		//public static final String BASE_HTTP_URL="http://www.js96008.com:1800/fusepay/servlet/DownLoadServlet?id=10&type=T002&size=4";
		
		/** 存放位置  */
		public static final String SD_PATH=Environment.getExternalStorageDirectory().getAbsolutePath()+"/RangeDownload";
		
		/** filename */ 
//		public static final String filename="201609230903_abc.log";
		
		public static final String filename="my99.apk";
		//public static final String filename="1.txt";
	}
}
