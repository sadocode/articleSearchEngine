package com.sadocode.articlecrawler.crawler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Repository;

import lombok.Data;

/**
 * 크롤링을 위한 html tag 모음
 * application.properties에서 변수값을 읽어온다.
 * 
 * @author sadocode
 *
 */
@Repository("htmlTags")
@Data
@PropertySource("classpath:tag.properties")
public class HtmlTags {
//	@Value("#{properties['crawling.crawler.article_list_element']}")
	@Value("${crawling.crawler.article_list_element}")
	public String articleListElement;
	
//	@Value("#{properties['crawling.crawler.article_url']}")
	@Value("${crawling.crawler.article_url}")
	public String articleUrl;
	
//	@Value("#{properties['crawling.crawler.href']}")
	@Value("${crawling.crawler.href}")
	public String href;
	
	
	/**                                     */
	
//	@Value("#{properties['crawling.article.article_content']}")
	@Value("${crawling.article.article_content}")
	public String articleContent;
	
//	@Value("#{properties['crawling.article.title']}")
	@Value("${crawling.article.title}")
	public String title;
	
//	@Value("#{properties['crawling.subtitle']}")
	@Value("${crawling.article.subtitle}")
	public String subtitle;
	
//	@Value("#{properties['crawling.article.body']}")
	@Value("${crawling.article.body}")
	public String body;
	
//	@Value("#{properties['crawling.article.cid']}")
	@Value("${crawling.article.cid}")
	public String cid;
	
//	@Value("#{properties['crawling.article.section']}")
	@Value("${crawling.article.section}")
	public String section;
	
//	@Value("#{properties['crawling.article.content']}")
	@Value("${crawling.article.content}")
	public String content;
	
//	@Value("#{properties['crawling.article.news_keywords']}")
	@Value("${crawling.article.news_keywords}")
	public String newsKeywords;
	
//	@Value("#{properties['crawling.article.author']}")
	@Value("${crawling.article.author}")
	public String author;
	
//	@Value("#{properties['crawling.article.time']}")
	@Value("${crawling.article.time}")
	public String time;

	
}
