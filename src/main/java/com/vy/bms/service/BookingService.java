package com.vy.bms.service;


import com.vy.bms.dto.*;
import com.vy.bms.exception.ResourceNotFoundException;
import com.vy.bms.exception.SeatUnavailableException;
import com.vy.bms.model.*;
import com.vy.bms.repository.BookingRepository;
import com.vy.bms.repository.ShowRepository;
import com.vy.bms.repository.ShowSeatRepository;
import com.vy.bms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Transactional
    public BookingDto createBooking(BookingRequestDto bookingRequest) {
        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Show show = showRepository.findById(bookingRequest.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));


        List<Long> seatIds = bookingRequest.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new SeatUnavailableException("Please select at least one seat");
        }

        List<ShowSeat> selectedSeats = showSeatRepository.findAllById(seatIds);
        if (selectedSeats.size() != new HashSet<>(seatIds).size()) {
            throw new ResourceNotFoundException("One or more selected seats were not found");
        }

        for(ShowSeat seat : selectedSeats) {
            if (!seat.getShow().getId().equals(show.getId())) {
                throw new SeatUnavailableException("Seat " + seat.getSeat().getSeatNumber() + " does not belong to this show");
            }
            if(!"AVAILABLE".equals(seat.getStatus())) {
                throw new SeatUnavailableException("Seat " + seat.getSeat().getSeatNumber() + " is not available");
            }

            seat.setStatus("LOCKED");
        }
        showSeatRepository.saveAll(selectedSeats);

        Double totalAmount = selectedSeats.stream()
                .mapToDouble(ShowSeat::getPrice)
                .sum();

        Payment payment = new Payment();
        payment.setAmount(totalAmount);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentMethod(bookingRequest.getPaymentMethod());
        payment.setStatus("SUCCESS");
        payment.setTransactionId(UUID.randomUUID().toString());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(totalAmount);
        booking.setBookingNumber(UUID.randomUUID().toString());
        booking.setPayment(payment);

        Booking saveBooking = bookingRepository.save(booking);


        selectedSeats.forEach(seat -> {
            seat.setStatus("BOOKED");
            seat.setBooking(saveBooking);
        });

        showSeatRepository.saveAll(selectedSeats);

        return mapToBookingDto(saveBooking,selectedSeats);
    }


    public BookingDto getBookingById(Long id)
    {
        Booking booking=bookingRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Booking Not Found"));
        List<ShowSeat> seats=showSeatRepository.findByBookingId(booking.getId());
        return mapToBookingDto(booking,seats);
    }

    public BookingDto getBookingByNumber(String bookingNumber)
    {
        Booking booking=bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(()->new ResourceNotFoundException("Booking Not Found"));
        List<ShowSeat> seats=showSeatRepository.findByBookingId(booking.getId());
        return mapToBookingDto(booking,seats);
    }

    public List<BookingDto> getBookingByUserId(Long userId)
    {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User Not Found");
        }
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(booking -> {
                    List<ShowSeat> seats=showSeatRepository.findByBookingId(booking.getId());
                    return mapToBookingDto(booking,seats);
                })
                .collect(Collectors.toList());

    }

    @Transactional
    public BookingDto cancelBooking(Long id)
    {
        Booking booking=bookingRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Booking Not found"));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        booking.setStatus("CANCELLED");

        List<ShowSeat> seats=showSeatRepository.findByBookingId(booking.getId());

        seats.forEach(seat->{
            seat.setStatus("AVAILABLE");
            seat.setBooking(null);
        });

        if (booking.getPayment()!=null)
        {
            booking.getPayment().setStatus("REFUNDED");
        }

        Booking updateBooking=bookingRepository.save(booking);
        showSeatRepository.saveAll(seats);
        return mapToBookingDto(updateBooking,seats);

    }


    private BookingDto mapToBookingDto(Booking booking, List<ShowSeat> seats)
    {
        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(booking.getId());
        bookingDto.setBookingNumber(booking.getBookingNumber());
        bookingDto.setBookingTime(booking.getBookingTime());
        bookingDto.setStatus(booking.getStatus());
        bookingDto.setTotalAmount(booking.getTotalAmount());

        UserDto userDto = new UserDto();
        userDto.setId(booking.getUser().getId());
        userDto.setEmail(booking.getUser().getEmail());
        userDto.setName(booking.getUser().getName());
        userDto.setPhoneNumber(booking.getUser().getPhoneNumber());

        bookingDto.setUser(userDto);

        ShowDto showDto = new ShowDto();
        showDto.setId(booking.getShow().getId());
        showDto.setStartTime(booking.getShow().getStartTime());
        showDto.setEndTime(booking.getShow().getEndTime());


        MovieDto movieDto = new MovieDto();
        movieDto.setId(booking.getShow().getMovie().getId());
        movieDto.setTitle(booking.getShow().getMovie().getTitle());
        movieDto.setDescription(booking.getShow().getMovie().getDescription());
        movieDto.setLanguage(booking.getShow().getMovie().getLanguage());
        movieDto.setGenre(booking.getShow().getMovie().getGenre());
        movieDto.setDurationMins(booking.getShow().getMovie().getDurationMins());
        movieDto.setReleaseDate(booking.getShow().getMovie().getReleaseDate());
        movieDto.setPosterUrl(booking.getShow().getMovie().getPosterUrl());

        showDto.setMovie(movieDto);

        ScreenDto screenDto = new ScreenDto();
        screenDto.setId(booking.getShow().getScreen().getId());
        screenDto.setName(booking.getShow().getScreen().getName());
        screenDto.setTotalSeats(booking.getShow().getScreen().getTotalSeats());

        Theater theater = booking.getShow().getScreen().getTheater();
        TheaterDto theaterDto = new TheaterDto();
        theaterDto.setId(theater.getId());
        theaterDto.setAddress(theater.getAddress());
        theaterDto.setCity(theater.getCity());
        theaterDto.setName(theater.getName());
        theaterDto.setTotalScreens(theater.getTotalScreens());

        screenDto.setTheater(theaterDto);
        showDto.setScreen(screenDto);
        bookingDto.setShow(showDto);

        List<ShowSeatDto> seatDtos = seats.stream()
                .map(seat -> {
                    ShowSeatDto seatDto = new ShowSeatDto();
                    seatDto.setId(seat.getId());
                    seatDto.setStatus(seat.getStatus());
                    seatDto.setPrice(seat.getPrice());


                    SeatDto baseSeatDto = new SeatDto();
                    baseSeatDto.setId(seat.getSeat().getId());
                    baseSeatDto.setSeatNumber(seat.getSeat().getSeatNumber());
                    baseSeatDto.setSeatType(seat.getSeat().getSeatType());
                    baseSeatDto.setBasePrice(seat.getSeat().getBasePrice());

                    seatDto.setSeat(baseSeatDto);

                    return seatDto;
                })
                .collect(Collectors.toList());
        bookingDto.setSeats(seatDtos);


        if(booking.getPayment() != null) {
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setId(booking.getPayment().getId());
            paymentDto.setAmount(booking.getPayment().getAmount());
            paymentDto.setPaymentMethod(booking.getPayment().getPaymentMethod());
            paymentDto.setPaymentTime(booking.getPayment().getPaymentTime());
            paymentDto.setStatus(booking.getPayment().getStatus());
            paymentDto.setTransactionId(booking.getPayment().getTransactionId());

            bookingDto.setPayment(paymentDto);
        }

        return bookingDto;
    }
}