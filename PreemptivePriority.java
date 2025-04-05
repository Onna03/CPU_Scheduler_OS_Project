import java.util.*;

public class PreemptivePriority implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        int n = processes.size();
        int[] remainingBurst = new int[n];
        boolean[] started = new boolean[n];

        for (int i = 0; i < n; i++) {
            remainingBurst[i] = processes.get(i).getBurstTime();
        }

        int completed = 0;
        int time = 0;
        Process currentProcess = null;
        int executionStart = -1;

        while (completed < n) {
            int minPriority = Integer.MAX_VALUE;
            int selectedIndex = -1;

            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && remainingBurst[i] > 0 && p.getPriority() < minPriority) {
                    minPriority = p.getPriority();
                    selectedIndex = i;
                }
            }

            if (selectedIndex != -1) {
                Process selectedProcess = processes.get(selectedIndex);

                if (!started[selectedIndex]) {
                    selectedProcess.setStartTime(time);
                    started[selectedIndex] = true;
                }

                if (currentProcess != selectedProcess) {
                    if (currentProcess != null && executionStart != -1 && executionStart < time) {
                        ganttChart.add(new GanttEntry("P" + currentProcess.getId(), executionStart, time));
                    }
                    currentProcess = selectedProcess;
                    executionStart = time;
                }

                remainingBurst[selectedIndex]--;
                time++;

                if (remainingBurst[selectedIndex] == 0) {
                    selectedProcess.setCompletionTime(time);
                    selectedProcess.setTurnaroundTime(time - selectedProcess.getArrivalTime());
                    selectedProcess.setWaitingTime(selectedProcess.getTurnaroundTime() - selectedProcess.getBurstTime());
                    completed++;
                }

            } else {
                if (currentProcess != null && executionStart != -1 && executionStart < time) {
                    ganttChart.add(new GanttEntry("P" + currentProcess.getId(), executionStart, time));
                    currentProcess = null;
                }
                time++;
            }
        }

        if (currentProcess != null && executionStart != -1 && executionStart < time) {
            ganttChart.add(new GanttEntry("P" + currentProcess.getId(), executionStart, time));
        }
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
