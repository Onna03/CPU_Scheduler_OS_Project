import java.util.*;

public class SJFNonPreemptiveScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private int time = 0;
    private boolean[] isCompleted;
    private List<Process> processes;
    private int completed = 0;

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


    private Process currentProcess;
    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Select the process with the shortest remaining time that has arrived and is not finished
        if (currentProcess == null || currentProcess.getRemainingTime() == 0) {
            currentProcess = processesCopy.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0) // Only processes that have arrived and not finished
                    .min(Comparator.comparingInt(Process::getRemainingTime)) // Get the one with the shortest remaining time
                    .orElse(null);
        }

        if (currentProcess != null) {
            // Update the Gantt chart for the current process at the current time step
            updateGantt(currentProcess, currentTime, liveGantt);

            // Decrease the remaining time for the selected process
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

            // If the process has finished, set its completion time
            if (currentProcess.getRemainingTime() == 0) {
                currentProcess.setCompletionTime(currentTime + 1); // Set the completion time (current time + 1)
                int turnaroundTime = currentProcess.getCompletionTime() - currentProcess.getArrivalTime();
                int waitingTime = turnaroundTime - currentProcess.getBurstTime();
                currentProcess.setTurnaroundTime(turnaroundTime); // Turnaround Time = Completion Time - Arrival Time
                currentProcess.setWaitingTime(waitingTime); // Waiting Time = Turnaround Time - Burst Time
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

}
