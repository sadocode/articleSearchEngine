package com.sadocode.articlecrawler.article;

import lombok.Data;

@Data
public class Article {
	// contents id
		private String cid;
		
		// section, genre
		private String section;
		
		// 송고 시간
		private String time;
		
		// 기자
		private String author;
		
		// 제목
		private String title;
		
		// 부제목
		private String subtitle;
		
		// 내용
		private String content;
		
		// 기사 키워드
		private String newsKeywords;
		
		public Article()
		{
			// 
		}
}
