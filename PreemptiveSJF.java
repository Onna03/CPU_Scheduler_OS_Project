import java.util.*;

public class PreemptiveSJF implements Scheduler, GanttProvider {
    private final List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        int n = processes.size();
        int[] remaining = new int[n];
        boolean[] finished = new boolean[n];
        int time = 0, completed = 0;

        for (int i = 0; i < n; i++) {
            remaining[i] = processes.get(i).getBurstTime();
        }

        Process current = null;
        int lastStart = -1;

        while (completed < n) {
            int idx = -1;
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (!finished[i] && p.getArrivalTime() <= time && remaining[i] < min && remaining[i] > 0) {
                    min = remaining[i];
                    idx = i;
                }
            }

            if (idx == -1) {
                // Idle time
                if (current != null && lastStart < time) {
                    ganttChart.add(new GanttEntry("P" + current.getId(), lastStart, time));
                    current = null;
                }
                time++;
                continue;
            }

            Process p = processes.get(idx);

            if (current != p) {
                if (current != null && lastStart < time) {
                    ganttChart.add(new GanttEntry("P" + current.getId(), lastStart, time));
                }
                current = p;
                lastStart = time;
            }

            // Execute for 1 unit
            remaining[idx]--;
            time++;

            // First time executing
            if (p.getStartTime() == -1) {
                p.setStartTime(time - 1);
            }

            if (remaining[idx] == 0) {
                finished[idx] = true;
                completed++;

                p.setCompletionTime(time);
                p.setTurnaroundTime(time - p.getArrivalTime());
                p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
            }
        }

        // Close off last Gantt entry
        if (current != null && lastStart < time) {
            ganttChart.add(new GanttEntry("P" + current.getId(), lastStart, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}

