import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
Main class - Invokes the function that the user passes on the command line
to perform time tracking of the entered tasks and get information about said
tasks */
public class TM extends errorHandler {
  static List<TaskDetails> tasks;
  static Log taskLog;

  public static void main(String[] args) {
    tasks = new ArrayList<>();
    final String filename = "taskLog.csv";
    taskLog = Log.getInstance(filename);
    tasks = taskLog.logRead();
    Parser parser = Parser.getInstance(args);
    errorHandler.validateNumberOfArgs(parser.getSizeOfArgs(),
            (args1) -> args1 >= 1, "main", "Invalid number of arguments");
    run(parser);
  }

  private static void run(Parser parse) {
    switch (parse.getFunction()) {
      case "start":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 2, "start",
                "Invalid number of arguments");
        start(parse.getName());
        break;

      case "stop":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 2, "stop",
                "Invalid number of arguments");
        stop(parse.getName());
        break;

      case "summary":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 2 || args == 1, "summary",
                "Invalid number of arguments");
        summary(tasks, parse);
        break;

      case "delete":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 2, "delete",
                "Invalid number of arguments");
        delete(parse.getName());
        break;

      case "size":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 3, "size",
                "Invalid number of arguments");
        size(parse.getName(), parse.getSize());
        break;

      case "rename":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 3, "rename",
                "Invalid number of arguments");
        rename(parse.getName(), parse.getNewName());
        break;

      case "describe":
        errorHandler.validateNumberOfArgs(parse.getSizeOfArgs(),
                (args) -> args == 3 || args == 4, "describe",
                "Invalid number of arguments");
        describe(tasks, parse);
        break;

      default:
        errorHandler.printError(parse.getFunction(),
                "Invalid function");
    }
  }

  static void setTaskLog(List<TaskDetails> updatedTaskDetails) {
    tasks.clear();
    tasks.addAll(updatedTaskDetails);
    taskLog.logWrite(tasks);
  }

  static void addToTaskLog(TaskDetails task) {
    tasks.add(task);
    taskLog.logWrite(tasks);
  }

  private static void start(String name) {
    new startTask().execute(name, tasks, new String[0]);
  }

  private static void stop(String name) {
    new stopTask().execute(name, tasks, new String[0]);
  }

  private static void delete(String name) {
    new deleteTask().execute(name, tasks, new String[0]);
  }

  private static void rename(String name, String newName) {
    new renameTask().execute(name, tasks, new String[]{newName});
  }

  private static void describe(List<TaskDetails> tasks, Parser parse) {
    new describeTask().execute(tasks, parse);
  }

  private static void size(String name, String size) {
    new sizeTask().execute(name, tasks, new String[]{size});
  }

  private static void summary(List<TaskDetails> tasks, Parser parse) {
    new summaryTask().execute(tasks, parse);
  }
}
//------------------------------------------------------------------------------

// Class that handles all the task data
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
    return name + ", " + time.toString() + ", " + stage + ", "
            + timeSpentTillNow + ", " + size + ", " + description + " \n";
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

//------------------------------------------------------------------------------

// Command pattern interface - to handle methods that provide the name of
// a task and other parameters depending on function
interface Command {
  void execute(String name, List<TaskDetails> tasks, String[] parameters);
}

class startCommand {
  public void run(String name, List<TaskDetails> tasks) {
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    List<TaskDetails> extractTasks = DsUtils
            .getNameMatchedTasks(tasks, name, false, "");
    if (!extractTasks.isEmpty()) {
      startTask(name, extractTasks, now);
    } else {
      TM.addToTaskLog(new TaskDetails(name, now, "start", 0L,
              "", ""));
    }
  }

