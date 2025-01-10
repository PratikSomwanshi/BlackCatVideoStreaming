package com.wanda.utils.exceptions.response;

import com.wanda.utils.exceptions.enums.SuccessCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class SuccessResponse<T> {
    private Boolean success = true;
    private String message = "Successfully done the request!";
    private Explanation explanation;
    private Object data = new ArrayList<>();

    public SuccessResponse(String explanation_msg, SuccessCode successCode, T data) {
        this.explanation = new Explanation(explanation_msg, successCode);
        this.data = Objects.isNull(data) ? new Object() : data;
    }

    public SuccessResponse(String explanation_msg,SuccessCode successCode, List<T> data) {
        this.explanation = new Explanation(explanation_msg, successCode);
        Optional.ofNullable(data).ifPresent(value -> this.data = value);
    }

}
