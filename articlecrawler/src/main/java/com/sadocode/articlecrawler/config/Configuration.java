package com.sadocode.articlecrawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Repository;

import lombok.Data;

@Data
@Repository("configuration")
@PropertySource("classpath:application.properties")
public class Configuration {

	@Value("${crawling.save_path}")
	private String savePath;
	
	@Value("${crawling.cool_time}")
	private int coolTime;
	
	@Value("${crawling.recent_article_url}")
	private String recentArticleUrl;
	
	@Value("${elasticsearch.url}")
	private String elasticsearchUrl;
	
	@Value("${elasticsearch.index_name}")
	private String elasticsearchIndex;
}
