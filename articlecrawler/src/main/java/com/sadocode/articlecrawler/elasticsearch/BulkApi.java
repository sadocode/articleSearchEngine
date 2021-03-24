package com.sadocode.articlecrawler.elasticsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sadocode.articlecrawler.log.KLogger;
import com.sadocode.articlecrawler.log.LogData;

public class BulkApi implements Runnable{

//	private static final String DIR_PATH = "C:/app/bulk";
//	private static final String DIR_PATH = "C:/app";
	private static final String DIR_PATH = "C:/app2/bulk";
	private static final String WRITE_PATH = "C:/app/bulk";

	private static final String URL = "http://localhost:9200/yna_akr/_doc/";
	private static final String PUT = "PUT";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCEPT_CHARSET = "Accept-Charset";
	private static final String UTF8 = "UTF-8";
	
	
	
	private KLogger log;
	
	private static final String CID = "cid";
	private static final String AKR_CID = "akr_cid";
	private static final String META_DATA = "metaData";
	private static final String AKR_META_DATA = "akr_meta_data";
	private static final String CONTENT_DATA = "contentData";
	private static final String AKR_CONTENT_DATA = "akr_content_data";
	private static final String AUTHOR = "author";
	private static final String AKR_AUTHOR = "akr_author";
	private static final String NEWS_KEYWORDS = "newsKeywords";
	private static final String AKR_NEWS_KEYWORDS = "akr_news_keywords";
	private static final String SECTION = "section";
	private static final String AKR_SECTION = "akr_section";
	private static final String TIME = "time";
	private static final String AKR_TIME = "akr_time";
	private static final String TITLE = "title";
	private static final String AKR_TITLE = "akr_title";
	private static final String SUBTITLE = "subtitle";
	private static final String AKR_SUBTITLE = "akr_subtitle";
	private static final String CONTENT = "content";
	private static final String AKR_CONTENT = "akr_content";
	
	public BulkApi()
	{
	}
	
	@Override
	public void run()
	{
		this.log = new KLogger("bulk");
		Thread thread = new Thread(this.log);
		thread.start();
		this.log.log(new LogData(KLogger.INFO, "[[[RUN]]]"));
		
		this.readFiles();
	}

	private void readFiles()
	{
		File dir = new File(DIR_PATH);
		File[] fileList = dir.listFiles();
		
		String fileName = null;
		JSONParser parser = new JSONParser();
		Object obj = null;
		JSONObject jo = null;
		
		JSONObject jo1 = null;
		JSONObject jo2 = null;
		
		String cid = null;
		String metaData = null;
		String author = null;
		String newsKeywords = null;
		String section = null;
		String time = null;
		String contentData = null;
		String title = null;
		String subtitle = null;
		String content = null;
		
		JSONObject result = null;
		JSONObject metaResult = null;
		JSONObject contentResult = null;
		
		for(File file : fileList)
		{
			try
			{
				if(file.isFile())
				{
					fileName = file.getName();
					System.out.println(fileName);
					
					
					try(FileReader fr = new FileReader(file))
					{
						obj = parser.parse(fr);
						jo = (JSONObject) obj;
						
						cid = jo.get(AKR_CID).toString();
						/* // 이건 잘못 등록된? 그 json key 값 이상했던 애들 대상임.
						jo1 = (JSONObject) jo.get(META_DATA);
						jo2 = (JSONObject) jo.get(CONTENT_DATA);
						
						cid = jo.get(CID).toString();
						author = jo1.get(AUTHOR).toString();
						time = jo1.get(TIME).toString();
						section = jo1.get(SECTION).toString();
						newsKeywords = jo1.get(NEWS_KEYWORDS).toString();
						
						title = jo2.get(TITLE).toString();
						subtitle = jo2.get(SUBTITLE).toString();
						content = jo2.get(CONTENT).toString();
						
						result = new JSONObject();
						metaResult = new JSONObject();
						metaResult.put(AKR_AUTHOR, author);
						metaResult.put(AKR_SECTION, section);
						metaResult.put(AKR_NEWS_KEYWORDS, newsKeywords);
						metaResult.put(AKR_TIME, time);
						contentResult = new JSONObject();
						contentResult.put(AKR_TITLE, title);
						contentResult.put(AKR_SUBTITLE, subtitle);
						contentResult.put(AKR_CONTENT, content);
						result.put(AKR_CID, cid);
						result.put(AKR_META_DATA, metaResult);
						result.put(AKR_CONTENT_DATA, contentResult);
						
						//this.writeFile(fileName, result);
						*/
						
						// 이건 하나씩 쏴주기 위해서임.
						this.log.log(new LogData(KLogger.INFO, "fileRead success. fileName : " + fileName));
						this.callElasticsearchAPI(cid, jo);
					}
					catch(Exception e)
					{
						this.log.log(new LogData(KLogger.ERROR, "@@fileRead Error. fileName : " + fileName + e.toString()));
					}
					
//					System.out.println(jo.toJSONString());
				}
			}
			catch(Exception e)
			{
				this.log.log(new LogData(KLogger.ERROR, "##fileRead Error. fileName : " + fileName + e.toString()));
			}
		}
	}
	private void writeFile(String fileName, JSONObject jsonData)
	{
		File file = new File(WRITE_PATH + "/" + fileName);
		try(FileWriter fw = new FileWriter(file))
		{
			fw.write(jsonData.toJSONString());
			fw.flush();
			this.log.log(new LogData(KLogger.INFO, "##filewrite fin. fileName : " + fileName));
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, "##fileWrite Error. fileName : " + fileName));
		}
	}
	private void callElasticsearchAPI(String cid, JSONObject jsonData)
	{
		OutputStream os = null;
		BufferedReader in = null;
		String inputLine = null;
		StringBuffer sb = new StringBuffer();
		try
		{
			 
			URL sendUrl = new URL(URL + cid);
			HttpURLConnection conn = (HttpURLConnection) sendUrl.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(PUT);
			conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
			conn.setRequestProperty(ACCEPT_CHARSET, UTF8);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			
			os = conn.getOutputStream();
			os.write(jsonData.toJSONString().getBytes(UTF8));
			os.flush();
			
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF8));
			while((inputLine = in.readLine()) != null)
			{
				sb.append(inputLine);
			}
			
			conn.disconnect();
			
//			System.out.println(jsonData.toJSONString());
			this.log.log(new LogData(KLogger.INFO, "##call API Success. cid : " + cid));
			this.log.log(new LogData(KLogger.INFO, sb.toString()));
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, "##call API ERROR. cid : " + cid));
		}
	}
}
