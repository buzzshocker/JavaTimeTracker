import java.io.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TM extends errorHandler{
    static List<TaskDetails> tasks;
    final static String filename = "taskLog.csv";
    static Log taskLog;
    public static void main(String[] args) {
      Parser parser = Parser.getInstance(args);
      tasks = new ArrayList<>();
      taskLog = Log.getInstance(filename);
      tasks = taskLog.logRead();
      run(parser);
    }

    private static void run(Parser parse){
      switch (parse.getFunction()) {
        case "start":
          if(parse.getSizeOfArgs() != 2) {
            errorHandler.printError("start", "Invalid number of arguments");
          }
          start(parse.getName());
          break;
        case "stop":
          stop(parse.getName());
          break;
        case "summary":
          if (parse.getSizeOfArgs() == 1) {
            Summary.allSummary(tasks);
          } else if (parse.getSizeOfArgs() == 2 && 
              !errorHandler.doesSizeExist(errorHandler.Size.values(), 
              parse.getSummarySize())) {
            Summary.oneTask(tasks, parse.getName());
          } else {
            Summary.oneSize(tasks, parse.getSummarySize());
          }
          break;
        case "delete":
          delete(parse.getName());
          break;
        case "size":
          size(parse.getName(), parse.getSize());
          break;
        case "rename":
          rename(parse.getName(), parse.getNewName());
          break;
        case "describe":
          if (parse.getSizeOfArgs() == 4) {
            describe(parse.getName(), parse.getDescription(),
              parse.getDescribeSize());
          } else {
              describe(parse.getName(), parse.getDescription(), "");
          }
          break;
        default:
          System.out.println("Invalid command");
          System.exit(1);
      }
    }

    private static void setTaskLog(List<TaskDetails> updatedTaskDetails) {
        tasks.clear();
        tasks.addAll(updatedTaskDetails);
        taskLog.logWrite(tasks);
    }

    private static void addToTaskLog(TaskDetails task) {
        tasks.add(task);
        taskLog.logWrite(tasks);
    }

    private static void start(String name) {
      LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
      List<TaskDetails> extractTasks = DSutils
            .getNameMatchedTasks(tasks, name, false, "");
      if (!extractTasks.isEmpty()) {
          TaskDetails lastTask = extractTasks.get(extractTasks.size() - 1);
          if (lastTask.getStage().equals("start")) {
            startErrorHandler(name);
          }
          addToTaskLog(new TaskDetails(name, now, "start",
                  lastTask.getTimeSpentTillNow(), lastTask.getSize(),
                  lastTask.getDescription()));
      } else {
          addToTaskLog(new TaskDetails(name, now, "start", 0L,
                  "", ""));
      }
    }

    private static void stop(String name) {
      LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
      List<TaskDetails> extractedTasks = DSutils
        .getNameMatchedTasks(tasks, name, false, "");
      if (extractedTasks.isEmpty()) {
          stopErrorHandler(name);
      }
      TaskDetails taskDetails = extractedTasks.get(extractedTasks.size() - 1);
      if (taskDetails.getStage().equals("start")) {
          taskDetails.setTimeSpentTillNow(timeUtils.
                  getTimeSpent(taskDetails.getTime(), now));
      } else {
          stopErrorHandler(name);
      }
      addToTaskLog(new TaskDetails(name, now, "stop",
              taskDetails.getTimeSpentTillNow(), taskDetails.getSize(),
              taskDetails.getDescription()));
    }

    private static void delete(String name) {
      validateTaskExists(name, tasks);
      setTaskLog(DSutils
        .getNameMatchedTasks(tasks, name, true, ""));
    }

    private static void rename(String name, String newName) {
      validateTaskExists(name, tasks);
      setTaskLog(DSutils
        .renameTasks(tasks, name, newName));
    }

    private static void describe(String name, String description, String size) {
      validateTaskExists(name, tasks);
      size = DSutils.checkForSize(name, tasks, size);
      setTaskLog(DSutils
        .describeTasks(tasks, name, description, size.toUpperCase()));
    }

    private static void size(String name, String size) {
      validateTaskExists(name, tasks);
      sizeErrorHandler(name, size);
      setTaskLog(DSutils.resizeTasks(tasks, name, size.toUpperCase()));
    }
}

class Parser{
  private static Parser instance;
  
  private String[] args;
  
  private Parser(String[] args) {
    this.args = args;
  }
  
