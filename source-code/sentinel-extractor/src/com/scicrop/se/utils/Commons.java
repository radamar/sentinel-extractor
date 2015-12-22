package com.scicrop.se.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import javax.net.ssl.HttpsURLConnection;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;

import com.scicrop.se.http.SeHttpAuthenticator;

public class Commons {

	private Commons(){}

	private static Commons INSTANCE = null;

	public static Commons getInstance(){
		if(INSTANCE == null) INSTANCE = new Commons();
		return INSTANCE;
	}



	public byte[] getByteArrayFromUrlString(String urlStr, String outputFileNamePath, long completeFileSize, String contentType, String user, String password) throws SentinelRuntimeException{

		int buffer = 8192;

		byte[] retBuf = null;
		URL url = null;
		HttpURLConnection connection = null;
		FileOutputStream fos = null;
		InputStream in = null;
		OutputStream bout = null;
		byte[] data = null;
		int bytesread = 0;
		int bytesBuffered = 0;
		RandomAccessFile randomAccessfile = null;

		long downloadedFileSize = 0;

		try {

			Authenticator.setDefault(new SeHttpAuthenticator(user, password));
			url = new URL(urlStr);
			connection = (HttpsURLConnection) url.openConnection();

			connection.setRequestProperty(Constants.HTTP_HEADER_ACCEPT, contentType);

			File outFile = new File(outputFileNamePath);
			if(outFile.exists()){

				downloadedFileSize = outFile.length();

				if(downloadedFileSize < completeFileSize){
					System.out.println("Incomplete download. Resuming.");

					connection.setRequestProperty("Range", "bytes=" + downloadedFileSize + "-");

					connection.connect();
					try {
						checkStatus(connection);
					} catch (SentinelHttpConnectionException e) {
						System.err.println("====> "+e.getMessage());
						return retBuf;
					}

					long contentLength = connection.getContentLength();
					if (contentLength < 1) contentLength = completeFileSize;
					

					randomAccessfile = new RandomAccessFile(outputFileNamePath, "rw");
					randomAccessfile.seek(downloadedFileSize);

					in = connection.getInputStream();
					
					long downloadDiff = completeFileSize - downloadedFileSize;
					if (downloadDiff > buffer) data = new byte[buffer];
					else data = new byte[(int) downloadDiff];

					while( (bytesread = in.read( data )) > -1 ) {
						
						randomAccessfile.write(data, 0, bytesread);
						downloadedFileSize += bytesread;
						formatDownloadedProgress(completeFileSize, downloadedFileSize);
					}
					

				}
			}else{

				
				connection.connect();
				try {
					checkStatus(connection);
				} catch (SentinelHttpConnectionException e) {
					System.err.println(e.getMessage());
					return retBuf;
				}

				in = connection.getInputStream();
				fos = new FileOutputStream(outputFileNamePath);
				bout = new BufferedOutputStream(fos, buffer);
				data = new byte[buffer];



				
				while( (bytesread = in.read( data )) > -1 ) {
					bout.write( data , 0, bytesread );
					bytesBuffered += bytesread;
					downloadedFileSize += bytesread;

					formatDownloadedProgress(completeFileSize, downloadedFileSize);
					

					if (bytesBuffered > 1024 * 1024) { //flush after 1MB
						bytesBuffered = 0;
						bout.flush();
					}
				}			



			}





		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{

			if(bout !=null){
				try {bout.flush(); } catch (IOException e) { e.printStackTrace(); }
				try {bout.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if(fos !=null){
				try {fos.flush(); } catch (IOException e) { e.printStackTrace(); }
				try {fos.close(); } catch (IOException e) { e.printStackTrace(); }
			}
			if(in !=null) try { in.close(); } catch (IOException e) { e.printStackTrace(); }
			
			if(randomAccessfile != null) try { randomAccessfile.close(); } catch (IOException e) { e.printStackTrace(); } 
			
			if(connection != null) connection.disconnect();
		}

		return retBuf;

	}


	private void formatDownloadedProgress(long completeFileSize, long downloadedFileSize) {
		DecimalFormat dfa = new DecimalFormat("000.0");
		DecimalFormat dfb = new DecimalFormat("###,###,###,###");
		double currentProgress;
		String formatedProgress;
		currentProgress = ((((double)downloadedFileSize) * 100) / ((double)completeFileSize));
		formatedProgress = dfa.format(currentProgress)+"% "+dfb.format(downloadedFileSize) + " bytes";
		System.out.println(formatedProgress);
	}


	private void checkStatus(HttpURLConnection connection) throws SentinelRuntimeException, SentinelHttpConnectionException {
		HttpStatusCodes httpStatusCode = null;
		try {
			httpStatusCode = HttpStatusCodes.fromStatusCode(connection.getResponseCode());
		} catch (IOException e) {
			throw new SentinelRuntimeException(e);
		}
		if (400 <= httpStatusCode.getStatusCode() && httpStatusCode.getStatusCode() <= 599) {
			throw new SentinelHttpConnectionException("Http Connection failed with status " + httpStatusCode.getStatusCode() + " " + httpStatusCode.toString());
		}else System.out.println("HTTP Response Code: "+httpStatusCode);
		
	}


	private HttpURLConnection initializeConnection(String absolutUri, String contentType, String httpMethod, String user, String password) throws SentinelRuntimeException {

		//		// Create a trust manager that does not validate certificate chains
		//        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
		//                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		//                    return null;
		//                }
		//                public void checkClientTrusted(X509Certificate[] certs, String authType) {
		//                }
		//                public void checkServerTrusted(X509Certificate[] certs, String authType) {
		//                }
		//            }
		//        };
		// 
		//        // Install the all-trusting trust manager
		//        SSLContext sc = SSLContext.getInstance("SSL");
		//        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		//        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		// 
		//        // Create all-trusting host name verifier
		//        HostnameVerifier allHostsValid = new HostnameVerifier() {
		//            public boolean verify(String hostname, SSLSession session) {
		//                return true;
		//            }
		//        };
		// 
		//        // Install the all-trusting host verifier
		//        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		Authenticator.setDefault(new SeHttpAuthenticator(user, password));
		URL url = null;
		HttpURLConnection connection = null;
		try {
			url = new URL(absolutUri);
			connection = (HttpsURLConnection) url.openConnection();

			connection.setRequestMethod(httpMethod);
			connection.setRequestProperty(Constants.HTTP_HEADER_ACCEPT, contentType);
			if(Constants.HTTP_METHOD_POST.equals(httpMethod) || Constants.HTTP_METHOD_PUT.equals(httpMethod)) {
				connection.setDoOutput(true);
				connection.setRequestProperty(Constants.HTTP_HEADER_CONTENT_TYPE, contentType);
			}
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");

		} catch (MalformedURLException e) {
			throw new SentinelRuntimeException(e);
		} catch (IOException e) {
			throw new SentinelRuntimeException(e);
		} finally {
			if(connection != null) connection.disconnect();
		}
		

		return connection;
	}


	public InputStream execute(String relativeUri, String contentType, String httpMethod, String user, String password) throws SentinelRuntimeException, SentinelHttpConnectionException  {
		HttpURLConnection connection = null;
		InputStream content = null;
		try {
			connection = initializeConnection(relativeUri, contentType, httpMethod, user, password);
			connection.connect();
			checkStatus(connection);
			content = connection.getInputStream();

		} catch (IOException e) {
			throw new SentinelRuntimeException(e);
		} 

		return content;
	}

	

}