package com.example.cpu_scheduler;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class Main extends Application {

    private ChoiceBox<String> schedulerChoice;
    private TextField idField, arrivalField, burstField, priorityField, quantumField;
    private Button addButton, startButton, resetButton, toggleLiveModeButton;
    private TableView<Process> processTable;
    private static Canvas ganttCanvas;
    private Text avgWaitingText, avgTurnaroundText;
    private List<Process> processes = new ArrayList<>();
    private boolean isRunning = false;
    private boolean isLiveMode = false;
    private Timeline liveTimeline;
    private List<Process> processesCopy = new ArrayList<>();
    private int quantumValue = 2;
    private List<Process> liveProcesses = new ArrayList<>();
    private Scheduler scheduler;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("CPU Scheduler");

        Region spaceAboveScheduler = new Region();
        spaceAboveScheduler.setPrefHeight(20);

        Text pickScheduler = new Text("Choose Scheduling Algorithm: ");
        schedulerChoice = new ChoiceBox<>();
        schedulerChoice.getItems().addAll("FCFS", "SJF (Non-Preemptive)", "SJF (Preemptive)", "Priority (Non-Preemptive)", "Priority (Preemptive)", "Round Robin");

        HBox schedulerBox = new HBox(10, pickScheduler, schedulerChoice);
        schedulerBox.setPadding(new Insets(10));

        idField = new TextField(); idField.setPromptText("ID");
        arrivalField = new TextField(); arrivalField.setPromptText("Arrival Time");
        burstField = new TextField(); burstField.setPromptText("Burst Time");
        priorityField = new TextField(); priorityField.setPromptText("Priority"); priorityField.setVisible(false);
        quantumField = new TextField(); quantumField.setPromptText("Quantum (RR)"); quantumField.setVisible(false);

        HBox inputBox = new HBox(10, idField, arrivalField, burstField, priorityField, quantumField);
        inputBox.setPadding(new Insets(10));

        addButton = new Button("Add Process");
        startButton = new Button("Start");
        resetButton = new Button("Reset");
        toggleLiveModeButton = new Button("Switch to Live Mode");

        HBox controlBox = new HBox(10, addButton, startButton, resetButton, toggleLiveModeButton);
        controlBox.setPadding(new Insets(10));

        TableColumn<Process, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));

        TableColumn<Process, Integer> arrivalCol = new TableColumn<>("Arrival Time");
        arrivalCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getArrivalTime()).asObject());

        TableColumn<Process, Integer> burstCol = new TableColumn<>("Remaining Time");
        burstCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRemainingTime()).asObject());

        processTable = new TableView<>();
        processTable.getColumns().addAll(idCol, arrivalCol, burstCol);
        VBox tableBox = new VBox(processTable);
        tableBox.setPadding(new Insets(10));

        ganttCanvas = new Canvas(1500, 100);
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 1500, 100);

        avgWaitingText = new Text("Avg Waiting Time: ");
        avgTurnaroundText = new Text("Avg Turnaround Time: ");

        VBox bottomBox = new VBox(10, tableBox, ganttCanvas, avgWaitingText, avgTurnaroundText);
        bottomBox.setPadding(new Insets(10));

        VBox root = new VBox(10, spaceAboveScheduler, schedulerBox, inputBox, controlBox, bottomBox);
        Scene scene = new Scene(root, 1200, 500);
        stage.setScene(scene);
        stage.show();

        addButton.setOnAction(e -> addProcess());
        startButton.setOnAction(e -> { if (!isRunning) startScheduling(); });
        resetButton.setOnAction(e -> resetAll());
        schedulerChoice.setOnAction(e -> { togglePriorityField(); toggleQuantumField(); });
        toggleLiveModeButton.setOnAction(e -> toggleLiveMode());
    }

    private void toggleLiveMode() {
        isLiveMode = !isLiveMode;
        System.out.println("Live mode now");
        toggleLiveModeButton.setText(isLiveMode ? "Switch to Normal Mode" : "Switch to Live Mode");
        ganttCanvas.getGraphicsContext2D().clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());
    }

    private void toggleQuantumField() {
        quantumField.setVisible("Round Robin".equals(schedulerChoice.getValue()));
        if (quantumField.isVisible()) priorityField.setVisible(false);
    }

    private void togglePriorityField() {
        priorityField.setVisible(schedulerChoice.getValue().contains("Priority"));
        if (priorityField.isVisible()) quantumField.setVisible(false);
    }

    private void addProcess() {
        try {
            String id = idField.getText();
            int arrival = Integer.parseInt(arrivalField.getText());
            int burst = Integer.parseInt(burstField.getText());
            int priority = priorityField.getText().isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(priorityField.getText());
            // Check for duplicate ID
            boolean idExists = processes.stream().anyMatch(p -> p.getId().equalsIgnoreCase(id));
            if (idExists) {
                showAlert("A process with this ID already exists. Please use a unique ID.");
                return;
            }
            Process p = new Process(id, arrival, burst, priority);
            processes.add(p);
            processTable.getItems().add(p);
            // If we are in live mode and simulation is running, also add to liveProcesses
            if (isLiveMode && isRunning) {
                processesCopy.add(new Process(id, arrival, burst, priority)); // [MODIFIED] add live too
            }
//            if (isLiveMode && isRunning) {
//                liveProcesses.add(p);
//            }
            clearInputs();
        } catch (NumberFormatException e) {
            showAlert("Invalid input. Please enter valid numbers.");
        }
    }

    private void clearInputs() {
        idField.clear(); arrivalField.clear(); burstField.clear(); priorityField.clear(); quantumField.clear();
    }

    private void startScheduling() {
        isRunning = true;
        String choice = schedulerChoice.getValue();
        if (choice == null || choice.isEmpty()) {
            showAlert("Please select a scheduling algorithm.");
            isRunning = false;
            return;
        }

        if (choice.equals("Round Robin")) {
            try {
                quantumValue = Integer.parseInt(quantumField.getText());
            } catch (NumberFormatException e) {
                showAlert("Invalid quantum value.");
                isRunning = false;
                return;
            }
        }

        // Instantiate the scheduler
        switch (choice) {
            case "SJF (Non-Preemptive)": scheduler = new SJFNonPreemptiveScheduler(); break;
            case "SJF (Preemptive)": scheduler = new PreemptiveSJF(); break;
            case "Priority (Non-Preemptive)": scheduler = new PriorityNonPreemptiveScheduler(); break;
            case "Priority (Preemptive)": scheduler = new PreemptivePriority(); break;
            case "Round Robin": scheduler = new RoundRobinScheduler(quantumValue); break;
            case "FCFS": scheduler = new FCFSScheduler(); break;
            default: showAlert("Please select a valid scheduling algorithm."); isRunning = false; return;
        }

        // Reset scheduler state
        scheduler.reset();  // Call reset before scheduling

        processesCopy.clear();
        for (Process p : processes) {
            processesCopy.add(new Process(p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getPriority()));
        }

        if (isLiveMode) {
            animateScheduling(scheduler);
        } else {
            scheduler.schedule(processesCopy);
            if (scheduler instanceof GanttProvider)
                drawGanttChart(((GanttProvider) scheduler).getGanttChart());
            showAverages(processesCopy);
        }
    }

    private boolean allProcessesCompleted(List<Process> processes) {
        return processes.stream().allMatch(p -> p.getRemainingTime() <= 0);
    }


    private void animateScheduling(Scheduler scheduler) {
        //liveProcesses = processesCopy;  // Link the global list
        List<GanttEntry> liveGantt = new ArrayList<>();
        liveTimeline = new Timeline();
        liveTimeline.setCycleCount(Timeline.INDEFINITE);
        final int[] timeStep = {0};

        KeyFrame keyFrame = new KeyFrame(Duration.seconds(1), e -> {
            // Schedule step (get process to run at this time)
            scheduler.scheduleStep(processesCopy, timeStep[0], liveGantt);
            // Always update chart, even for the last time unit
//            Platform.runLater(() -> {
//                drawGanttChart(liveGantt);
//                processTable.getItems().setAll(liveProcesses);
//            });
//            //  Always refresh the process table
//
//            Platform.runLater(() -> {
//                processTable.getItems().clear();
//                processTable.getItems().addAll(processesCopy);  // This will now reflect the updated remaining time
//            });
            Platform.runLater(() -> {
                drawGanttChart(liveGantt);
                processTable.getItems().clear();
                processTable.getItems().addAll(processesCopy);
            });


            // Check if all are done AFTER chart update
            //boolean allDone = processesCopy.stream().allMatch(p -> p.getRemainingTime() <= 0);
//            if (allDone) {
//                liveTimeline.stop();
//                isRunning = false;
//                showAverages(processesCopy);
//            }
            if (allProcessesCompleted(processesCopy)) {
                liveTimeline.stop();
                isRunning = false;
                showAverages(processesCopy);
            }

            timeStep[0]++;
        });

        liveTimeline.getKeyFrames().add(keyFrame);
        liveTimeline.play();
    }

    public static void drawGanttChart(List<GanttEntry> ganttEntries) { // <1, 1 , 2>,
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());
        int currentX = 10, unitWidth = 30, boxHeight = 50;

        for (GanttEntry entry : ganttEntries) {
            int width = (entry.getEndTime() - entry.getStartTime()) * unitWidth;
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRoundRect(currentX, 30, width, boxHeight, 10, 10);
            gc.setStroke(Color.BLACK);
            gc.strokeRoundRect(currentX, 30, width, boxHeight, 10, 10);
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 14));
            gc.fillText(entry.getProcessId(), currentX + width / 2 - 10, 55); // 2
            gc.setStroke(Color.BLACK);
            gc.strokeText(String.valueOf(entry.getStartTime()), currentX - 5, 80);
            currentX += width;
        }

        if (!ganttEntries.isEmpty()) {
            GanttEntry lastEntry = ganttEntries.get(ganttEntries.size() - 1); // last entry = p1
            gc.strokeText(String.valueOf(lastEntry.getEndTime()), currentX - 5, 80); // 1
        }
    }

    private void showAverages(List<Process> scheduled) {
        double avgWaiting = 0, avgTurnaround = 0;
        for (Process p : scheduled) {
            avgWaiting += p.getWaitingTime();
            avgTurnaround += p.getTurnaroundTime();
        }
        avgWaitingText.setText("Avg Waiting Time: " + String.format("%.2f", avgWaiting / scheduled.size()));
        avgTurnaroundText.setText("Avg Turnaround Time: " + String.format("%.2f", avgTurnaround / scheduled.size()));
    }

    private void resetAll() {
        if (liveTimeline != null) liveTimeline.stop();
        processes.clear(); processTable.getItems().clear(); clearInputs();
        ganttCanvas.getGraphicsContext2D().clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());
        avgWaitingText.setText("Avg Waiting Time: ");
        avgTurnaroundText.setText("Avg Turnaround Time: ");
        isRunning = false;

        // Reset the active scheduler state (if one exists)
        if (scheduler != null) {
            scheduler.reset(); // Reset the scheduler when everything is cleared
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}