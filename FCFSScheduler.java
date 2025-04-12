import java.util.*;

public class FCFSScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;
    
    @Override
    public void schedule(List<Process> processes) {
        // Sort processes by arrival time
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        int currentTime = 0;  // Track the current time

        for (Process p : processes) {
            // Wait for the process to arrive
            if (currentTime < p.getArrivalTime()) {
                currentTime = p.getArrivalTime();  // Set currentTime to the arrival time of the process
            }

            // Set the start time of the process
            p.setStartTime(currentTime);
            int start = currentTime;
            currentTime += p.getBurstTime();  // Increase currentTime by the burst time of the process
            p.setCompletionTime(currentTime);

            // Calculate turnaround and waiting times
            p.setTurnaroundTime(p.getCompletionTime() - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            // Update total waiting and turnaround times
            totalWaitingTime += p.getWaitingTime();
            totalTurnaroundTime += p.getTurnaroundTime();

            // Add the Gantt chart entry for this process
            ganttChart.add(new GanttEntry("P" + p.getId(), start, currentTime));
        }

        // Calculate averages after all processes have been scheduled
        double avgWaitingTime = (double) totalWaitingTime / processes.size();
        double avgTurnaroundTime = (double) totalTurnaroundTime / processes.size();

        System.out.println("FCFS Average Waiting Time: " + avgWaitingTime);
        System.out.println("FCFS Average Turnaround Time: " + avgTurnaroundTime);

        // Draw Gantt chart (make sure to call from Main class)
        Main.drawGanttChart(ganttChart); // This ensures the Gantt chart is drawn from the main class
    }
    private void updateGantt(Process process, int currentTime, List<GanttEntry> liveGantt) {
        if (liveGantt.isEmpty() || liveGantt.get(liveGantt.size() - 1).getProcessId() != process.getId()) {
            liveGantt.add(new GanttEntry(process.getId(), currentTime, currentTime + 1));
        } else {
            GanttEntry last = liveGantt.get(liveGantt.size() - 1);
            last.setEndTime(currentTime + 1);
        }
    }

        private Process currentProcess;
    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Select new process if needed
        if (currentProcess == null || currentProcess.getRemainingTime() <= 0) {
            currentProcess = processesCopy.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .min(Comparator.comparingInt(Process::getArrivalTime))
                    .orElse(null);

            if (currentProcess != null && currentProcess.getStartTime() == -1) {
                currentProcess.setStartTime(currentTime);
            }
        }

        // If there's a current process, update Gantt and handle time
        if (currentProcess != null) {
            // Log the second it's running
            updateGantt(currentProcess, currentTime, liveGantt);

            // Decrement remaining time
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

            // If it's finished after this second, mark it done
            if (currentProcess.getRemainingTime() == 0) {
                currentProcess.setCompletionTime(currentTime + 1);

                int turnaroundTime = currentProcess.getCompletionTime() - currentProcess.getArrivalTime();
                int waitingTime = turnaroundTime - currentProcess.getBurstTime();

                currentProcess.setTurnaroundTime(turnaroundTime);
                currentProcess.setWaitingTime(waitingTime);
                
                Process finished = currentProcess;
                currentProcess = null;
                return finished; 
            }
        }

        return currentProcess;
    }
    
    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }

}
