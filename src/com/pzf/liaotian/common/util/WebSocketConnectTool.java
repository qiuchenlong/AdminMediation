package com.pzf.liaotian.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.bither.util.NativeUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.pzf.liaotian.PublicChatActivity;
import com.pzf.liaotian.UploadUtil;
import com.pzf.liaotian.adapter.MessageAdapter;
import com.pzf.liaotian.app.PushApplication;
import com.pzf.liaotian.bean.MessageItem;
import com.pzf.liaotian.bean.RecentItem;
import com.pzf.liaotian.db.MessageDB;
import com.pzf.liaotian.db.RecentDB;
import com.pzf.liaotian.xlistview.MsgListView;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class WebSocketConnectTool extends WebSocketConnection {

	private static SharePreferenceUtil mSpUtil;
	  
	
    /**
     * 内部类实现单例模式
     * 延迟加载，减少内存开销
     * 
     * @author xuzhaohu
     * 
     */
    private static class SingletonHolder {
        private static WebSocketConnectTool websocket = new WebSocketConnectTool();
    }

    /**
     * 私有的构造函数
     */
    private WebSocketConnectTool() {

    }

    public static WebSocketConnectTool getInstance() {
        return SingletonHolder.websocket;
    }

    public void handleConnection(File file) {
    	mSpUtil = PushApplication.getInstance().getSpUtil();
    	
    	final String wsuri = mSpUtil.getServerIP();
    	
        if (!SingletonHolder.websocket.isConnected()) {
			
	    	  try {
	    		  SingletonHolder.websocket.connect(wsuri, new WebSocketHandler() {
			        	
			            @Override
			            public void onOpen() {
			               Log.d("chat", "Status: Connected to " + wsuri);	
			               PublicChatActivity.sendTextMessage(mSpUtil.getNick()+",进入聊天室",true);
			       	    }
			            
			            @Override
						public void onTextMessage(String payload) {
			            	Log.d("chat", "Got echo: " + payload);
				            String filetype = null;
				            if (payload.contains(".")) {
				            	filetype = payload.substring(payload.lastIndexOf("."));
								Log.d("chat", "Got echo filetype: " + filetype);
					               
					            UploadUtil.mUserName = "被诉人";
					            UploadUtil.mUserID = "100000";
					            UploadUtil.mFileType = filetype;
					            UploadUtil.mVoiceLength = 16;
					               
					               //如果是私聊则不接受消息，因为只有协调员可以看到
					            if (UploadUtil.isPrivateChat == 1) {
									return;
								}
					               
					            UploadUtil.handleMessage(payload);
				            }
			            }
			            
			            @Override
			            public void onClose(int code, String reason) {
			               Log.d("chat", "Connection lost.");
			               PublicChatActivity.sendTextMessage(mSpUtil.getNick()+",退出聊天室",true);	
			            }
			         });
			      } catch (WebSocketException e) {
			 
			         Log.d("chat", e.toString());
			      }
			} else {
				if (file != null) {
					sendMessage(file);	
				}						     					
			}
    }
    
    public static void sendMessage(File file) {
		
		   InputStream inputStream = null;
		   
			try {
				inputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}  
			
         ByteArrayOutputStream swapStream = new ByteArrayOutputStream(); 
         byte[] buff = new byte[100]; //buff用于存放循环读取的临时数据 
         int rc = 0; 
         
         try {
				while ((rc = inputStream.read(buff, 0, 100)) > 0) { 
				   swapStream.write(buff, 0, rc); 
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
         
         byte[] in_b = swapStream.toByteArray(); //in_b为转换之后的结果 
                    
        
         
         String fileName = file.toString();
         String suffixName = null;
         if (fileName.contains(".")) {
				suffixName = fileName.substring(fileName.length()-4, fileName.length());
			}
         
         
        //判断要传送文件的格式  		   
		   if (file.toString().contains(".jpg") || file.toString().contains(".png")) {
			   //发送图片 
			   
			   try {
				   inputStream = new FileInputStream(file);
			   } catch (FileNotFoundException e) {
				   e.printStackTrace();
			   }  
			 
			   Bitmap btp = BitmapFactory.decodeStream(inputStream); 
            btp = NativeUtil.compressBitmap(btp, 50,null, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();    
            btp.compress(Bitmap.CompressFormat.JPEG, 40, baos); 
                           
            //需要发送的信息：姓名、用户ID、文件类型、是否是悄悄话、音频长度
            String encodeString = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            String user_info = getUserJsonInfo(".jpg",encodeString);
//            byte[] in_all = byteMerger(user_info.getBytes(),);
           
            SingletonHolder.websocket.sendTextMessage(user_info);
		   } else {
			   String encodeString = Base64.encodeToString(in_b, Base64.NO_WRAP);
			   String user_info = getUserJsonInfo(suffixName,encodeString);
//            byte[] in_all = byteMerger(user_info.getBytes(), in_b);
          
			   SingletonHolder.websocket.sendTextMessage(user_info);
//            mConnection.sendBinaryMessage(in_all);
		   }
		           
        try {
				inputStream.close(); 
        } catch (IOException e) {
				e.printStackTrace();
        }   
	} 
	
	public static String getUserJsonInfo(String filetype,String data) {
		return "{"+ 
				"\"username\":"+
				"\"" +  mSpUtil.getNick() + "\"," +
				"\"userid\":" +
				"\"" + mSpUtil.getUserId() + "\"," + 
				"\"filetype\":" + 
				"\"" + filetype + "\"," +
				"\"isprivatechat\":" + 
				mSpUtil.getIsPrivateChat() + "," +
				"\"voicetime\":" + 
				 mSpUtil.getVoiceTime() + "," +
				 "\"data\":" + 
				 "\"" +data + "\"" +
			  "}";
	}
}






