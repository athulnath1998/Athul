package es.upc.lewis.quadadk.comms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import es.upc.lewis.quadadk.mission.MissionThread;

import android.util.Log;

public class HTTPCalls{
	private static String START = "start";
	private static String END   = "end";
	
	static int HTTPResponseOK = 200;
	static String server_addr = "http://pbl1.webfactional.com/";
	
	/*** MISSION ***/
	public static boolean get_startmission(String quadid){
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		HttpGet httpget = new HttpGet(server_addr+"get_startmission.php?id="+quadid);
		HttpResponse response;
		
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			int myresponsecode = response.getStatusLine().getStatusCode();	
			if(myresponsecode==HTTPResponseOK) {
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				StringBuilder builder = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				content.close();
				entity.consumeContent();
				if(builder.toString().equals(START)) {
					return true;
				} else {
					return false;
				}
			}	
		} catch (ClientProtocolException e) { } catch (IOException e) { }
		
		return false;

	}
	
	public static boolean get_abortmission(String quadid){
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		HttpGet httpget = new HttpGet(server_addr+"get_endmission.php?id="+quadid);
		HttpResponse response;
		
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			int myresponsecode = response.getStatusLine().getStatusCode();	
			if(myresponsecode==HTTPResponseOK) {
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				StringBuilder builder = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				content.close();
				entity.consumeContent();
				if(builder.toString().equals(END)) {
					return true;
				} else {
					return false;
				}
			}	
		} catch (ClientProtocolException e) { } catch (IOException e) { }
		
		return false;

	}
	
	//SYSTEM LOG
	public static boolean debug_data(String quadid, String data) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		@SuppressWarnings("deprecation")
		String params = URLEncoder.encode(data);
		HttpGet httpget = new HttpGet(server_addr+"send_logs.php?id="+quadid+"&data="+params);
		HttpResponse response;
		
		try{
			response = httpclient.execute(httpget);
			int myresponsecode=response.getStatusLine().getStatusCode();
			if(myresponsecode==HTTPResponseOK) {
				return true;
			} else {
				return false;
			}
		}catch (ClientProtocolException e) { } catch (IOException e) { }
		
		return false;
	}
	
	/**
	 * 
	 * @param quadid
	 * @param varname temp1, temp2, hum1, hum2, co, no2, alt_bar, alt_gps
	 * @param value
	 * @return
	 */
	public static boolean send_data(String quadid, String varname, String value) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		HttpGet httpget = new HttpGet(server_addr+"send_data.php?id="+quadid+"&varname="+varname+"&value="+value);
		HttpResponse response;
		
		try{
			response = httpclient.execute(httpget);
			int myresponsecode=response.getStatusLine().getStatusCode();
			if(myresponsecode==HTTPResponseOK) {
				return true;
			} else {
				return false;
			}
		}catch (ClientProtocolException e) { } catch (IOException e) { }
		
		return false;
	}
	
	//SEND PICTURE
	public static boolean send_picture(File file, String pic_id) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		HttpPost httppost = new HttpPost(server_addr+"send_picture.php?id="+MissionThread.QUAD_ID+"&pic="+pic_id);
		FileBody fb = new FileBody(file);
		MultipartEntityBuilder buildern = MultipartEntityBuilder.create();
		buildern.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		buildern.addPart("userfile",fb);
		final HttpEntity resentity = buildern.build();
		//httppost.setHeader("Content-Type", "image/jpg");
		//httppost.setHeader("file","myfilename.jpg");
		httppost.setEntity(resentity);
		HttpResponse response;
		
		try {
			response = httpclient.execute(httppost);
			
			HttpEntity entity = response.getEntity();
			
			int myresponsecode = response.getStatusLine().getStatusCode();
			if(myresponsecode==HTTPResponseOK) {
				
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(content));
				String line;
				StringBuilder builder = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				content.close();
				entity.consumeContent();
				
				Log.d("TESTS", builder.toString());
				
				return true;
			} else {
				return false;
			}
		}
		catch(ClientProtocolException e) { } catch(IOException e) { }
		
		return false;
	}
}
