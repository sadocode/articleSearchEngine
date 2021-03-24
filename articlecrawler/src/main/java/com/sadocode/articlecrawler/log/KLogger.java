package com.sadocode.articlecrawler.log;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * Log인터페이스를 queue에 담아 비동기로 처리하는 클래스.<br>
 * 1.1 : Spring project에서 KLogger를 가져다 쓸 수 있도록 수정.
 * 		 의존성 주입을 통해 사용할 경우, 여러 곳에서 KLogger를 선언하면 안 된다.(클래스 변수들 때문에 thread-safe하지 않음)
 * 1.2 : 버그 때문에 1.1 제거함...	
 * @version 1.2
 * @author 황경진
 *
 */
public class KLogger extends Thread{

	/** 로그 이름*/
	public String logName;
	
	/** 로그 레벨 */
	public static final int DEBUG = 0;
	public static final int INFO = 1;
	public static final int WARN = 2;
	public static final int ERROR = 3;
	public static final int FATAL = 4;
	
	/** default 로그 레벨 */
	public static final String DEFAULT_LEVEL = "INFO";
	
	/** 로그레벨 String 저장 Map */
	public static Map<Integer, String> levelMap;
	
	static
	{
		levelMap = new HashMap<Integer, String>();
		levelMap.put(0, "DEBUG");
		levelMap.put(1, "INFO");
		levelMap.put(2, "WARN");
		levelMap.put(3, "ERROR");
		levelMap.put(4, "FATAL");
	}
	
	/** properties에서  설정으로 가져올 값. DI하지 않을 경우, 직접 properties파일에 접근하여 해당 값들을 가져온다. */
	public static final String PROPERTIES_FILE_NAME = "D:\\dockerOutput\\els\\articlecrawler\\src\\main\\resources\\log.properties";
	public static final String LOG_FOLDER_NAME = "klog.dir";
	public static final String LOG_LEVEL = "klog.loglevel";
	
	/** DI해서 쓸 경우, properties 파일에서 가져올 값들*/
//	@Value("${klog.dir}")
//	private String logFolderName;
//	@Value("${klog.loglevel}")
//	private String logLevel;
	
	/** logfile의 폴더 */
	private String logDir;
	
	/** logger의 레벨. 해당 값 이상만 로그 파일에 찍힌다. */
	public int loggerLevel;

	/** 로그를 파일에 기록할 때 사용하는 변수들 */
	private StringBuilder logMsg;
	private Date date;
	private SimpleDateFormat sdf;
	private final static String FILE_SUFFIX = ".log";
	private StringBuilder fileName;
	
	/** 로그를 담을 큐 */
	private List<Log> queue;
	
	/** 로그가 종료되었는지 여부 */
	private boolean isEnd;
	
	/**
	 * KLogger 생성자<br>
	 * properties 파일을 직접 읽고, 설정을 한다.<br>
	 * @param logName 로거 이름
	 */
	public KLogger(String logName)
	{
		if(logName == null || logName.length() == 0)
			throw new NullPointerException();
		
		this.logName = logName;
		this.init();
		this.loadProperties(false);
	}
	
	/**
	 * KLogger 생성자<br>
	 * properties 파일을 직접 읽고, 설정을 한다.<br>
	 * isInjection이 true이면 property를 직접 로드하지 않는다.<br>
	 * isInjection이 false이면 property를 직접 로드한다.<br>
	 * @param logName 로거 이름
	 * @param isInjection DI 해서 사용할 것인지 여부
	 */
	/**
	public KLogger(@Value("${klog.logname}") String logName, @Value("${klog.isInjection}") boolean isInjection)
	{
		if(logName == null || logName.length() == 0)
			throw new NullPointerException();
		
		this.logName = logName;
		this.init();
		this.loadProperties(isInjection);
	}
	*/
	
	/**
	 * KLogger 생성자 <br>
	 * properties 파일을 읽지 않는다. 인자로 받은 값들을 그대로 사용.<br>
	 * logLevel 인자는 KLogger의 public static 변수들을 참고한다.<br>
	 * 0 : DEBUG, 1 : INFO, 2 : WARN, 3: ERROR, 4: FATAL
	 * @param logName 로거 이름
	 * @param logPath 로그 파일을 저장할 상위 폴더명
	 * @param logLevel 로그 레벨
	 */
	public KLogger(String logName, String logPath, int logLevel)
	{
		if(logName == null || logName.length() == 0 || logPath == null || logPath.length() == 0)
			throw new NullPointerException();
		if(logLevel < DEBUG || logLevel > FATAL)
			throw new IllegalArgumentException();
		
		this.logName = logName;
		this.logDir = logPath;
		this.loggerLevel = logLevel;
		this.init();
	}
	
	/**
	 * KLogger 생성자 <br>
	 * properties 파일을 읽지 않는다. 인자로 받은 값들을 그대로 사용.<br>
	 * logLevel 인자는 KLogger의 public static 변수들을 참고한다.<br>
	 * 입력 가능한 logLevel : DEBUG, INFO, WARN, ERROR, FATAL
	 * @param logName 로거 이름
	 * @param logPath 로그 파일을 저장할 상위 폴더명
	 * @param logLevel 로그 레벨
	 */
	public KLogger(String logName, String logPath, String logLevel)
	{
		if(logName == null || logName.length() == 0 || logPath == null || logPath.length() == 0)
			throw new NullPointerException();
		
		this.logName = logName;
		this.logDir = logPath;
		this.setLoggerLevel(logLevel);
		this.init();
	}
	
