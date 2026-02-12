package view;

import org.slf4j.LoggerFactory;

public class Logger {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Logger.class);

	public static void log(String message) {
		log.info(message);
	}

}
