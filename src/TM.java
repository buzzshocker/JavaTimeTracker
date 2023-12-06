import java.io.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TM {
    static List<TaskDetails> tasks;
    static Log taskLog;
    public static void main(String[] args) {
        String function = args[0];
        tasks = new ArrayList<>();
        final String filename = "taskLog.csv";
        taskLog = new Log(filename);
        tasks = taskLog.logRead();
        //System.out.println(args[0]);
        switch (function) {
            case "start":
                start(args[1]);
                break;
            case "stop":
                stop(args[1]);
                break;
            case "summary":
                break;
            case "delete":
                delete(args[1]);
                break;
            case "size":
                size(args[1], args[2]);
                break;
            case "rename":
                rename(args[1], args[2]);
                break;
            case "describe":
                describe(args[1], args[2], args[3]);
                break;
        }
    }

    public static void start(String name) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        List<TaskDetails> extractedTasks = tasks.stream()
                .filter(t -> t.getName().equals(name))
                .collect(Collectors.toList());
        String size;
        String description;
        long timeSpent;
        if (!extractedTasks.isEmpty()) {
            /// Possible error handling if there is already a task with the same
            // name that hasn't been stopped yet.
            TaskDetails lastTask = extractedTasks
                    .get(extractedTasks.size() - 1);
            size = lastTask.getSize();
            description = lastTask.getDescription();
            timeSpent = lastTask.getTimeSpentTillNow();
        } else {
            size = "";
            description = "";
            timeSpent = 0;
        }
        TaskDetails taskDetails = new TaskDetails(name, now, "start",
                timeSpent, size, description);
        tasks.add(taskDetails);
        taskLog.logWrite(tasks);
    }

    public static void stop(String name) {
        ///TODO: Make this a function...
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        List<TaskDetails> extractedTasks = tasks.stream()
                .filter(t -> t.getName().equals(name))
                .collect(Collectors.toList());

        TaskDetails taskDetails = extractedTasks.getLast();
        if (taskDetails.getStage().equals("start")) {
            timeUtils utils = new timeUtils();
            taskDetails.setTimeSpentTillNow(utils.
                    getTimeSpent(taskDetails.getTime(), now));
        } else {
            ///error handling
        }

        String description = taskDetails.getDescription();
        String size = taskDetails.getSize();
        long timeSpent = taskDetails.getTimeSpentTillNow();

        TaskDetails newTask = new TaskDetails(name, now, "stop",
                timeSpent, size, description);
        ///Upto this point
        tasks.add(newTask);
        taskLog.logWrite(tasks);
    }

    public static void delete(String name) {
        List<TaskDetails> newTaskDetails = tasks.stream()
                .filter(t -> !t.getName().equals(name))
                .collect(Collectors.toList());
        tasks.clear();
        tasks.addAll(newTaskDetails);
        taskLog.logWrite(tasks);
    }

    public static void rename(String name, String newName) {
        List<TaskDetails> newTaskDetails = tasks.stream()
                .map(t -> {
                    if (t.getName().equals(name)) {
                        t.setName(newName);
                    }
                    return t;
                })
                .collect(Collectors.toList());
        tasks.clear();
        tasks.addAll(newTaskDetails);
        taskLog.logWrite(tasks);
    }

    public static void describe(String name, String description, String size) {
        List<TaskDetails> newTaskDetails = tasks.stream()
                .map(t -> {
                    if (t.getName().equals(name)) {
                        t.setDescription(description);
                        t.setSize(size);
                    }
                    return t;
                })
                .collect(Collectors.toList());
        tasks.clear();
        tasks.addAll(newTaskDetails);
        taskLog.logWrite(tasks);
    }

        public static void size(String name, String size) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        List<TaskDetails> newTaskDetails = tasks.stream()
                .map(t -> {
                    if (t.getName().equals(name)) {
                        t.setSize(size);
                    }
                    return t;
                }).collect(Collectors.toList());
        tasks.clear();
        tasks.addAll(newTaskDetails);
        ///Upto this point
    }

}

class timeUtils {
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

class Summary{
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

    if(taskSpecific.size() >= 2){
      List<Long> time = taskSpecific.values().stream().map(
      t -> t.get(t.size() - 1).getTimeSpentTillNow()
      ).
      collect(Collectors.toList());

      System.out.println("The max time spent on tasks of size " + size + " is "
       + Collections.max(time));
      System.out.println("The min time spent on tasks of size " + size + " is "
      + Collections.min(time));
      System.out.println("The average time spent on tasks of size "
      + size + " is " + time.stream().mapToLong(t -> t).average().getAsDouble());
    }

  }

  private static void printTask(TaskDetails task){
    System.out.println("Task Name: " + task.getName());
    if(task.getSize() != ""){
      System.out.println("Task Size: " + task.getSize());
    }
    System.out.println("Task Description: " + task.getDescription());
    System.out.println("Task Time: " + timeUtils.computeTime(task.getTimeSpentTillNow()));
  }

  public static void allSummary(List<TaskDetails> tasks){
  }

  public static void oneTask(List<TaskDetails> tasks, String task){
    List<TaskDetails> specificTask = tasks.stream()
      .filter(t ->
        t.getName().equals(task) &&
        t.getStage().equals("stop"))
      .collect(Collectors.toList());
    //make a function for getting the array size
    printTask(specificTask.get(specificTask.size() - 1));

  }

  public static void oneSize(List<TaskDetails> tasks, String size){
    Map<String, List<TaskDetails>> sizeToTaskDetails = tasks.stream()
      .filter(t ->
        t.getSize().equals(size) &&
        t.getStage().equals("stop"))
        .collect(Collectors.groupingBy(TaskDetails::getSize));

    sizeToTaskDetails.forEach(
      (k,v) ->
        sizeStatistics(size, tasks)
    );
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
    //change it to stringbuilder
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
        this.description = this.description + description;
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

