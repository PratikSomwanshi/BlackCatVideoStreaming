package com.wanda.utils.exceptions;

import com.wanda.utils.exceptions.enums.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class CustomException extends  RuntimeException{

    private HttpStatus statusCode;
    private ErrorCode code;


    public CustomException(String msg, HttpStatus statusCode, ErrorCode code) {
        super(msg);
        this.statusCode = statusCode;
        this.code = code;
    }



}
