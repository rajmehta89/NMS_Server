package org.example.Logger;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static Logger instance;

    private Logger() {

    }

    public static synchronized Logger getInstance() {

        if (instance == null) {

            instance = new Logger();
        }

        return instance;
    }


    private String getTimestamp() {

        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }


    public void logInfo(String message) {

        System.out.println("[INFO]  " + getTimestamp() + " - " + message);
    }


    public void logWarn(String message) {

        System.out.println("[WARN]  " + getTimestamp() + " - " + message);
    }

    public void logError(Exception e) {

        System.out.println("[ERROR] " + getTimestamp() + " - " + getStackTrace(e));
    }

    private String getStackTrace(Exception e) {

        StringWriter sw = new StringWriter();

        try (PrintWriter pw = new PrintWriter(sw)) {

            e.printStackTrace(pw);
        }

        return sw.toString();

    }

}
