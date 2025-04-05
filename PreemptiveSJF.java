import java.util.*;

public class PreemptiveSJF implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        int n = processes.size();
        int[] remaining = new int[n];
        boolean[] started = new boolean[n];
        int completed = 0;
        int time = 0;

        // Initialize remaining burst time and process start time
        for (int i = 0; i < n; i++) {
            remaining[i] = processes.get(i).getBurstTime();
            processes.get(i).setStartTime(-1);
        }

        Process current = null;
        int start = -1;

        // Preemptive SJF Scheduling
        while (completed < n) {
            int idx = -1;
            int minRemaining = Integer.MAX_VALUE;

            // Find process with the shortest remaining burst time
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && remaining[i] > 0 && remaining[i] < minRemaining) {
                    minRemaining = remaining[i];
                    idx = i;
                }
            }

            if (idx != -1) {
                Process p = processes.get(idx);

                // Mark start time for the first execution
                if (p.getStartTime() == -1) {
                    p.setStartTime(time);
                }

                // Handle context switch
                if (current != p) {
                    if (current != null && start < time) {
                        ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
                    }
                    current = p;
                    start = time;
                }

                // Execute for one time unit
                remaining[idx]--;
                time++;

                // If process finishes
                if (remaining[idx] == 0) {
                    p.setCompletionTime(time);
                    p.setTurnaroundTime(time - p.getArrivalTime());
                    p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
                    completed++;
                }
            } else {
                // If no process is available, mark idle time
                if (current != null && start < time) {
                    ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
                    current = null;
                }
                time++;
            }
        }

        // Finalize the last process entry in the Gantt chart
        if (current != null && start < time) {
            ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
