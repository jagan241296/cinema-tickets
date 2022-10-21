package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */

public final class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        if (noOfTickets < 0) {
            throw new IllegalArgumentException("Requested number of tickets cannot be less than zero");
        }
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT(20), CHILD(10), INFANT(0);

        final int price;

        Type(int price) {
            this.price = price;
        }

        public int getPrice() {
            return this.price;
        }
    }
}