  public static Parser getInstance(String[] args) {
    if(instance == null) {
      instance = new Parser(args);
    }
    return instance;
  }

  public String getFunction() {
    return args[0];
  }

  public String getName() {
    return args[1];
  }

  public String getSummarySize() {
    return args[1];
  }

  public String getDescription() {
    return args[2];
  }

  public String getSize() {
    return args[2];
  }

  public String getDescribeSize(){
    return args[3];
  }

  public String getNewName() {
    return args[2];
  }

  public Integer getSizeOfArgs(){
    return args.length;
  }
}

class errorHandler {
  public errorHandler() {}

  enum Size{
      S, M, L, XL
  }

  public static boolean doesSizeExist(Size[] sizes , String size){
    for (Size value : sizes) {
      if (value.name().equals(size.toUpperCase())) {
        return true;
      }
    }
    return false;
  }

  protected static void printError(String name, String message) {
    System.out.println(name + " : " + message);
    System.exit(1);
  }

  public static void stopErrorHandler(String name) {
    printError(name, "Task has not been started yet.");
  }

  public static void startErrorHandler(String name) {
    printError(name, "Task has already been started.");
  }

  public static void validateTaskExists(String name,List<TaskDetails> tasks){
    boolean isPresent = tasks.stream()
            .anyMatch(task -> task.getName().equals(name));
    if (!isPresent) {
      printError(name,"Task does not exist.");
    }
  }

  public static void sizeErrorHandler(String name, String size) {
    if (!doesSizeExist(errorHandler.Size.values(), size)) {
      printError(name, "Invalid size - " + size);
    }
  }
}

final class DSutils {
  private DSutils() {}

  public static List<TaskDetails>
  getNameMatchedTasks(List<TaskDetails> tasks, String predicate1,
                    boolean isSpecial, String predicate2) {
    if (predicate2.isEmpty()) {
      if (isSpecial) {
        return tasks.stream().filter(t -> !t.getName().equals(predicate1))
                .collect(Collectors.toList());
      }
    }
    if (predicate2.equals("stop")) {
        if (!isSpecial) {
            return tasks.stream().filter(t -> t.getName().equals(predicate1)
                    && t.getStage().equals("stop"))
                    .collect(Collectors.toList());
        }
        return tasks.stream().filter(t -> t.getSize().equals(predicate1) &&
                t.getStage().equals("stop")).collect(Collectors.toList());
    }

    return tasks.stream()
            .filter(t -> t.getName().equals(predicate1))
            .collect(Collectors.toList());
  }

  public static List<TaskDetails>
  renameTasks(List<TaskDetails> tasks, String oldName, String newName) {
    return tasks.stream()
            .map(t -> {
                if (t.getName().equals(oldName)) {
                    t.setName(newName);
            }
              return t;
            })
            .collect(Collectors.toList());
  }

  public static List<TaskDetails>
  describeTasks(List<TaskDetails> tasks, String name, String description,
                String size) {
      return tasks.stream()
              .map(t -> {
                  if (t.getName().equals(name)) {
                      t.setDescription(description);
                      t.setSize(size);
                  }
                  return t;
              }).collect(Collectors.toList());
  }

  public static String checkForSize(String name, List<TaskDetails> tasks,
                                    String size) {
      if (!size.isEmpty()) {
          return size;
      }
      List<TaskDetails> matchedTasks =
              getNameMatchedTasks(tasks, name, false, "");

      TaskDetails task = matchedTasks.get(matchedTasks.size() - 1);
      if (!task.getSize().isEmpty()) {
          return task.getSize();
      }
      return "";
  }

  public static List<TaskDetails>
  resizeTasks(List<TaskDetails> tasks, String name, String size) {
      return tasks.stream()
              .map(t -> {
                  if (t.getName().equals(name)) {
                      t.setSize(size);
                  }
                  return t;
              }).collect(Collectors.toList());
  }
}

final class timeUtils {
    
  private timeUtils() {}

  public static LocalDateTime getStringTime(String time) {
    return LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  public static long getTimeSpent(LocalDateTime start, LocalDateTime end) {
      return Math.abs(Duration.between(start, end).toSeconds());
  }

  public static String computeTime(long timeSpent) {
      long hours = timeSpent/3600;
      timeSpent -= (hours*3600);

      long minutes = timeSpent/60;
      timeSpent -= (minutes*60);

      return (hours + ":" + minutes + ":" + timeSpent);
  }
}

class Summary {

