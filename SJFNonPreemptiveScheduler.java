import java.util.*;

public class SJFNonPreemptiveScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        int time = 0, completed = 0, n = processes.size();
        boolean[] isCompleted = new boolean[n];

        while (completed < n) {
            int idx = -1, minBurst = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (!isCompleted[i] && p.getArrivalTime() <= time && p.getBurstTime() < minBurst) {
                    minBurst = p.getBurstTime();
                    idx = i;
                }
            }

            if (idx == -1) {
                time++;
                continue;
            }

            Process p = processes.get(idx);
            if (time < p.getArrivalTime()) {
                time = p.getArrivalTime();
            }

            p.setStartTime(time);
            int start = time;
            time += p.getBurstTime();
            p.setCompletionTime(time);
            p.setTurnaroundTime(time - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
            isCompleted[idx] = true;
            completed++;

            ganttChart.add(new GanttEntry("P" + p.getId(), start, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
