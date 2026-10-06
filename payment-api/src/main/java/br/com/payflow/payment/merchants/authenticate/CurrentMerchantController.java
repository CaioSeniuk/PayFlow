package br.com.payflow.payment.merchants.authenticate;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// Driving adapter of the authentication slice: GET /v1/merchants/me.
@RestController
class CurrentMerchantController {

    static final String API_KEY_HEADER = "X-Api-Key";

    private final AuthenticateMerchant authenticateMerchant;

    CurrentMerchantController(AuthenticateMerchant authenticateMerchant) {
        this.authenticateMerchant = authenticateMerchant;
    }

    @GetMapping("/v1/merchants/me")
    CurrentMerchantResponse currentMerchant(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey) {
        AuthenticatedMerchant merchant = authenticateMerchant.execute(apiKey);
        return new CurrentMerchantResponse(merchant.id(), merchant.name());
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    ResponseEntity<ProblemDetail> invalidApiKey(InvalidApiKeyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey header=\"" + API_KEY_HEADER + "\"")
                .body(problem);
    }

    record CurrentMerchantResponse(UUID id, String name) {
    }
}
