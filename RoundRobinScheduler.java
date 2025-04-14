package com.example.cpu_scheduler;
import java.util.*;

public class RoundRobinScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private int quantum;
    private double avgWaitingTime;
    private double avgTurnaroundTime;
    private Queue<Process> queue = new LinkedList<>();
    private Process currentProcess = null;
    private int rrTimeLeft = 0;
    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;


    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public void schedule(List<Process> processes) {
        ganttChart.clear();            // Clear Gantt chart
        queue.clear();                 // Reset queue
        currentProcess = null;         // Reset current process
        rrTimeLeft = 0;                // Reset time slice

        processes.sort(Comparator.comparingInt(Process::getArrivalTime));
        Map<Process, Integer> remainingTime = new HashMap<>();
        int time = 0;
        int completed = 0;
        int index = 0;

        // Initialize remaining time
        for (Process p : processes) {
            remainingTime.put(p, p.getBurstTime());
            p.setStartTime(-1);
        }

        while (completed < processes.size()) {
            while (index < processes.size() && processes.get(index).getArrivalTime() <= time) {
                queue.add(processes.get(index));
                index++;
            }

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

            while (index < processes.size() && processes.get(index).getArrivalTime() <= time) {
                queue.add(processes.get(index));
                index++;
            }

            if (remainingTime.get(current) == 0) {
                completed++;
                current.setCompletionTime(time);
                current.setTurnaroundTime(current.getCompletionTime() - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
            } else {
                queue.add(current);
            }
        }

        avgWaitingTime = processes.stream().mapToInt(Process::getWaitingTime).average().orElse(0.0);
        avgTurnaroundTime = processes.stream().mapToInt(Process::getTurnaroundTime).average().orElse(0.0);
    }

    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        // Add new arrivals
        for (Process p : processesCopy) {
            if (p.getArrivalTime() == currentTime && p.getRemainingTime() > 0) {
                queue.offer(p);
            }
        }

        // Time slice expired or current finished
        if (currentProcess == null || rrTimeLeft == 0 || currentProcess.getRemainingTime() == 0) {
            if (currentProcess != null && currentProcess.getRemainingTime() > 0) {
                queue.offer(currentProcess);
            }
            currentProcess = queue.poll();
            rrTimeLeft = quantum;
        }

        if (currentProcess != null) {
            updateGantt(currentProcess, currentTime, liveGantt);
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
            rrTimeLeft--;

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
