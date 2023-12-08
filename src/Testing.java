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
                        !errorHandler.doesSizeExist(errorHandler
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