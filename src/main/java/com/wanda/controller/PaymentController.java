package com.wanda.controller;


import com.wanda.dto.PaymentResponse;
import com.wanda.dto.PlanDetails;
import com.wanda.service.PaymentService;
import com.wanda.utils.exceptions.enums.SuccessCode;
import com.wanda.utils.exceptions.response.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkoutProducts(@RequestBody PlanDetails planDetails) {
        PaymentResponse stripeResponse = paymentService.checkout(planDetails);

        var success = new SuccessResponse<PaymentResponse>(
                "Payment checkout done",
                SuccessCode.GENERAL_SUCCESS,
                stripeResponse
        );

        return new ResponseEntity<>(success, HttpStatus.OK);
    }

    @PostMapping("/webhook")
    public void handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {


            this.paymentService.webhook(payload, sigHeader);
    }
}
