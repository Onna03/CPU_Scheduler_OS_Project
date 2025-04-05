import javafx.application.Application;
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

import java.util.*;

public class Main extends Application {

    private ChoiceBox<String> schedulerChoice;
    private TextField idField, arrivalField, burstField, priorityField, quantumField;
    private Button addButton, startButton, resetButton;
    private TableView<Process> processTable;
    private Canvas ganttCanvas;
    private Text avgWaitingText, avgTurnaroundText, waitingDetailsText, turnaroundDetailsText;

    private List<Process> processes = new ArrayList<>();
    private boolean isRunning = false;
    private boolean quantumLocked = false;
    private int quantumValue = 2;

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
        schedulerChoice.getItems().addAll(
                "FCFS",
                "SJF (Non-Preemptive)",
                "SJF (Preemptive)",
                "Priority (Non-Preemptive)",
                "Priority (Preemptive)",
                "Round Robin"
        );

        HBox schedulerBox = new HBox(10, pickScheduler, schedulerChoice);
        schedulerBox.setPadding(new Insets(10));

        idField = new TextField();
        idField.setPromptText("ID");

        arrivalField = new TextField();
        arrivalField.setPromptText("Arrival Time");

        burstField = new TextField();
        burstField.setPromptText("Burst Time");

        priorityField = new TextField();
        priorityField.setPromptText("Priority");
        priorityField.setVisible(false);

        quantumField = new TextField();
        quantumField.setPromptText("Quantum (RR)");
        quantumField.setVisible(false);

        HBox inputBox = new HBox(10, idField, arrivalField, burstField, priorityField, quantumField);
        inputBox.setPadding(new Insets(10));

        addButton = new Button("Add Process");
        startButton = new Button("Start");
        resetButton = new Button("Reset");

        HBox controlBox = new HBox(10, addButton, startButton, resetButton);
        controlBox.setPadding(new Insets(10));

