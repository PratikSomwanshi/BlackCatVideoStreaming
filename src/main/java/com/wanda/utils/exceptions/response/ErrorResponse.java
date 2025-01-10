package com.wanda.utils.exceptions.response;

import com.wanda.utils.exceptions.enums.ErrorCode;
import com.wanda.utils.exceptions.enums.SuccessCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ErrorResponse {
    private Boolean success;
    private String message;
    private Explanation error;
    private List<?> data= new ArrayList<>();

    public ErrorResponse( String explanation, ErrorCode code) {
        this.success = false;
        this.message = "Something went wrong";
        this.error = new Explanation(explanation, code);
    }

    public ErrorResponse( String message, String explanation, ErrorCode code) {
        this.success = false;
        this.message = message;
        this.error = new Explanation(explanation, code);
    }
}


@Getter
@Setter
class Explanation{
    private String explanation;
    private String code;

    public Explanation(String explanation, ErrorCode code) {
        this.explanation = explanation;
        this.code = code.toString();
    }

    public Explanation(String explanation, SuccessCode code) {
        this.explanation = explanation;
        this.code = code.toString();
    }
}