	/**
	 * init 메소드 <br>
	 * KLogger가 생성될 때, 실행된다.
	 */
	private void init()
	{
		this.queue = new LinkedList<Log>();
		this.logMsg = new StringBuilder();
		this.sdf = new SimpleDateFormat("yyyyMMdd");
		this.fileName = new StringBuilder();
	}

	@Override
	public void run()
	{
		while(!Thread.currentThread().isInterrupted())
		{
			synchronized(this.queue)
			{
				if(this.isEnd)
				{
					System.out.println("KLogger 종료");
					break;
				}
				try(PrintWriter pw = this.getWriter())
				{
					while(!this.queue.isEmpty())
					{
						System.out.println("Queue is not empty");
						this._log(this.queue.remove(0), pw);
					}
					
					System.out.println("Queue is empty");
					this.queue.wait();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		System.out.println("Queue END");
	}
	
	/**
	 * 외부(다른 클래스)에서 log를 기록할 때 호출된다.
	 * @param log 로그 정보
	 */
	public void log(Log log)
	{
		synchronized(this.queue)
		{
			this.queue.add(log);
			this.queue.notify();
		}
	}
	
	/**
	 * 로그를 종료하는 메소드<br>
	 * 로그가 종료될 때, 호출해줘야함!
	 */
	public void terminate()
	{
		System.out.println("terminate()");
		synchronized(this.queue)
		{
			this.isEnd = true;
			this.queue.notify();
		}
	}
	
	/**
	 * log를 로그 파일에 기록한다.<br>
	 * KLogger에 설정된 로그레벨보다 낮은 레벨의 로그는 기록하지 않는다.
	 * @param log 로그 정보
	 * @param pw PrintWriter
	 */
	private void _log(Log log, PrintWriter pw)
	{
		if(log == null)
			return;

		if(log.getLogLevel() < this.loggerLevel)
			return;
		
		this.logMsg.setLength(0);
		this.logMsg.append(log.getLogTime()).append(" [").append(levelMap.get(log.getLogLevel())).append("] ").append(log.getLogMsg()).append("\n");
		System.out.println(logMsg);
		pw.print(logMsg.toString());
		pw.flush();
	}
	
	/**
	 * PrintWriter를 리턴한다.<br>
	 * 멀티 쓰레드 환경을 고려하여, run()에서 큐를 조회할 때마다 호출하게 된다.
	 * @return PrintWriter printWriter
	 */
	private PrintWriter getWriter()
	{
		this.date = new Date(System.currentTimeMillis());
		File logDirFile = null;
		File logFile = null;
		this.fileName.setLength(0);
		this.fileName.append(this.logName).append("_").append(this.sdf.format(date)).append(FILE_SUFFIX);
		
		PrintWriter pw = null;
		
		try
		{
			logDirFile = new File(this.logDir);
			
			if(!logDirFile.exists())
				logDirFile.mkdir();

			logFile = new File(logDirFile, fileName.toString());
			
			pw = new PrintWriter(Files.newBufferedWriter(logFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return pw;
	}
	
	/**
	 * properties 파일에서 설정값을 가져온다.<br>
	 * KLogger가 생성될 때, 1번 호출된다. 단, KLogger(logName) 생성자를 생성할 때만 호출됨.
	 * @param isInjection DI 여부
	 */
	private void loadProperties(Boolean isInjection)
	{
		if(isInjection)
		{
//			this.logDir = this.logFolderName;
//			this.setLoggerLevel(this.logLevel);
		}
		else
		{
			try(FileInputStream fis = new FileInputStream(PROPERTIES_FILE_NAME))
			{
				Properties properties = new Properties();
				properties.load(fis);
			
				// 로그 폴더 지정
				this.logDir = properties.getProperty(LOG_FOLDER_NAME);
				
				// 로그 레벨 지정
				String tempLevel = properties.getProperty(LOG_LEVEL);
				this.setLoggerLevel(tempLevel);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
		
	/**
	 * String 타입인 logLevel을 int로 형변환 후, loggerLevel에 설정한다.
	 * @param logLevel 로그 레벨
	 */
	private void setLoggerLevel(String logLevel)
	{
		if(logLevel == null || logLevel.length() == 0)
			logLevel = KLogger.DEFAULT_LEVEL;
		
		logLevel = logLevel.toUpperCase();
		boolean result = false;
		
		for(int key : levelMap.keySet())
		{
			if(logLevel.equals(levelMap.get(key)))
			{
				this.loggerLevel = key;
				result = true;
				break;
			}
		}
		
		// 정상적인 값이 오지 않았을 경우, DEFAULT_LEVEL인 INFO로 설정한다.
		if(!result)
		{
			this.loggerLevel = KLogger.INFO;
		}
	}
}