  private void startTask(String name, List<TaskDetails> tasks,
                         LocalDateTime now) {
    TaskDetails lastTask = tasks.get(tasks.size() - 1);
    if (lastTask.getStage().equals("start")) {
      errorHandler.startErrorHandler(name);
    }
    TM.addToTaskLog(new TaskDetails(name, now, "start",
            lastTask.getTimeSpentTillNow(), lastTask.getSize(),
            lastTask.getDescription()));
  }
}

final class startTask implements Command {
  private final startCommand task;

  public startTask() {
    task = new startCommand();
  }

  public void execute(String name, List<TaskDetails> tasks,
                      String[] params) {
    task.run(name, tasks);
  }
}

class stopCommand {

  public void run(String name, List<TaskDetails> tasks) {
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    List<TaskDetails> extractedTasks = DsUtils
            .getNameMatchedTasks(tasks, name, false, "");
    if (extractedTasks.isEmpty()) {
      errorHandler.stopErrorHandler(name);
    }
    TaskDetails taskDetails = extractedTasks.get(extractedTasks.size() - 1);
    if (taskDetails.getStage().equals("start")) {
      taskDetails.setTimeSpentTillNow(TimeUtils
              .getTimeSpent(taskDetails.getTime(), now));
    } else {
      errorHandler.stopErrorHandler(name);
    }
    TM.addToTaskLog(new TaskDetails(name, now, "stop",
            taskDetails.getTimeSpentTillNow(), taskDetails.getSize(),
            taskDetails.getDescription()));
  }
}

final class stopTask implements Command {
  private final stopCommand task;

  public stopTask() {
    task = new stopCommand();
  }

  public void execute(String name, List<TaskDetails> tasks,
                      String[] params) {
    task.run(name, tasks);
  }
}

class sizeCommand {
  public void run(String name, List<TaskDetails> tasks, String size) {
    errorHandler.validateTaskExists(name, tasks);
    errorHandler.sizeErrorHandler(name, size);
    TM.setTaskLog(DsUtils.resizeTasks(tasks, name, size.toUpperCase()));
  }
}

final class sizeTask implements Command {
  private sizeCommand task;

  public sizeTask() {
    task = new sizeCommand();
  }

  public void execute(String name, List<TaskDetails> tasks, String[] params) {
    task.run(name, tasks, params[0]);
  }
}

class renameCommand {
  public void run(String name, List<TaskDetails> tasks, String newName) {
    errorHandler.validateTaskExists(name, tasks);
    TM.setTaskLog(DsUtils.renameTasks(tasks, name, newName));
  }
}

final class renameTask implements Command {
  private renameCommand task;

  public renameTask() {
    task = new renameCommand();
  }

  public void execute(String name, List<TaskDetails> tasks, String[] params) {
    task.run(name, tasks, params[0]);
  }
}

class deleteCommand {
  public void run(String name, List<TaskDetails> tasks) {
    errorHandler.validateTaskExists(name, tasks);
    TM.setTaskLog(DsUtils.getNameMatchedTasks(tasks, name, true, ""));
  }
}

final class deleteTask implements Command {
  private deleteCommand task;

  public deleteTask() {
    task = new deleteCommand();
  }

  public void execute(String name, List<TaskDetails> tasks, String[] params) {
    task.run(name, tasks);
  }
}
//------------------------------------------------------------------------------

// Parse command interface - to handle methods that need extra parsing
// due to the nature of arguments that can be passed
interface parseCommand {
  void execute(List<TaskDetails> tasks, Parser parse);

  static void printStatement(String type, String size, Long time) {
    System.out.println("The " + type + " time spent on tasks of size " + size
            + " is " + TimeUtils.computeTime(time));
  }
}

class describeCommand {
  public void run(List<TaskDetails> tasks, Parser parse) {
    if (parse.getSizeOfArgs() == 4) {
      describe(parse.getName(), tasks, parse.getDescription(),
              parse.getDescribeSize());
    } else {
      describe(parse.getName(), tasks, parse.getDescription(), "");
    }
  }

