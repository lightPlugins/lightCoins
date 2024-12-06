package io.lightstudios.coins.api;

import java.math.BigDecimal;

public class VirtualResponse {

    /**
     * Enum for types of Responses indicating the status of a method call.
     */
    public static enum VirtualResponseType {
        SUCCESS(1),
        FAILURE(2),
        MAX_BALANCE_EXCEED(3),
        NOT_NEGATIVE(4),
        NOT_ENOUGH(5),
        NOT_IMPLEMENTED(6);

        private final int id;

        VirtualResponseType(int id) {
            this.id = id;
        }

        int getId() {
            return id;
        }
    }

    /**
     * Amount modified by calling method
     */
    public final BigDecimal amount;
    /**
     * New balance of account
     */
    public final BigDecimal balance;
    /**
     * Success or failure of call. Using Enum of ResponseType to determine valid
     * outcomes
     */
    public final VirtualResponseType type;
    /**
     * Error message if the variable 'type' is ResponseType.FAILURE
     */
    public final String errorMessage;

    /**
     * Constructor for EconomyResponse
     * @param amount Amount modified during operation
     * @param balance New balance of account
     * @param type Success or failure type of the operation
     * @param errorMessage Error message if necessary (commonly null)
     */
    public VirtualResponse(BigDecimal amount, BigDecimal balance, VirtualResponseType type, String errorMessage) {
        this.amount = amount;
        this.balance = balance;
        this.type = type;
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if an operation was successful
     * @return Value
     */
    public boolean transactionSuccess() {
        return type == VirtualResponseType.SUCCESS;
    }
}
