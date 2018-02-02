package com.example.rangedownloadts.mtms;
public abstract interface HttpFileDownloadListener
{
  public abstract void onFileblockFailed( Throwable paramThrowable);

  public abstract void onFileDownloadFailed( Throwable paramThrowable);

  public abstract void onStageChanged( HttpFileStatus paramHttpFileStatus, double paramDouble);
}