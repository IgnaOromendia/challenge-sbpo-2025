package org.sbpo2025.challenge;

public class TimeListener {

    private long lastTimeLimit;
    private long timeLimit;

    public TimeListener(long initialTimeLimit) {
        this.timeLimit = initialTimeLimit;
    }

    public boolean isLessOrEqualThan(long aTime) {
        return aTime >= this.timeLimit;
    }

    public boolean isGreaterThan(long aTime) {
		return aTime < this.timeLimit;
	}

    public void doubleTimeLimit() {
        updateTimeLimitTo(this.timeLimit * 2);
    }

    public void updateTimeLimitTo(long timeLimit) {
        if (timeLimit == 0) return;
        this.lastTimeLimit = this.timeLimit;
        this.timeLimit = timeLimit;
        System.out.println("Updated to: " + this.timeLimit);
    }

    @Override
    public String toString() {
        return String.valueOf(this.timeLimit);
    }

	public boolean fastIteration(long iterationDuration) {
        return iterationDuration < Math.max(timeLimit, lastTimeLimit) / 2;
	}
    
}
