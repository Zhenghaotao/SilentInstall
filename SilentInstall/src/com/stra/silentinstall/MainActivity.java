package com.stra.silentinstall;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.stra.silentinstall.utils.PackageUtils;
import com.stra.silentinstall.utils.StreamUtil;


public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	
	private String apkurl = "http://192.168.10.111:8080/apk.html";
	
	private String appName;
	private String version;
	private String description;
	private String downloadurl;
	
	public Handler installHandler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
			case PackageUtils.INSTALL_SUCCEEDED:
				break;
				
			default:
				break;
			}
		}
	};
	
	
	private Handler obtainMsgHandler  = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
			case 1:
				downloadApk();
				break;
				
			case -1:
				break;
			}
		}
		
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		new ObainServerMsgThread().start();
		
	}
	public void downloadApk(){
		FinalHttp finalhttp = new FinalHttp();
		finalhttp.download(downloadurl, Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ File.separator + appName,
				new AjaxCallBack<File>() {
					@Override
					public void onFailure(Throwable t, int errorNo,
							String strMsg) {
						t.printStackTrace();
						Toast.makeText(getApplicationContext(),
								"下载失败", 1).show();
						super.onFailure(t, errorNo, strMsg);
					}
					
					@Override
					public void onLoading(long count, long current) {
						super.onLoading(count, current);
						int progress = (int) (current * 100 / count);
					}

					@Override
					public void onSuccess(File t) {
						super.onSuccess(t);
						Log.i(TAG,"下载成功");
						startInstall(t);
					}
				});
	}
	
	
	public void startInstall(File t){
		Log.i(TAG, t.getAbsolutePath());
		PackageUtils.installSilent(this, t.getAbsolutePath());
	}
	
	class ObainServerMsgThread extends Thread{
		public void run(){
			Log.i(TAG, "start request");
			Message msg = Message.obtain();
			
			try {
				URL url = new URL(apkurl);
				// 联网
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				// 设置请求方法
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(4000);
				int code = conn.getResponseCode();
				if (code == 200) {
					// 联网成功
					InputStream is = conn.getInputStream();
					// 把流转换成字符串String
					String result = StreamUtil.readFromStream(is);
					Log.i(TAG, "result:   " + result);
					
					// JSON解析
					JSONObject obj = new JSONObject(result);
					Log.i(TAG, "is Success ??? ");
					version = (String) obj.get("version");
					Log.i(TAG, version);
					description = (String) obj.get("description");
					Log.i(TAG, description);
					downloadurl = (String) obj.get("downloadurl");
					Log.i(TAG, downloadurl);
					appName = downloadurl.substring(downloadurl.lastIndexOf("/") + 1);
					msg.what = 1;
				}
			} catch (Exception e) {
				e.printStackTrace();
				msg.what = -1;
			} finally {
				
				obtainMsgHandler.sendMessage(msg);
			}
		}
	}

}
