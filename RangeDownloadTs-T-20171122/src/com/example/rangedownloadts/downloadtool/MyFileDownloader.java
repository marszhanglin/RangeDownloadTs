package com.example.rangedownloadts.downloadtool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.util.Log;

public class MyFileDownloader extends Thread {

	private static final String TAG = "FileDownloader";
	public static final int BUFFER_SIZE = 1024;// 512
	public static final String DEFAULT_USER_AGENT = "MotoneMarket";
	// ////////////////////////////////////////////////////////////////////
	/** Error Code */
	/** 没有找到文件，请求URL有误 */
	public static final int E_URL_INVALID = -1;
	/** HTTP protocol 无效 */
	public static final int E_PROTOCOL_INVALID = 0;
	/** network I/O error */
	public static final int E_IO_EXP = 1;
	/** 创建下载文件失败 */
	public static final int E_FILE_NOTFOUND = 2;
	/** 文件安全权限错误 */
	public static final int E_FILE_SECURITY = 3;
	/** request 状态异常 */
	public static final int E_ILLEGAL_STATE = 4;
	/** 追加验证符 错误 */
	public static final int E_APPEND_VER_ERROR = 5;
	/** 下载不完成 */
	public static final int E_DOWN_NOT_COMPLETE = 6;
	/** 断点续传，服务断点返回错误 */
	public static final int E_RANGE_WRANG = 7;
	/** 断点续传，服务断点返回错误 */
	public static final int E_CONNECT_TIMEOUT = 8;
	/** 存储空间不足，无法创建文件 或 写入文件 */
	public static final int E_NO_SPACE = 9;
	// ////////////////////////////////////////////////////////////////////
	/** 同步锁 */
	private final Object mLock = new Object();
	/** 暂停标志：默认false,开启运行 */
	private boolean m_isPause = false;
	/** 停止标志：默认true停止未开始true，false：运行中 */
	private boolean m_isStop = true;
	/** 取消标志: 默认false未取消，true:取消 || false:未取消 */
	private boolean m_isCancel = false;
	/** 退出完成标志: 默认false未开始，未完成，true：完成 || false:未完成 */
	private boolean m_isExit = false;

	// ////////////////////////////////////////////////////////////////////
	/** 该下载对象的 ID */
	private int mId;
	/** 该下载目标 URL */
	private String mUrl;
	/** 文件全名 */
	private String mFile;
	/** 临时文件名 */
	private String mTFile;
	/** 文件的验证字符串 */
	private String mVerifyString = null;
	/** 文件下载监听器 */
	private OnDownloadListener mDownListener;

	// ////////////////////////////////////////////////////////////////////
	private HttpURLConnection httpClient;
	/** 写入文件的位置，文件的append位置 */
	private long mPos = 0;
	/** 文件总长度 */
	private long mLength = -1;

	/** HTTP getInput */
	private InputStream streamEntry = null;
	/** File Stream */
	private FileOutputStream stream = null;

	// ////////////////////////////////////////////////////////////////////
	public MyFileDownloader(int id, String url, String file, String verStr) {
		this.mId = id;
		this.mUrl = url;
		this.mFile = file;
		this.mTFile = file + ".info";
		this.mVerifyString = verStr;
	}

	/** 设置下载监听器 */
	public synchronized void setOnDownloadListener(OnDownloadListener listener) {
		this.mDownListener = listener;
	}

	/** 下载监听器 */
	public interface OnDownloadListener {
		void onFinish(int id);

		void onProgress(int id, int percent);

		void onError(int id, int errorCode);
	}

	/** 获取该任务ID */
	public int getID() {
		return mId;
	}

	/** 清理资源 */
	public void deleteFile() {
		File f = new File(mFile);

		if (f.exists()) {
			f.delete();
		}
	}

	/** 清理临时文件 */
	public void deleteTFile() {
		File f = new File(mTFile);

		if (f.exists()) {
			f.delete();
		}
	}

	// ////////////////////////////////////////////////////////////////////
	/** 线程操作 */
	/** 开启线程 */
	public synchronized void start() {

		super.start();
		Log.i(TAG, "###_start get url = " + mUrl);
	}

