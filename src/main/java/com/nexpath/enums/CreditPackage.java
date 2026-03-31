package com.nexpath.enums;

public enum CreditPackage {

    STARTER(5000, 4, "Starter Pack"),
    PRO(15000, 12, "Pro Pack"),
    POWER(50000, 40, "Power Pack"),
    CONNECTION_REFILL(2000, 3, "3 Connections Refill");

    private final int amountPaise;
    private final int credits;
    private final String displayName;

    CreditPackage(int amountPaise, int credits, String displayName) {
        this.amountPaise = amountPaise;
        this.credits = credits;
        this.displayName = displayName;
    }

    public int getAmountPaise() {
        return amountPaise;
    }

    public int getCredits() {
        return credits;
    }

    public String getDisplayName() {
        return displayName;
    }
}
