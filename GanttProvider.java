package com.example.cpu_scheduler;
import java.util.List;

public interface GanttProvider {
    List<GanttEntry> getGanttChart();
}