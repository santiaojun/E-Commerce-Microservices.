package com.cs6650.creditcard.dto;

public class CreditCardResponse {

    private String status;

    public CreditCardResponse() {
    }

    public CreditCardResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
