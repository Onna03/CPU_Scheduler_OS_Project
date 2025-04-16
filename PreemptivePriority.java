import java.util.*;

public class PreemptivePriority implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private Process current = null; // Holds the current running process

    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;
    private Process currentProcess;
    @Override
    public void schedule(List<Process> processes) {
        int n = processes.size();
        int time = 0;
        int completed = 0;
        int[] remaining = new int[n];
        boolean[] started = new boolean[n];

        // Initialize remaining burst time for each process
        for (int i = 0; i < n; i++) {
            remaining[i] = processes.get(i).getBurstTime();
        }

        // Track the start time of each process
        while (completed < n) {
            Process nextProcess = null;
            int highestPriority = Integer.MAX_VALUE;

            // Check for available processes with the highest priority
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && remaining[i] > 0) {
                    if (p.getPriority() < highestPriority) {
                        highestPriority = p.getPriority();
                        nextProcess = p;
                    }
                }
            }

            if (nextProcess != null) {
                // Handle the switch between processes (preemption)
                if (current != nextProcess) {
                    if (current != null && remaining[Integer.parseInt(current.getId())] > 0) {
                        ganttChart.add(new GanttEntry("P" + current.getId(), time - 1, time)); // Add preemption to Gantt chart
                    }
                    current = nextProcess;
                    ganttChart.add(new GanttEntry("P" + current.getId(), time, time + 1)); // Add current process to Gantt chart
                }

                // Decrement the burst time for the currently running process
                remaining[Integer.parseInt(current.getId())]--;
                time++;

                // When a process finishes
                if (remaining[Integer.parseInt(current.getId())] == 0) {
                    current.setCompletionTime(time);
                    current.setTurnaroundTime(time - current.getArrivalTime());
                    current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                    completed++;
                }
            } else {
                // No process is ready, idle time occurs
                ganttChart.add(new GanttEntry("Idle", time, time + 1));
                time++;
            }
        }

        // Add any remaining process to the Gantt chart
        if (current != null) {
            ganttChart.add(new GanttEntry("P" + current.getId(), time, time + 1));
        }
    }

    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Select the highest priority process that has arrived and still has time
        Process currentProcess = processesCopy.stream()
                .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                .min(Comparator.comparingInt(Process::getPriority)) // lower value = higher priority
                .orElse(null);

        if (currentProcess != null) {
            // Log to Gantt chart
            updateGantt(currentProcess, currentTime, liveGantt);

            // Decrease remaining time
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

            // If process is finished after this second, finalize its stats
            if (currentProcess.getRemainingTime() == 0) {
                currentProcess.setCompletionTime(currentTime + 1);
                int turnaroundTime = currentProcess.getCompletionTime() - currentProcess.getArrivalTime();
                int waitingTime = turnaroundTime - currentProcess.getBurstTime();
                currentProcess.setTurnaroundTime(turnaroundTime);
                currentProcess.setWaitingTime(waitingTime);
            }
        }

        return currentProcess;
    }

    private void updateGantt(Process process, int currentTime, List<GanttEntry> liveGantt) {
        if (liveGantt.isEmpty() || liveGantt.get(liveGantt.size() - 1).getProcessId() != process.getId()) {
            liveGantt.add(new GanttEntry(process.getId(), currentTime, currentTime + 1));
        } else {
            GanttEntry last = liveGantt.get(liveGantt.size() - 1);
            last.setEndTime(currentTime + 1);
        }
    }


    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }

    @Override
    public void reset() {
        // Clear internal state like Gantt chart and process data
        ganttChart.clear();
        totalWaitingTime = 0;
        totalTurnaroundTime = 0;
        currentProcess = null;
    }
}