	/** 工作循环 */
	public void run() {
		this.setPriority(MIN_PRIORITY);
		// 开始运行中...
		m_isStop = false;
		/** 获取本地文件的已下载长度 */
		mPos = getLocalFileSize();
		/** 初始化网络，并获取网络文件长度 */
		mLength = initHttp();
		if (mLength < 0) {
			close();
			return;
		}
		/** 开始下载文件 */
		boolean r = true;
		byte buf[] = new byte[BUFFER_SIZE];
		int numread = 0;
		int prePrec = 0;
		int CurPrec = 0;

		if (mPos < mLength) {
			/** 输入 输出 对接 */
			try {
				/** 获取网路输入流 */
				streamEntry = httpClient.getInputStream();
				/** 装饰到 FileOutputStream */
				File file = new File(mFile);
				if (!file.exists()) {
					file.createNewFile();
				}
				stream = new FileOutputStream(file, true);

			} catch (IllegalStateException e) {
				e.printStackTrace();
				doError(E_ILLEGAL_STATE);
				r = false;
			} catch (SecurityException e) {
				e.printStackTrace();
				doError(E_FILE_SECURITY);
				r = false;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				doError(E_FILE_NOTFOUND);
				r = false;
			} catch (IOException e) {
				e.printStackTrace();
				doError(E_NO_SPACE);
				r = false;
			}

			if (!r) {
				return;
			}

			/** 循环写入缓存文件 */
			if (streamEntry != null && stream != null) {
				while (true && !m_isStop) {
					/** 等待 */
					_wait();
					/** 工作 */
					try {
						/** 读取到 缓存 buf */
						numread = streamEntry.read(buf);
					} catch (IOException e) {
						e.printStackTrace();
						doError(E_IO_EXP);
						r = false;
						return;
					}
					// 休眠50ms
					// try
					// {
					// Thread.sleep(50);
					// } catch (InterruptedException e1)
					// {
					// e1.printStackTrace();
					// }
					/**
					 * the number of bytes actually read or -1 if the end of the
					 * stream has been reached.
					 */
					if (numread < 0 || mPos == mLength) {
						break;
					}
					/** 写入 文件 */
					try {
						stream.write(buf, 0, numread);
					} catch (IOException e) {
						e.printStackTrace();
						doError(E_NO_SPACE);
						r = false;
						return;
					}
					/** 增加记录 */
					mPos += numread;
					/** 报告监听器 */
					if (mDownListener != null) {
						CurPrec = (int) ((float) mPos / mLength * 100);
						if (CurPrec > prePrec) {
							try {
								mDownListener.onProgress(mId, CurPrec);
							} catch (Exception e) {
								e.printStackTrace();
								Log.e(TAG, "---> unable exception occor! <----");
							}
							prePrec = CurPrec;
						}
					}
				}
			}
		}

		/** 检验完整性 */
		if (mPos == mLength) {
			/** 追加验证字符串 */
			boolean verable = true;
			if (mVerifyString != null) {
				byte[] bytes = mVerifyString.getBytes();
				try {
					if (stream != null) {
						stream.write(bytes, 0, bytes.length);
					}
					deleteTFile();
					Log.i(TAG, "---> add verstring at apk : " + mVerifyString);
				} catch (IOException e) {
					e.printStackTrace();
					doError(E_NO_SPACE);
					verable = false;
					return;
				}
			}
			/** 下载完成 */
			if (verable && mDownListener != null) {
				try {
					mDownListener.onFinish(mId);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "---> unable exception occor! <----");
				}
			}
		} else {
			/** 下载不完成 */
			if (!m_isStop) {
				doError(E_DOWN_NOT_COMPLETE);
				deleteTFile();
			} else
			/** 中途暂停 */
			{
				/** do nothing */
			}
		}

