package com.accounting;

public class ValidationService {
    public void validateBalanced(double debit, double credit) {
        if (debit <= 0 || credit <= 0) throw new IllegalArgumentException("Amounts must be positive");
        if (Math.abs(debit - credit) > 0.000001)
            throw new IllegalArgumentException("Debit must equal credit. Difference: " + (debit - credit));
    }

    public void validateAccountType(String type) {
        if (!"Asset".equals(type) && !"Liability".equals(type) && !"Equity".equals(type)
                && !"Expense".equals(type) && !"Revenue".equals(type) && !"Other".equals(type))
            throw new IllegalArgumentException("Unsupported account type: " + type);
    }
}
