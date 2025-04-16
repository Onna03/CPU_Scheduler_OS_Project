import java.util.*;

public class PriorityNonPreemptiveScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private Process currentProcess; // used in live mode
    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;
    @Override
    public void schedule(List<Process> processes) {
        ganttChart.clear();          // Clear previous Gantt chart
        currentProcess = null;       // Reset for a fresh simulation

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


    // private Process currentProcess;
    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Select the process with the highest priority (assuming lower numbers mean higher priority)
        if (currentProcess == null || currentProcess.getRemainingTime() == 0) {
            currentProcess = processesCopy.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .min(Comparator.comparingInt(Process::getPriority))  // Select process with the highest priority
                    .orElse(null);
        }

        if (currentProcess != null) {
            // Update the Gantt chart with the current process
            updateGantt(currentProcess, currentTime, liveGantt);

            // Decrease the remaining time for the current process by 1
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

            // If the process is finished, set its completion time
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


    private void updateGantt(Process process, int currentTime, List<GanttEntry> liveGantt) {
//        if (liveGantt.isEmpty() || liveGantt.get(liveGantt.size() - 1).getProcessId() != process.getId())

        if (liveGantt.isEmpty() || !Objects.equals(liveGantt.get(liveGantt.size() - 1).getProcessId(), process.getId())) {
            liveGantt.add(new GanttEntry(process.getId(), currentTime, currentTime + 1));
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
