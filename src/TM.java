import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

public class TM {
    static List<TaskDetails> tasks;
    static Log taskLog;
    public static void main(String[] args) {
        String function = args[0];
        List<TaskDetails> tasks = new ArrayList<TaskDetails>();
        final String filename = "taskLog.csv";
        taskLog = new Log(filename);
        tasks = taskLog.logRead();
        switch (function) {
            case "start":
                start(args[1]);
            case "stop":
                stop(args[1]);
            case "summary":
                
            case "delete":
                delete(args[1]);
            case "size":

            case "rename":
                rename(args[1], args[2]);
            case "describe":
                describe(args[1], args[2], args[3]);
        }
    }

    public static void start(String name) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Map<String, String> sizeAndDesc = tasks.stream()
                .filter(t -> t.getName().equals(name))
                .collect(Collectors.toMap(
                        TaskDetails::getSize, TaskDetails::getDescription));
        String size = sizeAndDesc.keySet().
                toArray(new String[0])[0];
        String description = sizeAndDesc.get(size);
        TaskDetails taskDetails = new TaskDetails(name, now.toString(),
                "start", size, description);
        tasks.add(taskDetails);
        taskLog.logWrite(tasks);
    }

    public static void stop(String name) {
        ///TODO: Make this a function...
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Map<String, String> sizeAndDesc = tasks.stream()
                .filter(t -> t.getName().equals(name))
                .collect(Collectors.toMap(
                        TaskDetails::getSize, TaskDetails::getDescription));
        String size = sizeAndDesc.keySet().
                toArray(new String[0])[0];
        String description = sizeAndDesc.get(size);
        TaskDetails taskDetails = new TaskDetails(name, now.toString(),
                "stop", size, description);
        ///Upto this point
        tasks.add(taskDetails);
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
                })
                .toList();
        tasks.clear();
        tasks.addAll(newTaskDetails);
        ///Upto this point
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
      String time = fields[1].trim();
      String stage = fields[2].trim();
      String size;
      String description;

      if (fields.length >= 4) {
        size = fields[3].trim();
      } else {
          size = "";
      }
      if (fields.length >= 5) {
          description = fields[4].trim();
      } else {
          description = "";
      }

      return new TaskDetails(name, time, stage, size, description);
  };
}

class TaskDetails {
    //change it to stringbuilder
    private String name;
    private String time;
    private String stage;
    private String size;
    private String description;

    public TaskDetails(String name, String time, String stage, String size,
                       String description) {
        this.name = name;
        this.time = time;
        this.stage = stage;
    }
    @Override
    public String toString() {
        return "TaskDetails{" +
                "name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", stage='" + stage + '\'' +
                ", size='" + size + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String toCSVString() {
        return name + "," + time + "," + stage + "," + size + "," + description;
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

    public String getTime() {
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
}

