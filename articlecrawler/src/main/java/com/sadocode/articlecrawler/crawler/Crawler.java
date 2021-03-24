package com.sadocode.articlecrawler.crawler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.sadocode.articlecrawler.config.Configuration;
import com.sadocode.articlecrawler.log.KLogger;
import com.sadocode.articlecrawler.log.LogData;

/**
 * 최신기사 페이지를 크롤링하는 클래스
 * 최신기사 목록에 변경이 있을 경우(ex-새로운 기사가 추가된 경우), ArticleCrawler를 호출함
 * @author sadocode
 *
 */
@Service("crawler")
public class Crawler implements Runnable{

	// 크롤링을 위해 필요한 html tags
	@Resource(name = "htmlTags")
	private HtmlTags htmlTags;
	
	// 크롤러 설정값
	@Resource(name = "configuration")
	private Configuration configuration;

	//@Resource(name = "kLogger")
	private KLogger log;
	
	// 최신기사 25개를 저장하는 리스트
	private List<String> articleUrlList;
	
	// 크롤링해야하는 타겟 기사를 저장하는 리스트(25개 이하임)
	private List<String> targetArticleUrlList;
	
	// 최신기사 페이지
	private Document recentArticleDocument;
	
	
	// 가장 최근에 작업한 기사 url
	private String targetArticleUrl;
	
	// 기사 각각의 url에 붙여줄 prefix
	private static final String PREFIX = "https:";

	@Override
	public void run()
	{
		// injection하지 않고 사용하려고 넣은 소스
		this.log = new KLogger("cralwer");
		Thread logThread = new Thread(this.log);
		logThread.start();
		
		this.log.log(new LogData(KLogger.INFO, "[[Crawler RUN]]"));
		while(!Thread.currentThread().isInterrupted())
		{
			this.readPage();
			this.setTargetArticleUrlList();
			
			if(this.targetArticleUrlList.size() != 0)
			{
				this.crawlingArticle();
			}
			
			try
			{
				Thread.sleep(this.configuration.getCoolTime() * 1000);
			}
			catch(InterruptedException ie)
			{
				this.log.log(new LogData(KLogger.ERROR, ie.toString()));
				ie.printStackTrace();
				this.log.terminate();
				this.log.log(new LogData(KLogger.INFO, "[[KLogger terminated]]"));
			}
		}
	}
	
	/**
	 * 최신기사 페이지를 읽어오는 메소드
	 */
	private void readPage()
	{
		this.log.log(new LogData(KLogger.DEBUG, "readPage() method"));
		try
		{
			this.recentArticleDocument = Jsoup.connect(this.configuration.getRecentArticleUrl()).get();
			Elements articleListElement = this.recentArticleDocument.select(this.htmlTags.getArticleListElement());
			Iterator<Element> articleIterator = articleListElement.select(this.htmlTags.getArticleUrl()).iterator();
			
			this.initArticleUrlList();
			
			String temp = null;
			while(articleIterator.hasNext())
			{
				temp = articleIterator.next().attr(this.htmlTags.getHref());
				this.articleUrlList.add(temp);
			}
		}
		catch(IOException ioe)
		{
			this.log.log(new LogData(KLogger.ERROR, ioe.toString()));
			ioe.printStackTrace();
		}
	}
	
	private void setTargetArticleUrlList()
	{
		this.log.log(new LogData(KLogger.DEBUG, "setTargetArticleUrlList() method"));
		int index = 0;
		this.initTargetArticleUrlList();
		
		// 첫 크롤링일 경우
		if(this.targetArticleUrl == null)
		{
			// articleUrlList의 모든 기사를 크롤링하기위한 index설정
			index = this.articleUrlList.size();
		}
		// 2번째 크롤링부터
		else
		{
			// articleUrlList 중 index 보다 작은 기사를 크롤링하기위해 index설정
			index = this.articleUrlList.indexOf(this.targetArticleUrl);
		}
		
		for(int i = 0; i < index; i++)
		{
			this.targetArticleUrlList.add(PREFIX + this.articleUrlList.get(i));
		}
		
		if(index != 0)
			this.targetArticleUrl = this.articleUrlList.get(0);
	}
	
	private void crawlingArticle()
	{
		this.log.log(new LogData(KLogger.DEBUG, "crawlingArticle() method"));
		int size = this.targetArticleUrlList.size();
		
		for(int i = 0; i < size; i++)
		{
			this.loadArticleCrawler(this.targetArticleUrlList.get(i));
		}
	}
	
	private void loadArticleCrawler(String url)
	{
		this.log.log(new LogData(KLogger.DEBUG, "loadArticleCrawler() method"));
		ArticleCrawler articleCrawler = new ArticleCrawler(url, this.htmlTags, this.configuration, this.log);
		Thread thread = new Thread(articleCrawler);
		thread.start();
	}
	
	private void initArticleUrlList()
	{
		this.log.log(new LogData(KLogger.DEBUG, "initArticleUrlList() method"));
		this.articleUrlList = new LinkedList<String>();
	}
	
	private void initTargetArticleUrlList()
	{
		this.log.log(new LogData(KLogger.DEBUG, "initTargetArticleUrlList() method"));
		this.targetArticleUrlList = new LinkedList<String>();
	}
}
