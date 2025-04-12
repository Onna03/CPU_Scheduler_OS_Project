import java.util.List;

public interface Scheduler {
    void schedule(List<Process> processes);
    Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt); // Updated to accept int
    List<GanttEntry> getGanttChart();

}
