import java.util.*;
public class PreemptiveSJF implements Scheduler, GanttProvider {

    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;
    private Process currentProcess;
    private final List<GanttEntry> ganttChart = new ArrayList<>();
    //private List<GanttEntry> liveGantt = new ArrayList<>();

    @Override
    public void schedule(List<Process> processes) {
        ganttChart.clear();  // Clear previous Gantt entries before simulation

        int n = processes.size();
        int[] remaining = new int[n];
        boolean[] started = new boolean[n];
        int completed = 0;
        int time = 0;

        for (int i = 0; i < n; i++) {
            remaining[i] = processes.get(i).getBurstTime();
            processes.get(i).setStartTime(-1);
        }

        Process current = null;
        int start = -1;

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
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Select the process with the shortest remaining time that has arrived and still has time left
        Process currentProcess = processesCopy.stream()
                .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                .min(Comparator.comparingInt(Process::getRemainingTime)) // Shortest remaining time
                .orElse(null);

        if (currentProcess != null) {
            // Update the Gantt chart with the current process
            updateGantt(currentProcess, currentTime, liveGantt);

            // Decrease the remaining time for the current process by 1
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

            // If the process has finished execution, set the completion time
            if (currentProcess.getRemainingTime() == 0) {
                currentProcess.setCompletionTime(currentTime + 1);  // Completion time = current time + 1
                int turnaroundTime = currentProcess.getCompletionTime() - currentProcess.getArrivalTime();
                int waitingTime = turnaroundTime - currentProcess.getBurstTime();
                currentProcess.setTurnaroundTime(turnaroundTime);
                currentProcess.setWaitingTime(waitingTime);
            }
        }

        return currentProcess;
    }


    private void updateGantt(Process process, int currentTime, List<GanttEntry> liveGantt) { // p1 , 0, gannt
        if (liveGantt.isEmpty() || !Objects.equals(liveGantt.get(liveGantt.size() - 1).getProcessId(), process.getId())) { // t
            liveGantt.add(new GanttEntry(process.getId(), currentTime, currentTime + 1)); // livegantt = <(1, 0,1)>
        } else {
            GanttEntry last = liveGantt.get(liveGantt.size() - 1);
            last.setEndTime(currentTime + 1);
        }
    }


    @Override
    public void reset() {
        // Clear internal state like Gantt chart and process data
        ganttChart.clear();
        totalWaitingTime = 0;
        totalTurnaroundTime = 0;
        currentProcess = null;
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }
}
