package com.wanda.service;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.wanda.dto.PaymentResponse;
import com.wanda.dto.PlanDetails;
import com.wanda.dto.RedisUserDTO;
import com.wanda.repository.UsersRepository;
import com.wanda.utils.exceptions.CustomException;
import com.wanda.utils.exceptions.enums.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class PaymentService {

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.webhook.endpoint_secret}")
    private String ENDPOINT_SECRET ;

    private final String USER_CACHE_KEY = "user:";


    private UserService userService;
    private RedisTemplate<String, Object> redisTemplate;
    private UsersRepository usersRepository;
    private SimpMessagingTemplate  messagingTemplate;

    public PaymentService(UserService userService, RedisTemplate<String, Object> redisTemplate, UsersRepository usersRepository, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.usersRepository = usersRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    private void setStripe(){
        Stripe.apiKey = secretKey;
    }

    public PaymentResponse checkout(PlanDetails planDetails) {



        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(planDetails.getName())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("USD")
                .setUnitAmount(planDetails.getAmount())
                .setProductData(productData)
                .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams
                        .LineItem.builder()
                        .setQuantity(planDetails.getQuantity())
                        .setPriceData(priceData)
                        .build();

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(planDetails.getEmail()) // Email provided by the user
                .build();

        Customer customer = null;
        try{

            customer = Customer.create(customerParams);
        }catch(StripeException e){
            System.out.println("customer "+ customerParams.getEmail()+" not created");
            throw new CustomException("unable to create customer", HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        }

        long expirationTime = Instant.now().getEpochSecond() + (30 * 60);

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:9091/success")
                        .setCancelUrl("http://localhost:9091/cancel")
                        .addLineItem(lineItem)
                        .setCustomer(customer.getId())
                        .setExpiresAt(expirationTime)
                        .putMetadata("email", planDetails.getEmail())
                        .build();


        try {

            Session session = Session.create(params);

            return PaymentResponse
                    .builder()
                    .status("SUCCESS")
                    .message("Payment session created ")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();
        }catch (StripeException e){
            throw new CustomException(e.getMessage() , HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        } catch (Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.GENERAL_ERROR);
        }


    }


    public void webhook(String payload, String sigHeader){
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, ENDPOINT_SECRET);
        } catch (JsonSyntaxException e) {
            // Invalid payload
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        } catch (SignatureVerificationException e) {
            // Invalid signature
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        }

        // Deserialize the nested object inside the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Handle deserialization failure, possibly due to API version mismatch
            throw new CustomException("Invalid stripe object", HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        }

        // Handle the event
        switch (event.getType()) {
            case "payment_intent.succeeded":

                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;

                // Get the amount paid (in the smallest currency unit, e.g., cents)
                Long amountReceived = paymentIntent.getAmountReceived();

                String customerId = paymentIntent.getCustomer();
                String email;
                if (customerId != null) {
                    try {
                        Customer customer = Customer.retrieve(customerId);
                        email = customer.getEmail();


                    } catch (StripeException e) {
                        throw new CustomException("Failed to fetch customer: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.GENERAL_ERROR);
                    }
                } else {
                    throw new CustomException("Failed to fetch customer" , HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.GENERAL_ERROR);
                }



                System.out.println("Amount received: " + amountReceived);
                System.out.println("Customer Email: " + email);



                try {
                    if (!email.isBlank()) {
                        var userEmail = this.userService.makeUserPremium(email);

                        var existingUser = this.usersRepository.findByEmail(userEmail);

                        if (existingUser.isEmpty()) {
                            throw new CustomException("user not found", HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
                        }
                        var user = existingUser.get();


                        var redisUser = new RedisUserDTO(
                                user.getUsername(),
                                user.getEmail(),
                                user.getIsPremiumUser()
                        );

                        this.redisTemplate.opsForValue().set(USER_CACHE_KEY + redisUser.getEmail(), redisUser, Duration.ofMinutes(60));

                        this.messagingTemplate.convertAndSend("/topic/" + user.getUsername() +"/login", true);

                        System.out.println("done with payment");
                        break;
                    }
                }catch(Exception e){
                    System.out.println("exception: " + e.getMessage());
                }

                System.out.println(
                        "failing payment service"
                );
                throw new CustomException("Email not found in upcomming payment", HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);


                default:
                    throw new CustomException("Invalid event type", HttpStatus.BAD_REQUEST, ErrorCode.GENERAL_ERROR);
        }

    }

}
