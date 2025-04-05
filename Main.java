import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
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
    private Text avgWaitingText, avgTurnaroundText;

    private List<Process> processes = new ArrayList<>();
    private boolean isRunning = false;

    private int quantumValue = 2;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("CPU Scheduler");

        // Create a region with a fixed height to add space above the scheduler dropdown
        Region spaceAboveScheduler = new Region();
        spaceAboveScheduler.setPrefHeight(20);  // Set the height to control the space above the dropdown

        // Top: Scheduler Choice
        Text pickScheduler = new Text("Choose Scheduling Algorithm: ");
        schedulerChoice = new ChoiceBox<>();
        schedulerChoice.getItems().addAll("FCFS", "SJF (Non-Preemptive)", "SJF (Preemptive)", "Priority (Non-Preemptive)", "Priority (Preemptive)", "Round Robin");

        // Arrange "Choose Scheduling Algorithm" and the dropdown horizontally
        HBox schedulerBox = new HBox(10, pickScheduler, schedulerChoice);
        schedulerBox.setPadding(new Insets(10));

        // Input Fields
        idField = new TextField(); idField.setPromptText("ID");
        arrivalField = new TextField(); arrivalField.setPromptText("Arrival Time");
        burstField = new TextField(); burstField.setPromptText("Burst Time");

        priorityField = new TextField(); priorityField.setPromptText("Priority");
        priorityField.setVisible(false); // Initially hidden
        quantumField = new TextField(); quantumField.setPromptText("Quantum (RR)");
        quantumField.setVisible(false); // Initially hidden

        // Use HBox for input fields including both priority and quantum fields
        HBox inputBox = new HBox(10, idField, arrivalField, burstField, priorityField, quantumField);
        inputBox.setPadding(new Insets(10));

        // Add, Start, Reset Buttons
        addButton = new Button("Add Process");
        startButton = new Button("Start");
        resetButton = new Button("Reset");

        HBox controlBox = new HBox(10, addButton, startButton, resetButton);
        controlBox.setPadding(new Insets(10));

        // Gantt Chart Canvas
        ganttCanvas = new Canvas(600, 100);
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 600, 100);

        avgWaitingText = new Text("Avg Waiting Time: ");
        avgTurnaroundText = new Text("Avg Turnaround Time: ");

        VBox bottomBox = new VBox(10, ganttCanvas, avgWaitingText, avgTurnaroundText);
        bottomBox.setPadding(new Insets(10));

        // Arrange the scheduler choice at the top, followed by the input box and control box
        VBox root = new VBox(10, spaceAboveScheduler, schedulerBox, inputBox, controlBox, bottomBox);
        Scene scene = new Scene(root, 800, 360);
        stage.setScene(scene);
        stage.show();

        // Event Handlers
        addButton.setOnAction(e -> addProcess());
        startButton.setOnAction(e -> {
            if (!isRunning) {
                startScheduling();
            }
        });
        resetButton.setOnAction(e -> resetAll());

        // Listener for scheduler choice to show/hide quantum field and priority field
        schedulerChoice.setOnAction(e -> {
            togglePriorityField(); // Make sure the priority field visibility is toggled
            toggleQuantumField();  // Make sure the quantum field visibility is toggled
        });
    }

    private void toggleQuantumField() {
        // If Round Robin is selected, make the quantum field visible
        if (schedulerChoice.getValue().equals("Round Robin")) {
            quantumField.setVisible(true);
            priorityField.setVisible(false);  // Hide priority field
        } else {
            quantumField.setVisible(false);
        }
    }

    private void togglePriorityField() {
        // If "Priority (Preemptive)" or "Priority (Non-Preemptive)" is selected, make the priority field visible
        if (schedulerChoice.getValue().equals("Priority (Preemptive)") || schedulerChoice.getValue().equals("Priority (Non-Preemptive)")) {
            priorityField.setVisible(true);
            quantumField.setVisible(false);  // Hide quantum field
        } else {
            priorityField.setVisible(false);
        }
    }

    private boolean quantumLocked = false; // Ensure quantum is set before adding the first process

    private void addProcess() {
        try {
            String id = idField.getText();
            int arrival = Integer.parseInt(arrivalField.getText());
            int burst = Integer.parseInt(burstField.getText());
            int priority = priorityField.getText().isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(priorityField.getText());

            // Lock Quantum Value Before First Process is Added
            if (!quantumLocked && schedulerChoice.getValue().equals("Round Robin")) {
                try {
                    quantumValue = Integer.parseInt(quantumField.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert("Invalid quantum value. Please enter a valid integer.");
                    return;
                }
                quantumLocked = true; // Prevent future changes
            }

            Process p = new Process(id, arrival, burst, priority);
            processes.add(p);

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

        // Get quantum if needed
        if (choice.equals("Round Robin")) {
            try {
                quantumValue = quantumField.getText().isEmpty() ? quantumValue : Integer.parseInt(quantumField.getText());
            } catch (NumberFormatException e) {
                showAlert("Invalid quantum value.");
                isRunning = false;
                return;
            }
        }

        // Create scheduler based on user choice
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

        // Clone the process list
        List<Process> processesCopy = new ArrayList<>();
        for (Process p : processes) {
            processesCopy.add(new Process(p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getPriority()));
        }

        // Run the scheduler
        scheduler.schedule(processesCopy);

        // Retrieve Gantt chart entries from the scheduler if supported
        List<GanttEntry> ganttEntries = null;
        if (scheduler instanceof GanttProvider) {
            ganttEntries = ((GanttProvider) scheduler).getGanttChart();
        }

        // Fallback: generate basic Gantt entries using start and burst time
        if (ganttEntries == null) {
            ganttEntries = new ArrayList<>();
            for (Process p : processesCopy) {
                ganttEntries.add(new GanttEntry("P" + p.getId(), p.getStartTime(), p.getCompletionTime()));
            }
        }

        drawGanttChart(ganttEntries);
        showAverages(processesCopy);
    }


    private void drawGanttChart(List<GanttEntry> ganttEntries) {
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());

        int currentX = 10;
        int unitWidth = 30; // Width per time unit
        int boxHeight = 50; // Box height

        for (GanttEntry entry : ganttEntries) {
            int width = (entry.getEndTime() - entry.getStartTime()) * unitWidth;

            // Draw rounded rectangle
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRoundRect(currentX, 30, width, boxHeight, 10, 10);
            gc.setStroke(Color.BLACK);
            gc.strokeRoundRect(currentX, 30, width, boxHeight, 10, 10);

            // Centered text
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 14));
            gc.fillText(entry.getProcessId(), currentX + width / 2 - 10, 55);

            // Time markers
            gc.setStroke(Color.BLACK);
            gc.strokeText(String.valueOf(entry.getStartTime()), currentX - 5, 80);
            currentX += width;
        }

        // Draw last time marker
        if (!ganttEntries.isEmpty()) {
            GanttEntry lastEntry = ganttEntries.get(ganttEntries.size() - 1);
            gc.strokeText(String.valueOf(lastEntry.getEndTime()), currentX - 5, 80);
        }
    }

    private void showAverages(List<Process> scheduled) {
        double totalWaiting = 0;
        double totalTurnaround = 0;
        int n = scheduled.size();

        for (Process p : scheduled) {
            totalWaiting += p.getWaitingTime();
            totalTurnaround += p.getTurnaroundTime();
        }

        avgWaitingText.setText("Avg Waiting Time: " + String.format("%.2f", totalWaiting / n));
        avgTurnaroundText.setText("Avg Turnaround Time: " + String.format("%.2f", totalTurnaround / n));
    }

    private void resetAll() {
        processes.clear();
        isRunning = false;
        avgWaitingText.setText("Avg Waiting Time: ");
        avgTurnaroundText.setText("Avg Turnaround Time: ");
        GraphicsContext gc = ganttCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, 600, 100);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
}
