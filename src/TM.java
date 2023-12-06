import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
                // implement start function
            case "stop":
                stop(args[1]);
                // implement stop function
            case "summary":

            case "delete":

            case "size":

            case "rename":

            case "describe":
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
        tasks.add(taskDetails);
        taskLog.logWrite(tasks);
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

  //need to change this code to our own parser
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
        return size != null ? size : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }
}

