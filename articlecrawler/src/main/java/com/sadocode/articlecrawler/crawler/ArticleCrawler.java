package com.sadocode.articlecrawler.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sadocode.articlecrawler.article.Article;
import com.sadocode.articlecrawler.config.Configuration;
import com.sadocode.articlecrawler.log.KLogger;
import com.sadocode.articlecrawler.log.LogData;

/**
 * 기사 하나를 크롤링하는 클래스
 * @author sadocode
 *
 */
public class ArticleCrawler implements Runnable{

	// yna.co.kr 기사 크롤링을 위한 html tags 모음
	private HtmlTags htmlTags;
	
	// 크롤러 설정값
	private Configuration configuration;
	
	// 로거
	private KLogger log;
	
	// 기사의 URL
	private String url;
	
	// 크롤링 후, 초기화되는 기사 정보
	private Article article;
	
	// JSON 데이터로 변경한 기사정보
	private JSONObject articleJSON;
	
	// 저장할 json 파일
	private File file;
	
	// json 메타 이름
	private static final String CID = "akr_cid";
	private static final String SECTION = "akr_section";
	private static final String AUTHOR = "akr_author";
	private static final String NEWS_KEYWORDS = "akr_new_keywords";
	private static final String TIME = "akr_time";
	private static final String TITLE = "akr_title";
	private static final String SUBTITLE = "akr_subtitle";
	private static final String CONTENT = "akr_content";
	private	static final String META_DATA = "akr_meta_data";
	private static final String CONTENT_DATA = "akr_content_data";
	
	// elasticsearch call type & values
	private static final String DOCUMENT = "_doc";
	private static final String PUT = "PUT";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String APPLICATION_JSON = "application/json";
	private static final String ACCEPT_CHARSET = "Accept-Charset";
	private static final String UTF8 = "UTF-8";
	
	
	public ArticleCrawler(String url, HtmlTags htmlTags, Configuration configuration, KLogger log)
	{
		if(url == null || url.length() == 0)
			throw new java.lang.NullPointerException("article url is null.");
		
		this.url = url;
		this.htmlTags = htmlTags;
		this.configuration = configuration;
		this.log = log;
	}
	
