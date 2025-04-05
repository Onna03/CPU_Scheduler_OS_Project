import java.util.*;

public class PriorityNonPreemptiveScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        processes.sort(Comparator
                .comparingInt(Process::getArrivalTime)
                .thenComparingInt(Process::getPriority));

        int time = 0;
        for (Process p : processes) {
            if (time < p.getArrivalTime()) {
                time = p.getArrivalTime();
            }

            p.setStartTime(time);
            int start = time;
            time += p.getBurstTime();
            p.setCompletionTime(time);
            p.setTurnaroundTime(time - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            ganttChart.add(new GanttEntry("P" + p.getId(), start, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
