import java.util.*;

public class PreemptivePriority implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        int n = processes.size(), completed = 0, time = 0;
        int[] remaining = new int[n];
        boolean[] started = new boolean[n];

        for (int i = 0; i < n; i++) remaining[i] = processes.get(i).getBurstTime();

        Process current = null;
        int start = -1;

        while (completed < n) {
            int idx = -1, highestPriority = Integer.MAX_VALUE;

            // Check for available processes with the highest priority
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && remaining[i] > 0 && p.getPriority() < highestPriority) {
                    highestPriority = p.getPriority();
                    idx = i;
                }
            }

            if (idx != -1) {
                Process p = processes.get(idx);

                // Start time handling for the process
                if (!started[idx]) {
                    p.setStartTime(time);
                    started[idx] = true;
                }

                // Process switch logic for preemption
                if (current != p) {
                    if (current != null && start != -1 && start < time) {
                        ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
                    }
                    current = p;
                    start = time;
                }

                // Execute the process
                remaining[idx]--;
                time++;

                if (remaining[idx] == 0) {
                    p.setCompletionTime(time);
                    p.setTurnaroundTime(time - p.getArrivalTime());
                    p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
                    completed++;
                }
            } else {
                // Idle time: Add an entry for when no process is ready
                if (current != null && start != -1 && start < time) {
                    ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
                    current = null;
                }
                time++;
            }
        }

        // Final Gantt entry for the last process
        if (current != null && start != -1 && start < time) {
            ganttChart.add(new GanttEntry("P" + current.getId(), start, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