  public void describe(String name, List<TaskDetails> tasks, String description,
                       String size) {
    errorHandler.validateTaskExists(name, tasks);
    size = DsUtils.checkForSize(name, tasks, size);
    TM.setTaskLog(DsUtils
            .describeTasks(tasks, name, description, size.toUpperCase()));
  }
}

final class describeTask implements parseCommand {
  private describeCommand task;

  public describeTask() {
    task = new describeCommand();
  }

  public void execute(List<TaskDetails> tasks, Parser parse) {
    task.run(tasks, parse);
  }
}

class summaryCommand {
  public void sizeStatistics(String size, List<TaskDetails> tasks) {
    Map<String, List<TaskDetails>> taskSpecific = tasks.stream()
            .collect(Collectors.groupingBy(TaskDetails::getName));
    System.out.println("Tasks of size " + size + ": ");
    taskSpecific.forEach((k, v) -> {
      v.forEach(this::printTask);
    });

    if (taskSpecific.size() >= 2) {
      System.out.println("Statistics: ");
      List<Long> time = taskSpecific.values().stream()
              .map(t -> t.get(t.size() - 1).getTimeSpentTillNow())
              .collect(Collectors.toList());
      parseCommand.printStatement("max", size, Collections.max(time));
      parseCommand.printStatement("min", size, Collections.min(time));
      long averageTime = (long) time.stream().mapToLong(t -> t)
              .average().getAsDouble();
      parseCommand.printStatement("average", size, averageTime);
    }
  }

  public void printTask(TaskDetails task) {
    System.out.println("\nTask Name            :      " + task.getName());
    System.out.println("Task Size            :      " + task.getSize());
    System.out.println("Task Description     :      " + task.getDescription());
    System.out.println("Task Time            :      " + TimeUtils
            .computeTime(task.getTimeSpentTillNow()) + "\n");
  }

  public void allSummary(List<TaskDetails> tasks) {
    tasks.stream().collect(Collectors.groupingBy(TaskDetails::getName))
            .forEach((name, task) -> {
              printTask(task.get(task.size() - 1));
            });
  }

  public void oneTask(List<TaskDetails> tasks, String task) {
    List<TaskDetails> specificTask =  tasks.stream().filter(t -> t.getName()
            .equals(task)).collect(Collectors.toList());
    if (specificTask.isEmpty()) {
      errorHandler.printError(task, "Task not found.");
    }
    printTask(specificTask.get(specificTask.size() - 1));
  }

  public void oneSize(List<TaskDetails> tasks, String size) {
    List<TaskDetails> tasksOfSpecifiedSize = DsUtils
            .getNameMatchedTasks(tasks, size.toUpperCase(), true, "stop");
    sizeStatistics(size.toUpperCase(), tasksOfSpecifiedSize);
  }
}

class summaryTask implements parseCommand {
  private summaryCommand task;

  public summaryTask() {
    task = new summaryCommand();
  }

  @Override
  public void execute(List<TaskDetails> tasks, Parser parse) {
    if (parse.getSizeOfArgs() == 1) {
      task.allSummary(tasks);
    } else if (parse.getSizeOfArgs() == 2
            && !errorHandler.doesSizeExist(errorHandler.Size.values(),
                    parse.getSummarySize())) {
      task.oneTask(tasks, parse.getName());
    } else {
      task.oneSize(tasks, parse.getSummarySize());
    }
  }
}
//------------------------------------------------------------------------------

// Classes for file and command line argument handling
class Parser {
  private static Parser instance;

  private String[] args;

  Parser(String[] args) {
    this.args = args;
  }

