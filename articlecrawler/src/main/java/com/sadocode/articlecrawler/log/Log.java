package com.sadocode.articlecrawler.log;

/**
 * Log 인터페이스
 * @author 황경진
 *
 */
public interface Log {
	public int getLogLevel();
	public String getLogTime();
	public String getLogMsg();
}
