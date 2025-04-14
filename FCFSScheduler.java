package com.example.cpu_scheduler;

import java.util.*;

public class FCFSScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private int totalWaitingTime = 0;
    private int totalTurnaroundTime = 0;
    private Process currentProcess;

    @Override
    public void schedule(List<Process> processes) {
        // Sort processes by arrival time
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        int currentTime = 0;

        for (Process p : processes) {
            if (currentTime < p.getArrivalTime()) {
                currentTime = p.getArrivalTime();
            }

            p.setStartTime(currentTime);
            int start = currentTime;
            currentTime += p.getBurstTime();
            p.setCompletionTime(currentTime);

            p.setTurnaroundTime(currentTime - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            totalWaitingTime += p.getWaitingTime();
            totalTurnaroundTime += p.getTurnaroundTime();

            ganttChart.add(new GanttEntry("P" + p.getId(), start, currentTime));
        }

        double avgWaitingTime = (double) totalWaitingTime / processes.size();
        double avgTurnaroundTime = (double) totalTurnaroundTime / processes.size();

        System.out.println("FCFS Average Waiting Time: " + avgWaitingTime);
        System.out.println("FCFS Average Turnaround Time: " + avgTurnaroundTime);

        Main.drawGanttChart(ganttChart);
    }

    private void updateGantt(Process process, int currentTime, List<GanttEntry> liveGantt) {
        if (liveGantt.isEmpty() || !liveGantt.get(liveGantt.size() - 1).getProcessId().equals(process.getId())) {
            liveGantt.add(new GanttEntry(process.getId(), currentTime, currentTime + 1));
        } else {
            GanttEntry last = liveGantt.get(liveGantt.size() - 1);
            last.setEndTime(currentTime + 1);
        }
    }

    @Override
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        if (currentProcess == null || currentProcess.getRemainingTime() <= 0) {
            currentProcess = processesCopy.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .min(Comparator.comparingInt(Process::getArrivalTime))
                    .orElse(null);

            if (currentProcess != null && currentProcess.getStartTime() == -1) {
                currentProcess.setStartTime(currentTime);
            }
        }

        if (currentProcess != null) {
            updateGantt(currentProcess, currentTime, liveGantt);
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);

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

    // Add this method for mode switching support
    @Override
    public void reset() {
        ganttChart.clear();
        totalWaitingTime = 0;
        totalTurnaroundTime = 0;
        currentProcess = null;
    }
}
