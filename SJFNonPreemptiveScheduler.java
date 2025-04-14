package com.example.cpu_scheduler;

import java.util.*;

public class SJFNonPreemptiveScheduler implements Scheduler, GanttProvider {
    private List<GanttEntry> ganttChart = new ArrayList<>();
    private Process currentProcess;

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
    public Process scheduleStep(List<Process> processesCopy, int currentTime, List<GanttEntry> liveGantt) {
        if (currentProcess == null || currentProcess.getRemainingTime() == 0) {
            currentProcess = processesCopy.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .min(Comparator.comparingInt(Process::getRemainingTime))
                    .orElse(null);
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

    public void reset() {
        currentProcess = null;
    }
}
