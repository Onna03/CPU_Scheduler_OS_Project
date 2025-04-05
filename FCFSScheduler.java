import java.util.*;

public class FCFSScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        int time = 0;
        for (Process p : processes) {
            if (time < p.getArrivalTime()) {
                time = p.getArrivalTime();
            }
            p.setStartTime(time);
            int start = time;
            time += p.getBurstTime();
            p.setCompletionTime(time);
            p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            ganttChart.add(new GanttEntry("P" + p.getId(), start, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