	@Override
	public void run()
	{
		try
		{
			this.log.log(new LogData(KLogger.DEBUG, "[[ArticleCrawler RUN]]"));
			this.article = this.getArticle();
			this.articleJSON = this.getJSONObject();
			
			if(this.saveJSONFile())
			{
				// 성공시 elasticsearch 호출
				this.callElasticsearch();
			}
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, "ArticleCrawler failed. url : " + this.url));
			e.printStackTrace();
		}
	}
	
	private Article getArticle()
	{
		this.log.log(new LogData(KLogger.DEBUG, "getArticle() method"));
		
		// 리턴할 기사정보
		Article article = new Article();
		
		try
		{
			// 기사 URL에서 불러올 페이지
			Document articlePage = Jsoup.connect(this.url).get();
		
			// 기사의 내용이 담긴 element
			Elements articleElements = articlePage.select(this.htmlTags.getArticleContent());

		
			// null 일 경우 처리를 위해서
			String tempCid = null;
			String tempSection = null;
			String tempAuthor = null;
			String tempNewsKeywords = null;
			String tempTitle = null;
			String tempSubtitle = null;
			String tempTime = null;
		
			try
			{
				tempCid = articlePage.select(this.htmlTags.getCid()).first().attr(this.htmlTags.getContent());
			}
			catch(NullPointerException npe)
			{
				tempCid = "" + System.currentTimeMillis();
				this.log.log(new LogData(KLogger.DEBUG, "CID NullPointerException CID : " + tempCid));
			}	

			try
			{
				tempSection = articlePage.select(this.htmlTags.getSection()).first().attr(this.htmlTags.getContent());
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "SECTION NullPointerrException"));
				tempSection = "";
			}
			
			try
			{
				tempAuthor = articlePage.select(this.htmlTags.getAuthor()).first().attr(this.htmlTags.getContent());
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "AUTHOR NullPointerrException"));
				tempAuthor = "";
			}
		
			try
			{
				tempNewsKeywords = articlePage.select(this.htmlTags.getNewsKeywords()).first().attr(this.htmlTags.getContent());
				tempNewsKeywords = tempNewsKeywords.replace(',', ' ');
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "NEWS_KEYWORDS NullPointerrException"));
				tempNewsKeywords = "";
			}
			
			try
			{
				tempTitle = articleElements.select(this.htmlTags.getTitle()).text();
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "TITLE NullPointerrException"));
				tempTitle = "";
			}
		
			try
			{
				tempSubtitle = articleElements.select(this.htmlTags.getSubtitle()).text();
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "SUBTITLE NullPointerrException"));
				tempSubtitle = "";
			}
		
			try
			{
//				tempTime = articlePage.select(this.htmlTags.getTime()).first().attr(this.htmlTags.getContent());
				tempTime = articleElements.select(this.htmlTags.getTime()).text();
				tempTime = tempTime.substring(4);
			}
			catch(NullPointerException npe)
			{
				this.log.log(new LogData(KLogger.DEBUG, "TIME NullPointerrException"));
				tempTime = "";
			}
		
			// article의 cid 설정
			article.setCid(tempCid);
			// article의 section 설정
			article.setSection(tempSection);
			// article의 author 설정
			article.setAuthor(tempAuthor);
			// article의 newsKeyword 설정
			article.setNewsKeywords(tempNewsKeywords);
		
			// article의 title 설정
			article.setTitle(tempTitle);
			// article의 subtitle 설정
			article.setSubtitle(tempSubtitle);
			// article의 time 설정
			article.setTime(tempTime);
		
			// article의 content 설정
			Iterator<Element> body = articleElements.select(this.htmlTags.getBody()).iterator();
			StringBuilder tempContent = new StringBuilder();
			while(body.hasNext())
			{
				tempContent.append(body.next().text());
			}
			article.setContent(tempContent.length() > 0 ? tempContent.toString() : "");
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, "getArticle() error. url : " + this.url, e.toString()));
			e.printStackTrace();
		}
		
		return article;
	}
	
	private JSONObject getJSONObject()
	{
		this.log.log(new LogData(KLogger.DEBUG, "getJSONObject() method"));
		JSONObject outer = null;
		
		try
		{
			JSONObject metaData = new JSONObject();
			metaData.put(AUTHOR, this.article.getAuthor());
			metaData.put(TIME, this.article.getTime());
			metaData.put(NEWS_KEYWORDS, this.article.getNewsKeywords());
			metaData.put(SECTION, this.article.getSection());
		
			JSONObject contentData = new JSONObject();
			contentData.put(TITLE, this.article.getTitle());
			contentData.put(SUBTITLE, this.article.getSubtitle());
			contentData.put(CONTENT, this.article.getContent());
		
			outer = new JSONObject();
			outer.put(CID, this.article.getCid());
			outer.put(META_DATA, metaData);
			outer.put(CONTENT_DATA, contentData);
			this.log.log(new LogData(KLogger.DEBUG, "getJSONObject() json : " + outer.toJSONString()));
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, "getJSONObject() error. url : " + this.url, e.toString()));
		}
		return outer;
	}
	
	private boolean saveJSONFile()
	{
		this.log.log(new LogData(KLogger.DEBUG, "saveJSONFile() method"));
		
		Boolean result = false;
		StringBuilder tempPath = new StringBuilder();

		if(this.article.getCid().length() > 1)
		{
			tempPath.append(this.configuration.getSavePath()).append("/").append(this.article.getCid()).append(".json");
		}
		else
		{
			tempPath.append(this.configuration.getSavePath()).append("/").append("errorFile").append(".json");
			this.log.log(new LogData(KLogger.ERROR, "error file saved."));
		}
		
		this.file = new File(tempPath.toString());
		
		try(FileWriter fw = new FileWriter(file, true))
		{
			fw.write(this.articleJSON.toJSONString());
			fw.flush();
			result = true;
			this.log.log(new LogData(KLogger.INFO, "json file saved successfully. file name : " + tempPath.toString()));
		}
		catch(Exception e)
		{
			this.log.log(new LogData(KLogger.ERROR, e.toString()));
			e.printStackTrace();
		}
		
		return result;
	}
	
	private void callElasticsearch()
	{
		this.log.log(new LogData(KLogger.DEBUG, "callElasticsearch() method"));
		
		StringBuilder elasticUrl = null;
		OutputStream os = null;
		BufferedReader in = null;
		String inputLine = null;
		StringBuffer sb = new StringBuffer();
		String tempLine = null;
		try
		{
			elasticUrl = new StringBuilder(this.configuration.getElasticsearchUrl());
			if(elasticUrl == null || elasticUrl.length() == 0)
				throw new Exception();
			
			elasticUrl.append("/").append(this.configuration.getElasticsearchIndex()).append("/").append(DOCUMENT).append("/").append(this.article.getCid());
			URL sendUrl = new URL(elasticUrl.toString());
			
			HttpURLConnection conn = (HttpURLConnection) sendUrl.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(PUT);
			conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
			conn.setRequestProperty(ACCEPT_CHARSET, UTF8);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			
			os = conn.getOutputStream();
			os.write(this.articleJSON.toJSONString().getBytes(UTF8));
			os.flush();
			
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF8));
			while((inputLine = in.readLine()) != null)
			{
				tempLine = inputLine.replaceAll("\n", " ");
				sb.append(tempLine);
			}
			
			conn.disconnect();
			
			this.log.log(new LogData(KLogger.INFO, "##call API Success. response : " + sb.toString()));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			this.log.log(new LogData(KLogger.ERROR, "callElasticsearch() error. request: " + elasticUrl.toString() + " ", e.toString()));
		}
	}
}