  public static Parser getInstance(String[] args) {
    if (instance == null) {
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

  public String getDescribeSize() {
    return args[3];
  }

  public String getNewName() {
    return args[2];
  }

  public Integer getSizeOfArgs() {
    return args.length;
  }
}

class Log {
  private static Log instance;
  private static File file;

  public static Log getInstance(String filename) {
    if (instance == null) {
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
    LocalDateTime time = TimeUtils.getStringTime(fields[1].trim());
    String stage = fields[2].trim();
    long timeSpent = Long.parseLong(fields[3].trim());
    String size = fields[4].trim();
    String description = fields[5].trim();

    return new TaskDetails(name, time, stage, timeSpent, size, description);
  };
}
//------------------------------------------------------------------------------

// Utility classes for different classes throughout the program
class errorHandler {
  public errorHandler() {}

  enum Size { S, M, L, XL }

  public static boolean doesSizeExist(Size[] sizes, String size) {
    for (Size value : sizes) {
      if (value.name().equals(size.toUpperCase())) {
        return true;
      }
    }
    return false;
  }

  protected static void printError(String name, String message) {
    System.err.println(name + ": " + message);
    System.exit(1);
  }

  public static void validateNumberOfArgs(Integer args,
                                          Predicate<Integer> predicate,
                                          String function, String message) {
    if (!predicate.test(args)) {
      printError(function, message);
    }
  }

  public static void stopErrorHandler(String name) {
    printError(name, "Task has not been started yet.");
  }

  public static void startErrorHandler(String name) {
    printError(name, "Task has already been started.");
  }

  public static void validateTaskExists(String name, List<TaskDetails> tasks) {
    boolean isPresent = tasks.stream()
            .anyMatch(task -> task.getName().equals(name));
    if (!isPresent) {
      printError(name, "Task does not exist.");
    }
  }

  public static void sizeErrorHandler(String name, String size) {
    if (!doesSizeExist(errorHandler.Size.values(), size)) {
      printError(name, "Invalid size - " + size);
    }
  }
}

final class DsUtils {
  private DsUtils() {}

  public static List<TaskDetails> getNameMatchedTasks(List<TaskDetails> tasks,
                                                      String predicate1,
                                                      boolean isSpecial,
                                                      String predicate2) {
    if (predicate2.isEmpty()) {
      if (isSpecial) {
        return tasks.stream()
                .filter(t -> !t.getName().equals(predicate1))
                .collect(Collectors.toList());
      }
    }
    if (predicate2.equals("stop")) {
      if (!isSpecial) {
        return tasks.stream().filter(t -> t.getName().equals(predicate1)
                        && t.getStage().equals("stop"))
                .collect(Collectors.toList());
      }
      return tasks.stream().filter(t -> t.getSize().equals(predicate1)
              && t.getStage().equals("stop")).collect(Collectors.toList());
    }
    return tasks.stream().filter(t -> t.getName().equals(predicate1))
                .collect(Collectors.toList());
  }

  public static List<TaskDetails> renameTasks(List<TaskDetails> tasks,
                                              String oldName, String newName) {
    return tasks.stream()
            .map(t -> {
              if (t.getName().equals(oldName)) {
                t.setName(newName);
              }
              return t;
            }).collect(Collectors.toList());
  }

  public static List<TaskDetails> describeTasks(List<TaskDetails> tasks,
                                                String name, String description,
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

  public static List<TaskDetails> resizeTasks(List<TaskDetails> tasks,
                                              String name, String size) {
    return tasks.stream()
            .map(t -> {
              if (t.getName().equals(name)) {
                t.setSize(size);
              }
              return t;
            }).collect(Collectors.toList());
  }
}

final class TimeUtils {

  private TimeUtils() {}

  public static LocalDateTime getStringTime(String time) {
    return LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  public static long getTimeSpent(LocalDateTime start, LocalDateTime end) {
    return Math.abs(Duration.between(start, end).toSeconds());
  }

  public static String computeTime(long timeSpent) {
    long hours = timeSpent / 3600;
    timeSpent -= (hours * 3600);

    long minutes = timeSpent / 60;
    timeSpent -= (minutes * 60);

    return (hours + ":" + minutes + ":" + timeSpent);
  }
}