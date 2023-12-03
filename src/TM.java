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

class TaskDetails {
    private String name;
    private String time;
    private String stage;
    private String size;
    private String description;

    public TaskDetails(String name, String time, String stage) {
        this.name = name;
        this.time = time;
        this.stage = stage;
    }

    // Provide setters for other fields
    public void setSize(String size) {
        this.size = size;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Provide getters as needed

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public String getStage() {
        return stage;
    }

    public String getSize() {
        return size;
    }

    public String getDescription() {
        return description;
    }
}