        processTable = new TableView<>();
        processTable.setPadding(new Insets(10));
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Process, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Process, Integer> arrivalCol = new TableColumn<>("Arrival Time");
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        TableColumn<Process, Integer> burstCol = new TableColumn<>("Burst Time");
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));

        TableColumn<Process, Integer> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));

        TableColumn<Process, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStartTime() == -1 ? "" : String.valueOf(cellData.getValue().getStartTime()))
        );

        TableColumn<Process, String> completionCol = new TableColumn<>("Completion Time");
        completionCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCompletionTime() == -1 ? "" : String.valueOf(cellData.getValue().getCompletionTime()))
        );

        TableColumn<Process, String> turnaroundCol = new TableColumn<>("Turnaround Time");
        turnaroundCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTurnaroundTime() == -1 ? "" : String.valueOf(cellData.getValue().getTurnaroundTime()))
        );

        TableColumn<Process, String> waitingCol = new TableColumn<>("Waiting Time");
        waitingCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getWaitingTime() == -1 ? "" : String.valueOf(cellData.getValue().getWaitingTime()))
        );

        processTable.getColumns().addAll(idCol, arrivalCol, burstCol, startCol, completionCol, turnaroundCol, waitingCol);

        VBox tableBox = new VBox(processTable);
        tableBox.setPadding(new Insets(10));

        ganttCanvas = new Canvas(1500, 100);
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 1500, 100);

        avgWaitingText = new Text("Avg Waiting Time: ");
        avgTurnaroundText = new Text("Avg Turnaround Time: ");
        waitingDetailsText = new Text("Waiting Details: ");
        turnaroundDetailsText = new Text("Turnaround Details: ");

        VBox bottomBox = new VBox(10, ganttCanvas, avgWaitingText, waitingDetailsText, avgTurnaroundText, turnaroundDetailsText);
        bottomBox.setPadding(new Insets(10));

        VBox root = new VBox(10, spaceAboveScheduler, schedulerBox, inputBox, controlBox, tableBox, bottomBox);
        Scene scene = new Scene(root, 1200, 600);
        stage.setScene(scene);
        stage.show();

        addButton.setOnAction(e -> addProcess());
        startButton.setOnAction(e -> {
            if (!isRunning) {
                startScheduling();
            }
        });
        resetButton.setOnAction(e -> resetAll());

        schedulerChoice.setOnAction(e -> {
            togglePriorityField();
            toggleQuantumField();

            if (schedulerChoice.getValue().contains("Priority")) {
                if (!processTable.getColumns().contains(priorityCol)) {
                    processTable.getColumns().add(3, priorityCol);
                }
            } else {
                processTable.getColumns().remove(priorityCol);
            }
        });
    }

    private void toggleQuantumField() {
        if (schedulerChoice.getValue().equals("Round Robin")) {
            quantumField.setVisible(true);
            priorityField.setVisible(false);
        } else {
            quantumField.setVisible(false);
        }
    }

    private void togglePriorityField() {
        if (schedulerChoice.getValue().contains("Priority")) {
            priorityField.setVisible(true);
            quantumField.setVisible(false);
        } else {
            priorityField.setVisible(false);
        }
    }

    private void addProcess() {
        try {
            String id = idField.getText();
            int arrival = Integer.parseInt(arrivalField.getText());
            int burst = Integer.parseInt(burstField.getText());
            int priority = priorityField.getText().isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(priorityField.getText());

            if (!quantumLocked && schedulerChoice.getValue().equals("Round Robin")) {
                try {
                    quantumValue = Integer.parseInt(quantumField.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert("Invalid quantum value. Please enter a valid integer.");
                    return;
                }
                quantumLocked = true;
            }

            Process p = new Process(id, arrival, burst, priority);
            processes.add(p);
            processTable.getItems().add(p);

            clearInputs();
        } catch (NumberFormatException e) {
            showAlert("Invalid input. Please enter valid numbers.");
        }
    }

    private void clearInputs() {
        idField.clear();
        arrivalField.clear();
        burstField.clear();
        priorityField.clear();
        quantumField.clear();
    }

    private void startScheduling() {
        isRunning = true;

        String choice = schedulerChoice.getValue();
        if (choice == null || choice.isEmpty()) {
            showAlert("Please select a scheduling algorithm.");
            isRunning = false;
            return;
        }

        Scheduler scheduler;

        if (choice.equals("Round Robin")) {
            try {
                quantumValue = quantumField.getText().isEmpty() ? quantumValue : Integer.parseInt(quantumField.getText());
            } catch (NumberFormatException e) {
                showAlert("Invalid quantum value.");
                isRunning = false;
                return;
            }
        }

        switch (choice) {
            case "SJF (Non-Preemptive)":
                scheduler = new SJFNonPreemptiveScheduler();
                break;
            case "SJF (Preemptive)":
                scheduler = new PreemptiveSJF();
                break;
            case "Priority (Non-Preemptive)":
                scheduler = new PriorityNonPreemptiveScheduler();
                break;
            case "Priority (Preemptive)":
                scheduler = new PreemptivePriority();
                break;
            case "Round Robin":
                scheduler = new RoundRobinScheduler(quantumValue);
                break;
            case "FCFS":
                scheduler = new FCFSScheduler();
                break;
            default:
                showAlert("Please select a valid scheduling algorithm.");
                isRunning = false;
                return;
        }

        List<Process> processesCopy = new ArrayList<>();
        for (Process p : processes) {
            processesCopy.add(new Process(p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getPriority()));
        }

        scheduler.schedule(processesCopy);

        List<GanttEntry> ganttEntries = null;
        if (scheduler instanceof GanttProvider) {
            ganttEntries = ((GanttProvider) scheduler).getGanttChart();
        }

        if (ganttEntries == null) {
            ganttEntries = new ArrayList<>();
            for (Process p : processesCopy) {
                ganttEntries.add(new GanttEntry("P" + p.getId(), p.getStartTime(), p.getCompletionTime()));
            }
        }

        drawGanttChart(ganttEntries);
        showAverages(processesCopy);
        updateTableWithResults(processesCopy);
    }

    private void drawGanttChart(List<GanttEntry> ganttEntries) {
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());

        int currentX = 10;
        int unitWidth = 30;
        int boxHeight = 50;

        for (GanttEntry entry : ganttEntries) {
            int width = (entry.getEndTime() - entry.getStartTime()) * unitWidth;

            gc.setFill(Color.LIGHTBLUE);
            gc.fillRoundRect(currentX, 30, width, boxHeight, 10, 10);
            gc.setStroke(Color.BLACK);
            gc.strokeRoundRect(currentX, 30, width, boxHeight, 10, 10);

            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 14));
            gc.fillText(entry.getProcessId(), currentX + width / 2 - 10, 55);

            gc.setStroke(Color.BLACK);
            gc.strokeText(String.valueOf(entry.getStartTime()), currentX - 5, 80);
            currentX += width;
        }

        if (!ganttEntries.isEmpty()) {
            GanttEntry lastEntry = ganttEntries.get(ganttEntries.size() - 1);
            gc.strokeText(String.valueOf(lastEntry.getEndTime()), currentX - 5, 80);
        }
    }

    private void updateTableWithResults(List<Process> scheduled) {
        processTable.getItems().clear();
        processTable.getItems().addAll(scheduled);
    }

    private void showAverages(List<Process> scheduled) {
        double totalWaiting = 0;
        double totalTurnaround = 0;
        StringBuilder waitingSum = new StringBuilder();
        StringBuilder turnaroundSum = new StringBuilder();

        int n = scheduled.size();

        for (int i = 0; i < n; i++) {
            Process p = scheduled.get(i);
            totalWaiting += p.getWaitingTime();
            totalTurnaround += p.getTurnaroundTime();

            waitingSum.append(p.getWaitingTime());
            turnaroundSum.append(p.getTurnaroundTime());

            if (i < n - 1) {
                waitingSum.append(" + ");
                turnaroundSum.append(" + ");
            }
        }

        waitingDetailsText.setText("Waiting Details: " + waitingSum + " = " + (int) totalWaiting);
        turnaroundDetailsText.setText("Turnaround Details: " + turnaroundSum + " = " + (int) totalTurnaround);

        avgWaitingText.setText("Avg Waiting Time: " + String.format("%.2f", totalWaiting / n));
        avgTurnaroundText.setText("Avg Turnaround Time: " + String.format("%.2f", totalTurnaround / n));
    }

    private void resetAll() {
        processes.clear();
        processTable.getItems().clear();
        isRunning = false;
        avgWaitingText.setText("Avg Waiting Time: ");
        avgTurnaroundText.setText("Avg Turnaround Time: ");
        waitingDetailsText.setText("Waiting Details: ");
        turnaroundDetailsText.setText("Turnaround Details: ");
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 1500, 100);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
}
