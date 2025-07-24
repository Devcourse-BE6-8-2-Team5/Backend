package com.back.global.springdoc.globalExceptionHandler;


import com.back.global.springdoc.exception.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 글로벌 예외 핸들러 클래스
 * 각 예외에 대한 적절한 HTTP 상태 코드와 메시지를 포함한 응답 반환
 * 400: Bad Request
 * 404: Not Found
 * 500: Internal Server Error
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ServiceException: 서비스 계층에서 발생하는 커스텀 예외
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handle(ServiceException ex) {
        RsData<Void> rsData = ex.getRsData();

    }
}
