package com.company.recruitment.entity.enums;

public enum InterviewRound {
    L1_TECHNICAL("L1 - Technical Screening"),
    L2_TECHNICAL("L2 - Advanced Technical"),
    MANAGERIAL("Managerial Round"),
    HR_ROUND("HR Round");

    private final String displayName;

    InterviewRound(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
