package com.example.cleaning.services;// package com.example.buysell.services;

import com.example.cleaning.exceptions.TimeSlotAlreadyBookedException;
import com.example.cleaning.models.Product;
import com.example.cleaning.models.ProductRequest;
import com.example.cleaning.models.User;
import com.example.cleaning.models.enums.RequestStatus;
import com.example.cleaning.repositories.ProductRepository;
import com.example.cleaning.repositories.ProductRequestRepository;
import com.example.cleaning.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRequestService {
    private final ProductRequestRepository productRequestRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MailSenderService mailSenderService;

//    public void createRequest(Long productId, String date, String time, Principal principal) {
//        User user = userRepository.findByEmail(principal.getName());
//        Product product = productRepository.findById(productId).orElse(null);
//
//        if (user != null && product != null) {
//            ProductRequest request = new ProductRequest();
//            request.setUser(user);
//            request.setProduct(product);
//            request.setSelectedDate(LocalDate.parse(date));
//            request.setSelectedTime(LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")));
//
//            productRequestRepository.save(request);
//        }
//    }


    public boolean isTimeSlotAvailable(Long productId, LocalDate date, LocalTime time) {
        return !productRequestRepository.existsByProductIdAndSelectedDateAndSelectedTimeAndStatus(
                productId, date, time, RequestStatus.APPROVED);
    }

    public void createRequest(Long productId, String date, String time, Principal principal) throws TimeSlotAlreadyBookedException {
        User user = userRepository.findByEmail(principal.getName());
        Product product = productRepository.findById(productId).orElse(null);

        if (user != null && product != null) {
            LocalDate selectedDate = LocalDate.parse(date);
            LocalTime selectedTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));

            if (isTimeSlotAvailable(productId, selectedDate, selectedTime)) {
                ProductRequest request = new ProductRequest();
                request.setUser(user);
                request.setProduct(product);
                request.setSelectedDate(selectedDate);
                request.setSelectedTime(selectedTime);

                productRequestRepository.save(request);
            } else {
                throw new TimeSlotAlreadyBookedException("The selected time slot is already booked.");
            }
        }
    }


//    public void approveRequest(Long requestId) {
//        ProductRequest request = productRequestRepository.findById(requestId).orElse(null);
//        if (request != null && request.getStatus() == RequestStatus.PENDING) {
//            request.setStatus(RequestStatus.APPROVED);
//            productRequestRepository.save(request);
//        }
//    }

    public void approveRequest(Long requestId) {
        ProductRequest request = productRequestRepository.findById(requestId).orElse(null);
        if (request != null && request.getStatus() == RequestStatus.PENDING) {
            request.setStatus(RequestStatus.APPROVED);
            productRequestRepository.save(request);


            String recipientEmail = request.getUser().getEmail();
            String subject = "Request Approved: " + request.getProduct().getTitle();


            String htmlContent = String.format(
                    "<html>"
                            + "<body style='font-family: Arial, sans-serif; color: #333; background-color: #f9f9f9; padding: 20px;'>"
                            + "<div style='max-width: 600px; margin: 0 auto; background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);'>"
                            + "<h2 style='color: #4CAF50; text-align: center;'>Your Request Has Been Approved!</h2>"
                            + "<p style='font-size: 16px;'>Dear Customer,</p>"
                            + "<p style='font-size: 16px; line-height: 1.6;'>"
                            + "We are pleased to inform you that your request for the service <strong>\"%s\"</strong> has been successfully approved!"
                            + "</p>"
                            + "<h3 style='color: #4CAF50; border-bottom: 1px solid #ddd; padding-bottom: 5px;'>Details:</h3>"
                            + "<ul style='list-style-type: none; padding: 0; font-size: 16px; line-height: 1.6;'>"
                            + "<li><strong>Service Name:</strong> %s</li>"
                            + "<li><strong>Time:</strong> %s O'clock</li>"
                            + "<li><strong>Date:</strong> %s</li>"
                            + "</ul>"
                            + "<p style='font-size: 16px; line-height: 1.6;'>"
                            + "Thank you for choosing us! If you have any questions, feel free to contact our support team."
                            + "</p>"
                            + "<p style='margin-top: 20px; font-size: 16px; line-height: 1.6;'>"
                            + "Best regards,<br>"
                            + "<strong>Your Support Team</strong>"
                            + "</p>"
                            + "</div>"
                            + "</body>"
                            + "</html>",
                    request.getProduct().getTitle(),
                    request.getProduct().getTitle(),
                    request.getSelectedTime(),
                    request.getSelectedDate()
            );

            // html message
            try {
                mailSenderService.sendHtmlEmail(recipientEmail, subject, htmlContent);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    //public void approveRequest(Long requestId) {
//    ProductRequest request = productRequestRepository.findById(requestId).orElse(null);
//    if (request != null && request.getStatus() == RequestStatus.PENDING) {
//        request.setStatus(RequestStatus.APPROVED);
//        productRequestRepository.save(request);
//        String recipientEmail = request.getUser().getEmail();  // Get user's email address
//        String subject = "Request Approved: " + request.getProduct().getTitle();
//
//        String text = String.format(
//                "Dear Customer,\n\n"
//                        + "We are pleased to inform you that your request for the service \"%s\" has been successfully approved!\n\n"
//                        + "Details:\n"
//                        + "- Service Name: %s\n"
//                        + "- Time: %s O'clock\n"
//                        + "- Date: %s\n\n"
//                        + "Thank you for choosing us!\n\n"
//                        + "Best regards,\n"
//                        + "Your Support Team",
//                request.getProduct().getTitle(),
//                request.getProduct().getTitle(),
//                request.getSelectedTime(),
//                request.getSelectedDate()
//        );
//
//        mailSenderService.sendEmail(recipientEmail, subject, text);
//
//
//    }
//}
    public void rejectRequest(Long requestId) {
        ProductRequest request = productRequestRepository.findById(requestId).orElse(null);
        if (request != null && request.getStatus() == RequestStatus.PENDING) {
            request.setStatus(RequestStatus.REJECTED);
            productRequestRepository.save(request);
        }
    }

    public List<ProductRequest> getAllRequests() {
        return productRequestRepository.findAll();
    }


    public List<String> getBookedTimes(Long productId, LocalDate date) {
        List<ProductRequest> requests = productRequestRepository.findAllByProductIdAndSelectedDateAndStatus(
                productId, date, RequestStatus.APPROVED);

        return requests.stream()
                .map(request -> request.getSelectedTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList());
    }


}
