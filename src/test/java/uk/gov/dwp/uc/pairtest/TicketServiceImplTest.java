package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;

    private TicketServiceImpl ticketServiceImpl;

    @BeforeEach
    void setUp() {
        ticketServiceImpl = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    void purchaseTicketsWithInvalidAccount() {
        var exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ticketServiceImpl.purchaseTickets(0L, new TicketTypeRequest(Type.ADULT, 1)));
        Assertions.assertEquals("Account Id used to purchase tickets is not valid", exception.getMessage());

        exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ticketServiceImpl.purchaseTickets(null, new TicketTypeRequest(Type.ADULT, 1)));
        Assertions.assertEquals("Account Id used to purchase tickets is not valid", exception.getMessage());
    }

    @Test
    void purchaseTicketsWithInvalidTicketRequests() {
        var invalidTicketInRequestException = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> ticketServiceImpl.purchaseTickets(1001L, (TicketTypeRequest[]) null));
        Assertions.assertEquals("A minimum of one ticket is needed to make a purchase", invalidTicketInRequestException.getMessage());

        invalidTicketInRequestException = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> ticketServiceImpl.purchaseTickets(1001L));
        Assertions.assertEquals("A minimum of one ticket is needed to make a purchase", invalidTicketInRequestException.getMessage());
    }

    @Test
    void purchaseTicketsWithExceedingTicketPurchaseLimit() {
        var exceedingTicketLimitException = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> ticketServiceImpl.purchaseTickets(1001L, new TicketTypeRequest(Type.ADULT, 15),
                        new TicketTypeRequest(Type.CHILD, 10)));
        Assertions.assertEquals("Cannot purchase more than 20 tickets ini a single request", exceedingTicketLimitException.getMessage());
    }

    @Test
    void purchaseTicketsWithoutAnAdultInTicket() {
        var NoAdultException = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> ticketServiceImpl.purchaseTickets(1001L, new TicketTypeRequest(Type.CHILD, 10),
                        new TicketTypeRequest(Type.CHILD, 5), new TicketTypeRequest(Type.INFANT, 5)));
        Assertions.assertEquals("A minimum of one adult is needed with either Infant/child tickets in a purchase", NoAdultException.getMessage());
    }

    @Test
    void purchaseTicketsIfTotalAmountToPurchaseIsZero() {
        var totalAmountNilException = Assertions.assertThrows(InvalidPurchaseException.class,
                () -> ticketServiceImpl.purchaseTickets(1001L, new TicketTypeRequest(Type.ADULT, 0),
                        new TicketTypeRequest(Type.CHILD, 0), new TicketTypeRequest(Type.INFANT, 0)));
        Assertions.assertEquals("Purchase amount cannot be zero. Please add a ticket to the request", totalAmountNilException.getMessage());
    }


    @Test
    void TicketTypeRequestCreateObjectWithNegativeTickets() {
        var negativeTicketException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TicketTypeRequest(Type.ADULT, -1));

        Assertions.assertEquals("Requested number of tickets cannot be less than zero", negativeTicketException.getMessage());
    }

    @Test
    void purchaseTickets() {
        Mockito.doNothing()
                .when(ticketPaymentService).makePayment(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt());
        Mockito.doNothing()
                .when(seatReservationService).reserveSeat(ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt());

        ticketServiceImpl.purchaseTickets(1001L, new TicketTypeRequest(Type.ADULT, 10),
                new TicketTypeRequest(Type.CHILD, 8), new TicketTypeRequest(Type.INFANT, 2));

        Mockito.verify(ticketPaymentService).makePayment(1001, 280);
        Mockito.verify(seatReservationService).reserveSeat(1001, 18);
    }
}