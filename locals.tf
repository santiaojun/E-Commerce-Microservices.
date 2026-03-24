package com.cs6650.creditcard.controller;

import com.cs6650.creditcard.dto.CreditCardRequest;
import com.cs6650.creditcard.dto.CreditCardResponse;
import com.cs6650.creditcard.dto.ErrorResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/credit-card-authorizer")
public class CreditCardController {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardController.class);
    private final Random random = new Random();

    @Value("${creditcard.authorization.approval-rate:0.9}")
    private double approvalRate;

    /**
     * Credit card authorization endpoint
     *
     * @param request Credit card authorization request
     * @return 200 if authorized (90%), 402 if declined (10%), 400 if invalid format
     */
    @PostMapping("/authorize")
    public ResponseEntity<?> authorize(@Valid @RequestBody CreditCardRequest request) {
        String creditCardNumber = request.getCredit_card_number();

        logger.info("Received authorization request for card: {}",
                    maskCreditCard(creditCardNumber));

        // Generate random decision: 90% approval, 10% decline
        boolean isApproved = random.nextDouble() < approvalRate;

        if (isApproved) {
            logger.info("Card authorized: {}", maskCreditCard(creditCardNumber));
            CreditCardResponse response = new CreditCardResponse("Authorized");
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Card declined: {}", maskCreditCard(creditCardNumber));
            ErrorResponse errorResponse = new ErrorResponse("DECLINED", "Payment declined");
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse);
        }
    }

    /**
     * Handle validation errors (invalid credit card format)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null ? fieldError.getDefaultMessage()
                                                 : "Invalid request format";

        logger.error("Validation error: {}", errorMessage);
        return new ErrorResponse("INVALID_FORMAT", errorMessage);
    }

    /**
     * Mask credit card number for logging (show only last 4 digits)
     */
    private String maskCreditCard(String creditCardNumber) {
        if (creditCardNumber == null || creditCardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + creditCardNumber.substring(creditCardNumber.length() - 4);
    }
}
