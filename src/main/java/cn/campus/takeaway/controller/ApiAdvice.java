package cn.campus.takeaway.controller;

import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestControllerAdvice
public class ApiAdvice {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException error) { return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(error.getReason(),"请求失败"))); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("message",error.getMessage())); }
    @ExceptionHandler({IllegalStateException.class,DuplicateKeyException.class})
    ResponseEntity<?> conflict(RuntimeException error) { return ResponseEntity.status(409).body(Map.of("message",error instanceof DuplicateKeyException ? "数据已存在，请刷新后重试" : error.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> fields(MethodArgumentNotValidException error) {
        var fields=new LinkedHashMap<String,String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(),"请检查此项输入"));
        return ResponseEntity.badRequest().body(Map.of("message","请检查表单中标记的输入项","fields",fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception error) { return ResponseEntity.badRequest().body(Map.of("message","请求格式不正确或包含不支持的字段")); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException error) {
        LoggerFactory.getLogger(getClass()).error("Database operation failed",error);
        return ResponseEntity.status(503).body(Map.of("message","数据服务暂时不可用，当前操作未完成，请稍后重试"));
    }
}
