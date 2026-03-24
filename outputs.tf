package com.cs6650.creditcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreditCardRequest {

    @NotBlank(message = "Credit card number is required")
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$",
             message = "Invalid credit card format. Expected format: XXXX-XXXX-XXXX-XXXX")
    private String credit_card_number;

    public CreditCardRequest() {
    }

    public CreditCardRequest(String credit_card_number) {
        this.credit_card_number = credit_card_number;
    }

    public String getCredit_card_number() {
        return credit_card_number;
    }

    public void setCredit_card_number(String credit_card_number) {
        this.credit_card_number = credit_card_number;
    }
}
