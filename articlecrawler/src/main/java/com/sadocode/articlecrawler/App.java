package com.sadocode.articlecrawler;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sadocode.articlecrawler.crawler.Crawler;
import com.sadocode.articlecrawler.elasticsearch.BulkApi;

/**
 * ArticlecrawlerApplication 원래 이름
 * @author sadocode
 *
 */
@SpringBootApplication
public class App implements CommandLineRunner{

	@Resource(name="crawler")
	private Crawler crawler;
	
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String[] args)
	{
		Thread thread = new Thread(this.crawler);
		thread.start();

//		 bulk API 용
//		BulkApi bulk = new BulkApi();
//		Thread thread = new Thread(bulk);
//		thread.start();
	}
}