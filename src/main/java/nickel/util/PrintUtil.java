package nickel.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Murray on 21/07/2017
 */
public class PrintUtil {

    private Logger log = LogManager.getLogger(PrintUtil.class);

    public void printAndLog(String s) {
        print(s);
        log(s);
    }
    public void printAndLog(Throwable t) {
        printAndLog(new Exception(t));
    }
    public void printAndLog(Exception e) {
        e.printStackTrace(); // FIXME test line; remove
        print("ERROR: " + e.toString());
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        log(Level.ERROR, stackTrace.toString());
    }

    public void print(String line) {
        System.out.println(line);
    }
    public void print(Exception e) {
        e.printStackTrace();
    }

    public void log(String line) {
        log(Level.INFO, line);
    }
    public void log(Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        log(Level.ERROR, stackTrace.toString());
    }
    public void log(Level level, String line) {
        log.log(level, line);
    }
}