		/** 关闭资源 */
		close();
		return;
	}

	// ///////////////////////////////////////////////////////////////////////////////////
	/** 获取网络文件长度 */
	private long initHttp() {
		/** 创建URI */
		URL url = null;
		try {
			url = new URL(mUrl);
		} catch (MalformedURLException e) {
			/** 无效的URL */
			Log.e(TAG, "url = " + mUrl);
			doError(E_URL_INVALID);
			e.printStackTrace();
			return -1;
		}

		try {
			httpClient = (HttpURLConnection) url.openConnection();
			String property = "bytes=" + mPos + "-";
			httpClient.setRequestProperty("RANGE", property);

			/** 连接超时和读取超时都是5秒 */
			httpClient.setConnectTimeout(5000);
			httpClient.setReadTimeout(5000);

			httpClient.connect();

			int code = httpClient.getResponseCode();
			/** code == 200 || code == 206 */
			if (code < 400) {
				String header = null;
				/** 获取 content-length */
				for (int i = 1;; i++) {
					header = httpClient.getHeaderFieldKey(i);
					if (header != null) {
						if (header.toLowerCase().equals("content-length")) {
							mLength = Integer.parseInt(httpClient
									.getHeaderField(header));
							break;
						}
					} else {
						break;
					}
				}
				/** 获取 range */
				for (int j = 1;; j++) {
					header = httpClient.getHeaderFieldKey(j);
					if (header != null) {
						if (header.toLowerCase().equals("content-range")) {
							String range = httpClient.getHeaderField(header);
							int i = "bytes ".length();

							if (i > range.length()) {
								// content-range 返回不合法时,或者 不支持断点续传， 为0 跳出
								break;
							}

							String str = null;
							try {
								str = range.substring(i, range.length());
							} catch (IndexOutOfBoundsException e) {
								// content-range 返回不合法时,或者 不支持断点续传， 为0 跳出
								e.printStackTrace();
								break;
							}

							String[] split = str.split("-");
							if (split != null && split.length == 2) {
								try {
									long start = Long
											.parseLong(split[0].trim());
									String[] len = split[1].split("/");
									mLength = Long.parseLong(len[1].trim());

									if (start == mPos) {
										/** 断点设置 OK */
									} else {
										/** 断点设置 Fail */
										doError(E_RANGE_WRANG);
										return -1;
									}
								} catch (Exception e) {
									// content-range 返回不合法时,或者 不支持断点续传， 为0 跳出
									e.printStackTrace();
									doError(E_RANGE_WRANG);
									break;
								}
							}
							break;
						}
					} else {
						break;
					}
				}
				/** 返回网络文件长度 */
				return mLength;
			} else
			/** 请求的 Range 范围不合法 */
			if (code == 416) {
				doError(E_RANGE_WRANG);
			} else {
				/** 请求失败 */
				doError(E_URL_INVALID);
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			doError(E_CONNECT_TIMEOUT);
		} catch (IOException e) {
			e.printStackTrace();
			doError(E_IO_EXP);
		} catch (Exception e) {
			e.printStackTrace();
			doError(E_IO_EXP);
		}
		return -1;
	}

	/** 获取本地文件长度 */
	private long getLocalFileSize() {
		File f = new File(mFile);
		if (f.exists()) {
			return f.length();
		} else {
			File tf = new File(mTFile);
			try {
				tf.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return 0;
		}
	}

	/** 报告错误代码，并close */
	private void doError(int errorCode) {
		if (mDownListener != null) {
			try {
				mDownListener.onError(mId, errorCode);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "---> unable exception occor! <----");
			}
		}

		close();

		return;
	}

	/** 关闭资源 */
	private void close() {
		/** 关闭输入流 */
		if (streamEntry != null) {
			try {
				streamEntry.close();
				streamEntry = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/** 关闭输出流 */
		if (stream != null) {
			try {
				stream.flush();
				stream.getFD().sync();
				stream.close();
				stream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/** 关闭 HTTP Client */
		if (httpClient != null) {
			httpClient.disconnect();
			httpClient = null;
		}
		/** 清理资源 */
		if (m_isCancel) {
			deleteFile();
			deleteTFile();
		}
		// 设置完成时 状态
		// 重新设置为未开始
		m_isStop = true;
		// 标志线程结束
		m_isExit = true;
	}

	// ////////////////////////////////////////////////////////////////////
	/** 线程等待 */
	private void _wait() {
		/** 睡眠50ms */
		try {
			Thread.sleep(20);// 200
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		/** 等待 锁 */
		synchronized (mLock) {
			while (m_isPause) {
				try {
					mLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** 暂停线程 */
	public synchronized void _pause() {
		m_isPause = true;
		Log.i(TAG, "###_pause---->");
	}

	/** 恢复线程 */
	public synchronized void _resume() {
		m_isPause = false;
		synchronized (mLock) {
			mLock.notifyAll();
		}
		Log.i(TAG, "###_resume---->");
	}

	/** 停止线程 */
	public synchronized void _stop() {
		m_isStop = true;
		_resume();
		Log.i(TAG, "###_stop---->");
	}

	/** 取消线程 */
	public void _cancel() {
		if (!m_isStop) {
			// 运行中...
			_stop();
			m_isCancel = true;
		} else {
			// 未开始或者运行结束
			deleteFile();
			deleteTFile();
		}
	}

	/** 线程是否完成 */
	public synchronized boolean isFinished() {
		return m_isExit;
	}
}
