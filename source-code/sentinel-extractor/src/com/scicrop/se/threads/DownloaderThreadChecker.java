package com.scicrop.se.threads;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.scicrop.se.commons.dataobjects.ArgumentsHistory;
import com.scicrop.se.commons.dataobjects.Payload;
import com.scicrop.se.commons.net.NetUtils;
import com.scicrop.se.commons.utils.LogHelper;
import com.scicrop.se.net.SeUdpClient;
import com.scicrop.se.runtime.Launch;
import com.scicrop.se.utils.DownloadHelper;

public class DownloaderThreadChecker extends Thread {


	private String outputFileNamePath = null;
	private long len = -1l;
	private boolean forceStop = false;
	private ArgumentsHistory aHistory = null;
	private static Log log = LogFactory.getLog(DownloaderThreadChecker.class);

	public DownloaderThreadChecker(String outputFileNamePath, long len, ArgumentsHistory aHistory){
		this.outputFileNamePath = outputFileNamePath;
		this.len = len;
		this.aHistory = aHistory;
	}

	public void forceStop(){
		forceStop = true;
		Launch.STATUS = new Payload(NetUtils.SentinelExtractorStatus.FORCE_STOP, null, -1, Launch.CONF_PARAM, new Date().getTime());
		LogHelper.getInstance().handleVerboseLog(aHistory.isVerbose(), aHistory.isLog(), log, 'i', "ThreadChecker status:    "+Launch.STATUS.getSentinelExtractorStatus()+"                       ");
	}

	public void run(){
		RandomAccessFile raf = null;

		long lenT0 = -1l;
		long lenT1 = -1l;


		while(lenT1 < len && !forceStop){

			try {

				raf = new RandomAccessFile(outputFileNamePath, "r");
				lenT0 = raf.length();

				Thread.sleep(aHistory.getThreadCheckerSleep());

				lenT1 = raf.length();

				String statusDescription = outputFileNamePath+" | "+DownloadHelper.getInstance().formatDownloadedProgress(len, lenT1);

				if(lenT1 > lenT0){
					Launch.STATUS = new Payload(NetUtils.SentinelExtractorStatus.DOWNLOADING, statusDescription, -1, Launch.CONF_PARAM, new Date().getTime());
					//System.out.print("ThreadChecker status:             "+Launch.STATUS.getSentinelExtractorStatus()+"          ");
					LogHelper.getInstance().handleVerboseLog(false, aHistory.isLog(), log, 'i', "ThreadChecker status:\t"+DownloadHelper.getInstance().formatDownloadedProgress(len, lenT1));
				}
				else if(len == lenT1 || len == lenT0){
					Launch.STATUS = new Payload(NetUtils.SentinelExtractorStatus.FINISHED, statusDescription, -1, Launch.CONF_PARAM, new Date().getTime());
					LogHelper.getInstance().handleVerboseLog(aHistory.isVerbose(), aHistory.isLog(), log, 'i', "ThreadChecker status: "+Launch.STATUS.getSentinelExtractorStatus());
				}
				else if(lenT0 == lenT1){
					Launch.STATUS = new Payload(NetUtils.SentinelExtractorStatus.STALLED, statusDescription, -1, Launch.CONF_PARAM, new Date().getTime());
					LogHelper.getInstance().handleVerboseLog(aHistory.isVerbose(), aHistory.isLog(), log, 'i', "ThreadChecker status: "+Launch.STATUS.getSentinelExtractorStatus()+"         FORCE QUITING!");

					if(Launch.UDP_SERVER_PORT > 0){

						SeUdpClient udpClient = new SeUdpClient(Launch.UDP_SERVER_PORT, aHistory);
						udpClient.sendMsgToUdpServer();

					}

					System.exit(1);
				} 

			} catch (FileNotFoundException e) {

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if(raf != null)
					try {
						raf.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

		}
		LogHelper.getInstance().handleVerboseLog(aHistory.isVerbose(), aHistory.isLog(), log, 'i', "ThreadChecker status: END OF ThreadChecker [FORCE STOP = "+forceStop+"]" );



	}

}
