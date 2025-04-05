public class GanttEntry {
    private String processId;
    private int startTime;
    private int endTime;

    public GanttEntry(String processId, int startTime, int endTime) {
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getProcessId() {  // This is the correct getter
        return processId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }
}
