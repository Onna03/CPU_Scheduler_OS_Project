import java.util.*;

public class RoundRobinScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private int quantum;
    private double avgWaitingTime;
    private double avgTurnaroundTime;

    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public void schedule(List<Process> processes) {
        processes.sort(Comparator.comparingInt(Process::getArrivalTime)); // Sort by arrival time
        Queue<Process> queue = new LinkedList<>();
        Map<Process, Integer> remainingTime = new HashMap<>();
        int time = 0;
        int completed = 0;
        int index = 0;

        // Initialize the remaining time for each process
        for (Process p : processes) {
            remainingTime.put(p, p.getBurstTime());
            p.setStartTime(-1);  // Ensure the start time is uninitialized
        }

        while (completed < processes.size()) {
            // Add newly arrived processes to the queue
            while (index < processes.size() && processes.get(index).getArrivalTime() <= time) {
                queue.add(processes.get(index));
                index++;
            }

            // If the queue is empty, jump to the next process's arrival time
            if (queue.isEmpty()) {
                if (index < processes.size()) {
                    time = processes.get(index).getArrivalTime();
                }
                continue;
            }

            Process current = queue.poll();
            if (current.getStartTime() == -1) {
                current.setStartTime(time);
            }

            int execTime = Math.min(quantum, remainingTime.get(current));
            ganttChart.add(new GanttEntry("P" + current.getId(), time, time + execTime));

            time += execTime;
            remainingTime.put(current, remainingTime.get(current) - execTime);

            // Add newly arrived processes after execution
            while (index < processes.size() && processes.get(index).getArrivalTime() <= time) {
                queue.add(processes.get(index));
                index++;
            }

            if (remainingTime.get(current) == 0) {
                completed++;  // Mark process as completed
                current.setCompletionTime(time);
                current.setTurnaroundTime(current.getCompletionTime() - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
            } else {
                queue.add(current); // Requeue if not finished
            }
        }

        // Compute average waiting and turnaround times
        avgWaitingTime = processes.stream().mapToInt(Process::getWaitingTime).average().orElse(0.0);
        avgTurnaroundTime = processes.stream().mapToInt(Process::getTurnaroundTime).average().orElse(0.0);
    }

    @Override
    public List<GanttEntry> getGanttChart() {
        return ganttChart;
    }

    public double getAvgWaitingTime() {
        return avgWaitingTime;
    }

    public double getAvgTurnaroundTime() {
        return avgTurnaroundTime;
    }
}
