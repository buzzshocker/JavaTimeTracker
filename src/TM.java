import java.io.File;

public class TM {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }


}

class Log{
  private static Log log;

  public Log() {

  }

  public void logWrite() {

  }

  public void logRead() {

  }

  public void logDelete() {

  }

  public void logUpdate() {

  }

//  public static Log getInstance(){
//      if (log == null) {
//          log = new Log();
//      }
//      return log;
//  }

}

record taskDetails (String name, String time, String stage, String size,
                           String description) {
}
