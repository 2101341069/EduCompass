package com.xuecheng.base.execption;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(XueChengPlusException.class)
    public RestErrorResponse customException(XueChengPlusException e){
        String errMessage = e.getErrMessage();
        log.error("系统异常：{}",errMessage);
        RestErrorResponse restErrorResponse=new RestErrorResponse(errMessage);
        return restErrorResponse;
    }
    @ExceptionHandler(Exception.class)
    public RestErrorResponse exception(Exception e){
        String errMessage = e.getMessage();
        log.error("系统异常：{}",errMessage);
        if (errMessage.equals("系统异常：不允许访问")){
            return new RestErrorResponse("您没有权限访问，请联系管理严");
        }
        RestErrorResponse restErrorResponse=new RestErrorResponse(CommonError.UNKOWN_ERROR);
        return restErrorResponse;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors=new ArrayList<>();
        bindingResult.getFieldErrors().stream().forEach(item->{
            errors.add(item.getDefaultMessage());
        });
        String join= StringUtils.join(errors,",");
        return new RestErrorResponse(join);
    }
}
