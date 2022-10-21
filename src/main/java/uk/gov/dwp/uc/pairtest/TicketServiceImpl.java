package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS_PER_REQUEST = 20;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    public TicketPaymentService getTicketPaymentService() {
        return ticketPaymentService;
    }

    public SeatReservationService getSeatReservationService() {
        return seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateInputParameters(accountId, ticketTypeRequests);
        checkForTicketPurchaseLimitInSingleTransaction(ticketTypeRequests);
        checkForAdultTicketInRequest(ticketTypeRequests);
        makePurchase(accountId, ticketTypeRequests);
    }

    private static void validateInputParameters(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Account Id used to purchase tickets is not valid");
        }

        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("A minimum of one ticket is needed to make a purchase");
        }
    }

    private void checkForTicketPurchaseLimitInSingleTransaction(TicketTypeRequest[] ticketTypeRequests) {

        // Total number of tickets in a single request cannot be more than 20
        var totalTickets = 0;
        for (TicketTypeRequest ticket : ticketTypeRequests) {
            totalTickets += ticket.getNoOfTickets();
        }

        if (totalTickets > MAX_TICKETS_PER_REQUEST) {
            throw new InvalidPurchaseException("Cannot purchase more than 20 tickets ini a single request");
        }
    }

    private void checkForAdultTicketInRequest(TicketTypeRequest[] ticketTypeRequests) {

        // Either Child or Infant alone cannot purchase tickets without a minimum of single Adult ticket
        for (TicketTypeRequest ticket : ticketTypeRequests) {
            if (TicketTypeRequest.Type.ADULT.equals(ticket.getTicketType()))
                return;
        }

        // throw exception if no adult found
        throw new InvalidPurchaseException("A minimum of one adult is needed with either Infant/child tickets in a purchase");
    }

    private void makePurchase(Long accountId, TicketTypeRequest[] ticketTypeRequests) {
        var totalAmountToPurchase = 0;
        var totalSeatsToBook = 0;
        for (TicketTypeRequest ticket : ticketTypeRequests) {
            totalAmountToPurchase += (ticket.getNoOfTickets() * ticket.getTicketType().getPrice());
            if (!TicketTypeRequest.Type.INFANT.equals(ticket.getTicketType())) {
                totalSeatsToBook += ticket.getNoOfTickets();
            }
        }

        processPaymentForTickets(accountId, totalAmountToPurchase);
        processSeatReservation(accountId, totalSeatsToBook);
    }

    private void processPaymentForTickets(Long accountId, int totalAmountToPurchase) {
        if (totalAmountToPurchase == 0)
            throw new InvalidPurchaseException("Purchase amount cannot be zero. Please add a ticket to the request");
        getTicketPaymentService().makePayment(accountId, totalAmountToPurchase);
    }

    private void processSeatReservation(Long accountId, int totalSeatsToBook) {
        getSeatReservationService().reserveSeat(accountId, totalSeatsToBook);
    }
}
