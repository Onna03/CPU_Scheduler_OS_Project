public class Process {
    private int arrivalTime, burstTime, remainingBurstTime, priority;
    private String id;
    private int startTime = -1;
    private int completionTime = -1;
    private int waitingTime = 0;
    private int turnaroundTime = 0;
    private int remainingTime;
    private int elapsedTime = 0;
    private int originalBurstTime; // This holds the original burst time

    // Modified constructor to initialize originalBurstTime
    public Process(String id, int arrivalTime, int burstTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingBurstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
        this.originalBurstTime = burstTime;  // Initialize originalBurstTime with the burstTime value
        // this.startTime = -1;
    }
    // Deep copy constructor (important for live mode cloning)
    public Process(Process other) {
        this.id = other.id;
        this.arrivalTime = other.arrivalTime;
        this.burstTime = other.burstTime;
        this.originalBurstTime = other.originalBurstTime;
        this.remainingBurstTime = other.remainingBurstTime;
        this.remainingTime = other.remainingTime;
        this.priority = other.priority;
        this.startTime = other.startTime;
        this.completionTime = other.completionTime;
        this.waitingTime = other.waitingTime;
        this.turnaroundTime = other.turnaroundTime;
        this.elapsedTime = other.elapsedTime;
    }

    // === Getters ===
    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getOriginalBurstTime() { return originalBurstTime; }
    public int getRemainingBurstTime() { return remainingBurstTime; }
    public int getRemainingTime() { return remainingTime; }
    public int getPriority() { return priority; }
    public int getStartTime() { return startTime; }
    public int getCompletionTime() { return completionTime; }
    public int getWaitingTime() { return waitingTime; }
    public int getTurnaroundTime() { return turnaroundTime; }
    public int getElapsedTime() { return elapsedTime; }

    // === Setters ===
    public void setBurstTime(int burstTime) { this.burstTime = burstTime; }
    public void setRemainingBurstTime(int remainingBurstTime) { this.remainingBurstTime = remainingBurstTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }
    public void setElapsedTime(int elapsedTime) { this.elapsedTime = elapsedTime; }

}
