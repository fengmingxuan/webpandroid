package com.example.webp_example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.webp.libwebp;

public class MainActivity extends Activity {
  static {
    System.loadLibrary("webp");
  }
  String ALBUM_PATH = Environment.getExternalStorageDirectory() + "/webp/";
  String filePath = "https://img.pximg.com/2017/07/beb615c1965bc52.gif!pximg/both/205x277";
//  String filePath ="https://img.pximg.com/2017/07/e37aaaa7dd91117.jpg!pximg/both/205x277";
  private Bitmap mBitmap;
  String mFileName = "test.png";
  ImageView imageView1;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    imageView1 = (ImageView) this.findViewById(R.id.imageView1);
    
    new Thread(connectNet).start();
//    //Read a webp
//    byte[] webpEncodedData = loadFileAsByteArray(ALBUM_PATH+"test.webp");
//    Bitmap bitmap = webpToBitmap(webpEncodedData);
//    imageView1.setImageBitmap(bitmap);
//    
    //Write a webp
//    byte[] webpData = bitmapToWebp(ALBUM_PATH+"test.png");
//    writeFileFromByteArray(ALBUM_PATH+"test.webp", webpData);
  }
	private Handler connectHanlder = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d("", "display image");
			// 更新UI，显示图片
			if (mBitmap != null) {
				imageView1.setImageBitmap(mBitmap);// display image
			}
		}
	};
	
  private byte[] loadFileAsByteArray(String filePath) {
    File file = new File(filePath);
    byte[] data = new byte[(int)file.length()];
    try {
      FileInputStream inputStream;
      inputStream = new FileInputStream(file);
      inputStream.read(data);
      inputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return data;
  }

  private Bitmap webpToBitmap(byte[] encoded) {
    int[] width = new int[] { 0 };
    int[] height = new int[] { 0 };
    byte[] decoded = libwebp.WebPDecodeARGB(encoded, encoded.length, width, height);
    int[] pixels = new int[decoded.length / 4];
    ByteBuffer.wrap(decoded).asIntBuffer().get(pixels);
    return Bitmap.createBitmap(pixels, width[0], height[0], Bitmap.Config.ARGB_8888);
  }

  private byte[] bitmapToWebp(String filePath) {
    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
    int bytes = bitmap.getByteCount();
    ByteBuffer buffer = ByteBuffer.allocate(bytes);
    bitmap.copyPixelsToBuffer(buffer);
    byte[] pixels = buffer.array();

    int height = bitmap.getHeight();
    int width = bitmap.getWidth();
    Log.d("", "height=="+height+";width=="+width+";pixels=="+pixels.length);
    int stride = width * 4;
    int quality = 100;
    byte[] rgb = new byte[3];
    
    for (int y = 0; y < height * 4; y++) {
      for (int x = 0; x < width; x+=4) {
        for (int i = 0; i < 3; i++) {
          int len = x + y * width + i;
//          Log.d("", "y=="+y+";x=="+x+";i=="+i+";len=="+len);
          if(len>=pixels.length){
        	  len = pixels.length-1;
          }
          rgb[i] = pixels[len];
        }
        for (int i = 0; i < 3; i++) {
        	int len = x + y * width + 2 - i;
        	if(len>=pixels.length){
          	  len = pixels.length-1;
            }
            pixels[len] = rgb[i];
        }
      }
    }

    byte[] encoded = libwebp.WebPEncodeBGRA(pixels, width, height, stride, quality);
    return encoded;
  }
  
  private void writeFileFromByteArray(String filePath, byte[] data) {
    File webpFile = new File(filePath);
    BufferedOutputStream bos;
    try {
      bos = new BufferedOutputStream(new FileOutputStream(webpFile));
      bos.write(data);
      bos.flush();
      bos.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /*
	 * 连接网络 由于在4.0中不允许在主线程中访问网络，所以需要在子线程中访问
	 */
	private Runnable connectNet = new Runnable() {
		@Override
		public void run() {
			try {
				// 以下是取得图片的两种方法
				// ////////////// 方法1：取得的是byte数组, 从byte数组生成bitmap
				byte[] data = getImage(filePath);
				if (data != null) {
					try {
//						GifDrawable gifDrawable = new GifDrawable(getResources(), R.drawable.anim_flag_iceland);
//						GifDrawable gifDrawable = new GifDrawable(data);
//						gifImageView.setImageDrawable(gifDrawable);
					} catch (Exception e) {
						e.printStackTrace();
					}
				    mBitmap = webpToBitmap(data);
				} else {
					Log.d("", "data===null");
				}
				// //////////////////////////////////////////////////////
				//
				// //******** 方法2：取得的是InputStream，直接从InputStream生成bitmap
				// ***********/
//				 mBitmap =
//				 BitmapFactory.decodeStream(getImageStream(filePath));
				// //********************************************************************/

				// 发送消息，通知handler在主线程中更新UI
				connectHanlder.sendEmptyMessage(0);
				Log.d("", "set image ...");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};

	/**
	 * Get image from newwork
	 * 
	 * @param path
	 *            The path of image
	 * @return byte[]
	 * @throws Exception
	 */
	@SuppressLint("NewApi") public byte[] getImage(String path) throws Exception {
		try {
			URL url = null;
	        try {
	            url = new URL(path);
	        } catch (MalformedURLException e) {
	            Log.e("getStreamFromNetwork", e.getMessage(), e);
	        }
	        HttpURLConnection conn = null;

	        if (path.startsWith("https")) {
	            trustAllHosts();
	            HttpsURLConnection https;
	            
	            https = (HttpsURLConnection) url
	                    .openConnection();
	            https.setHostnameVerifier(DO_NOT_VERIFY);
	            
//	            https.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");  
//	            https.setRequestProperty("Upgrade-Insecure-Requests", "1");  
//	            https.setRequestProperty("Host", "img.pximg.com");
//	            https.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");  
//	            https.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");  
//	            https.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
//	            https.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
//	            https.setRequestProperty("Cache-Control", "max-age=0");
//	            https.setRequestProperty("Connection", "keep-alive");
	            https.setRequestProperty("referer", "img.pximg.com");
//	            https.setRequestProperty("Access-Control-Allow-Origin", "*");
//	            https.setRequestProperty("Origin", "img.pximg.com");
	            conn = https;
	            conn.connect();
	        } else {
	        	conn = (HttpURLConnection) url.openConnection();
	        }
	        
	        conn.setReadTimeout(5 * 1000);
			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod("GET");
			
			conn.getResponseCode();
		    InputStream inStream;
//		    if (inStream == null) {
		    	inStream = conn.getInputStream();
//		    }
		 // This is a try with resources, Java 7+ only
		    // If you use Java 6 or less, use a finally block instead
//		    try (Scanner scanner = new Scanner(inStream)) {
//		        scanner.useDelimiter("\\Z");
//		         System.out.print(scanner.next());
//		    }
		    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
		    	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, len);
				}
				outStream.close();
				inStream.close();
//				return outStream.toByteArray();
		    	
//		    	 BufferedInputStream bis = null;  
//		         ByteArrayOutputStream out =null;  
//		         try {  
//		             bis = new BufferedInputStream(inStream,1024);  
//		             out = new ByteArrayOutputStream();  
//		             int len=0;  
//		             byte[] buffer = new byte[1024];  
//		             while((len = bis.read(buffer)) != -1){  
//		                 out.write(buffer, 0, len);  
//		             }  
//		             out.close();  
//		             bis.close();  
//		         } catch (MalformedURLException e1) {  
//		             e1.printStackTrace();  
//		         } catch (IOException e) {  
//		             e.printStackTrace();  
//		         }  
//		         
		        File dirFile = new File(ALBUM_PATH);
		 		if (!dirFile.exists()) {
		 			dirFile.mkdir();
		 		}
		 		File myCaptureFile = new File(ALBUM_PATH +mFileName);
		 		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
		 		bos.write(outStream.toByteArray());
		 		bos.flush();
		 		bos.close();
		         return outStream.toByteArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	/**
	 * Get image from newwork
	 * 
	 * @param path
	 *            The path of image
	 * @return InputStream
	 * @throws Exception
	 */
	@SuppressLint("NewApi") public InputStream getImageStream(String path) throws Exception {
		URL url = null;
      try {
          url = new URL(path);
      } catch (MalformedURLException e) {
          Log.e("getStreamFromNetwork", e.getMessage(), e);
      }
      HttpURLConnection conn = null;

      if (path.startsWith("https")) {
          trustAllHosts();
          HttpsURLConnection https;
          
          https = (HttpsURLConnection) url
                  .openConnection();
          https.setHostnameVerifier(DO_NOT_VERIFY);
          
//          https.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");  
//          https.setRequestProperty("Upgrade-Insecure-Requests", "1");  
//          https.setRequestProperty("Host", "img.pximg.com");
//          https.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.75 Safari/537.36 QQBrowser/4.1.4132.400");  
////          https.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");  
//          https.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
//          https.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
//          https.setRequestProperty("Cache-Control", "max-age=0");
//          https.setRequestProperty("Connection", "keep-alive");
          https.setRequestProperty("referer", "img.pximg.com");
          conn = https;
          conn.connect();
      } else {
      	conn = (HttpURLConnection) url.openConnection();
      }
      
      conn.setReadTimeout(5 * 1000);
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("GET");
		
		conn.getResponseCode();
	    InputStream inStream;
//	    if (inStream == null) {
	    	inStream = conn.getInputStream();
//	    }
	 // This is a try with resources, Java 7+ only
	    // If you use Java 6 or less, use a finally block instead
//	    try (Scanner scanner = new Scanner(inStream)) {
//	        scanner.useDelimiter("\\Z");
//	         System.out.print(scanner.next());
//	    }
		return inStream;
	}

	   // always verify the host - dont check for certificate
  final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, SSLSession session) {
          return true;
      }
  };
  
	/**
   * Trust every server - dont check for any certificate
   */
  private static void trustAllHosts() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
          @Override
          public void checkClientTrusted(
                  java.security.cert.X509Certificate[] x509Certificates,
                  String s) throws java.security.cert.CertificateException {
          }

          @Override
          public void checkServerTrusted(
                  java.security.cert.X509Certificate[] x509Certificates,
                  String s) throws java.security.cert.CertificateException {
          }

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return new java.security.cert.X509Certificate[]{};
          }
      }};

      // Install the all-trusting trust manager
      try {
          SSLContext sc = SSLContext.getInstance("TLS");
          sc.init(null, trustAllCerts, new java.security.SecureRandom());
          HttpsURLConnection
                  .setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
}
