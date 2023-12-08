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
    static Log taskLog;
    final static String filename = "taskLog.csv";
    public static void main(String[] args) {
        Parser parser = new Parser(args);
        tasks = new ArrayList<>();
        taskLog = new Log(filename);
        tasks = taskLog.logRead();
        run(parser);
    }

    public static void run(Parser parse){
        switch (parse.getFunction()) {
            case "":
                System.out.println("Parse error: Please enter a command");
                System.exit(1);
            case "start":
                start(parse.getName());
                break;
            case "stop":
                stop(parse.getName());
                break;
            case "summary":
                if (parse.getSizeOfArgs() == 1) {
                    Summary.allSummary(tasks);
                } else if (parse.getSizeOfArgs() == 2 &&
                        !errorHandler.checkIfSizeExists(errorHandler
                                        .Size.values(), parse.getSummarySize())
                ) {
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

    private static void start(String name) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        List<TaskDetails> extractTasks =
                DSutils.getNameMatchedTasks(tasks, name, false, "");
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
        List<TaskDetails> extractedTasks =
                DSutils.getNameMatchedTasks(tasks, name, false, "");
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
        validateValueInList(name, tasks);
        List<TaskDetails> newTaskDetails =
                DSutils.getNameMatchedTasks(tasks, name, true, "");
        setTaskLog(newTaskDetails);
    }

    private static void rename(String name, String newName) {
        validateValueInList(name, tasks);
        List<TaskDetails> newTaskDetails =
                DSutils.renameTasks(tasks, name, newName);
        setTaskLog(newTaskDetails);
    }

    private static void describe(String name, String description, String size) {
        validateValueInList(name, tasks);
        size = DSutils.checkForSize(name, tasks, size);
        List<TaskDetails> newTaskDetails =
                DSutils.describeTasks(tasks, name, description,
                        size.toUpperCase());
        setTaskLog(newTaskDetails);
    }

    private static void size(String name, String size) {
        validateValueInList(name, tasks);
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        sizeErrorHandler(name, size);
        List<TaskDetails> newTaskDetails =
                DSutils.resizeTasks(tasks, name, size.toUpperCase());
        setTaskLog(newTaskDetails);
    }

    public static void summary() {}

    private static void setTaskLog(List<TaskDetails> updatedTaskDetails) {
        tasks.clear();
        tasks.addAll(updatedTaskDetails);
        taskLog.logWrite(tasks);
    }

    private static void addToTaskLog(TaskDetails task) {
        tasks.add(task);
        taskLog.logWrite(tasks);
    }

}

class Parser{
    private static String[] args;

    public Parser(String[] args) {
        Parser.args = args;
    }

    public String getFunction() {
        return args[0];
    }

    public String getName() {
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
    private static final Set<String> sizes = new HashSet<>();
    public errorHandler() {}

    enum Size{
        S, M, L, XL
    }

    public static boolean checkIfSizeExists(Size[] sizes , String size){
        for (Size value : sizes) {
            if (value.name().equals(size.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private static void setSizes() {
        sizes.add("S");
        sizes.add("M");
        sizes.add("L");
        sizes.add("XL");
    }
    public static void stopErrorHandler(String name) {
        System.out.println(name + ": Task has not been started yet.");
        System.exit(1);
    }

    public static void startErrorHandler(String name) {
        System.out.println(name + ": Task has already been started.");
        System.exit(1);
    }

    public static void validateValueInList(String name,
                                           List<TaskDetails> tasks) {
        boolean isPresent = tasks.stream()
                .anyMatch(task -> task.getName().equals(name));
         if (!isPresent) {
            System.out.println(name + ": Task does not exist.");
            System.exit(1);
         }
    }

    public static void sizeErrorHandler(String name, String size) {
        setSizes();
        String upperCaseSize = size.toUpperCase();
        if (!sizes.contains(upperCaseSize)) {
            System.out.println(name + ": Invalid size - " + upperCaseSize);
            System.exit(1);
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
                }).collect(Collectors.toList());
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
        DateTimeFormatter formatter =
                DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(time, formatter);
    }

    public static long getTimeSpent(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        return Math.abs(duration.toSeconds());
    }

    public static String computeTime(long timeSpent) {
        long hours = timeSpent/3600;
        timeSpent = timeSpent - (hours*3600);

        long minutes = timeSpent/60;
        timeSpent = timeSpent - (minutes*60);

        return (hours + ":" + minutes + ":" + timeSpent);
    }
}

class Summary {
  private static Summary summary;

  public static Summary getInstance() {
    if (summary == null) {
      summary = new Summary();
    }
    return summary;
  }

  private static void sizeStatistics(String size, List<TaskDetails> tasks){
    Map<String, List<TaskDetails>> taskSpecific = tasks.stream()
      .collect(Collectors
      .groupingBy(TaskDetails::getName)
    );

    if (taskSpecific.size() >= 2) {
      List<Long> time = taskSpecific.values().stream().map(
      t -> t.get(t.size() - 1).getTimeSpentTillNow())
              .collect(Collectors.toList());

            System.out.println("The max time spent on tasks of size "
                    + size + " is "
                    + timeUtils.computeTime(Collections.max(time)));
            System.out.println("The min time spent on tasks of size "
                    + size + " is "
                    + timeUtils.computeTime(Collections.min(time)));
            long fine_time = (long) time.stream().mapToLong(t -> t)
                    .average().getAsDouble();
            System.out.println("The average time spent on tasks of size "
                    + size + " is " + timeUtils.computeTime(fine_time));
        }

  }

  private static void printTask(TaskDetails task) {
    System.out.println("Task Name: " + task.getName());
    if (task.getSize().isEmpty()) {
      System.out.println("Task Size: " + task.getSize());
    }
    System.out.println("Task Description: " + task.getDescription());
    System.out.println("Task Time: " + timeUtils
            .computeTime(task.getTimeSpentTillNow()));
  }

    public static void allSummary(List<TaskDetails> tasks) {
        Map<String, List<TaskDetails>> taskSpecific = tasks.stream()
                .collect(Collectors
                        .groupingBy(TaskDetails::getName)
                );
        taskSpecific.forEach((name, task) -> {
                    printTask(task.get(task.size() - 1));
                }
        );

    }

  public static void oneTask(List<TaskDetails> tasks, String task){
    List<TaskDetails> specificTask = DSutils.getNameMatchedTasks(tasks, task,
            false, "stop");
    //make a function for getting the array size
    printTask(specificTask.get(specificTask.size() - 1));
  }

    public static void oneSize(List<TaskDetails> tasks, String size) {
        List<TaskDetails> tasksOfSpecifiedSize =
                DSutils.getNameMatchedTasks(tasks, size, true, "stop");
        sizeStatistics(size, tasksOfSpecifiedSize);
    }

}

class Log {
  private static File file;
  private static String filename;

  private void createCSV(File file) throws IOException {
      try {
          if (!file.exists()) {
              file.createNewFile();
          }
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }

  public Log(String filename) {
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

  private Function<String, TaskDetails> mapToTaskDetails = (line) -> {
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
    @Override
    public String toString() {
        return "TaskDetails{" +
                "name='" + name + '\'' +
                ", time='" + time.toString() + '\'' +
                ", stage='" + stage + '\'' +
                ", size='" + size + '\'' +
                ", description='" + description + '\'' +
                ", timeSpentTillNow=" + timeSpentTillNow +
                '}';
    }

    public String toCSVString() {
        return name + ", " + time.toString() + ", " + stage + ", "  +
                timeSpentTillNow + ", " + size + ", " + description + " \n";
    }
    // Provide setters for other fields
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

    // Provide getters as needed

    public String getName() {
        return name;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getStage() {
        return stage;
    }

    public String getSize() {
        return size != null ? size : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public long getTimeSpentTillNow() {
        return this.timeSpentTillNow;
    }

    public void setTimeSpentTillNow(long timeSpent) {
        this.timeSpentTillNow += timeSpent;
    }

}