  private static void printStatement(String type, String size, Long time){
    System.out.println(
      "The " + type + " time spent on tasks of size " + size + " is " + 
      timeUtils.computeTime(time)
    );
  }

  private static void sizeStatistics(String size, List<TaskDetails> tasks){
    Map<String, List<TaskDetails>> taskSpecific = tasks.stream()
      .collect(Collectors.groupingBy(TaskDetails::getName)
    );
    
    if (taskSpecific.size() >= 2) {
      List<Long> time = taskSpecific.values().stream()
        .map(t -> t.get(t.size() - 1).getTimeSpentTillNow())
        .collect(Collectors.toList());
      
      printStatement("max", size, Collections.max(time));
      printStatement("min", size, Collections.min(time));
      long fine_time = (long) time.stream().mapToLong(t -> t)
              .average().getAsDouble();
      printStatement("average", size, fine_time);
    }
  }

  private static void printTask(TaskDetails task) {
    System.out.println("Task Name: " + task.getName());
    System.out.println("Task Size: " + task.getSize());
    System.out.println("Task Description: " + task.getDescription());
    System.out.println(
      "Task Time: " + timeUtils.computeTime(task.getTimeSpentTillNow())
    );
  }

  public static void allSummary(List<TaskDetails> tasks) {
    tasks.stream().collect(Collectors.groupingBy(TaskDetails::getName))
      .forEach((name, task) -> {
        printTask(task.get(task.size() - 1));
      }
    );
  }

  public static void oneTask(List<TaskDetails> tasks, String task){
    List<TaskDetails> specificTask = DSutils
      .getNameMatchedTasks(tasks, task,false, "stop");
    printTask(specificTask.get(specificTask.size() - 1));
  }

  public static void oneSize(List<TaskDetails> tasks, String size) {
    List<TaskDetails> tasksOfSpecifiedSize = DSutils
      .getNameMatchedTasks(tasks, size, true, "stop");
    sizeStatistics(size, tasksOfSpecifiedSize);
  }

}

class Log {
  private static Log instance;
  private static File file;

  public static Log getInstance(String filename) {
    if(instance == null) {
      instance = new Log(filename);
    }
    return instance;
  }

  private void createCSV(File file) throws IOException {
    try {
      if (!file.exists()) {
        file.createNewFile();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Log(String filename) {
    Log.file = new File(filename);
    try {
      createCSV(file);
    } catch (IOException e) {
      e.getMessage();
    }
  }

  public void logWrite(List<TaskDetails> tasks) {
    try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) {
      for (TaskDetails details : tasks) {
        br.write(details.toCSVString());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public List<TaskDetails> logRead() {
    List<TaskDetails> tasks = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      tasks = br.lines().map(mapToTaskDetails).collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return tasks;
  }

  private final Function<String, TaskDetails> mapToTaskDetails = (line) -> {
    String[] fields = line.split(",");
    String name = fields[0].trim();
    LocalDateTime time = timeUtils.getStringTime(fields[1].trim());
    String stage = fields[2].trim();
    long timeSpent = Long.parseLong(fields[3].trim());
    String size = fields[4].trim();
    String description = fields[5].trim();

    return new TaskDetails(name, time, stage, timeSpent, size, description);
  };
}

class TaskDetails {
  private String name;
  private LocalDateTime time;
  private String stage;
  private String size;
  private String description;
  private long timeSpentTillNow = 0;

  public TaskDetails(String name, LocalDateTime time, String stage,
                    Long timeSpent, String size, String description) {
    this.name = name;
    this.time = time;
    this.stage = stage;
    this.timeSpentTillNow += timeSpent;
    this.description = description != null ? description : "";
    this.size = size != null ? size : "";
  }

  public String toCSVString() {
    return name + ", " + time.toString() + ", " + stage + ", "  +
            timeSpentTillNow + ", " + size + ", " + description + " \n";
  }

  public void setSize(String size) {
    this.size = size;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    if (!this.description.isEmpty()) {
      this.description = this.description + " " + description;;
    } else {
      this.description = description;
    }
  }

  public String getName() {
    return this.name;
  }

  public LocalDateTime getTime() {
    return this.time;
  }

  public String getStage() {
    return this.stage;
  }

  public String getSize() {
    return this.size;
  }

  public String getDescription() {
    return this.description;
  }

  public long getTimeSpentTillNow() {
    return this.timeSpentTillNow;
  }

  public void setTimeSpentTillNow(long timeSpent) {
    this.timeSpentTillNow += timeSpent;
  }
}