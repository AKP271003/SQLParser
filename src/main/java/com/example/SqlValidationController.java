package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/sql-validator")
public class SqlValidationController {

    private final SqlValidatorService sqlValidatorService;

    @Autowired
    public SqlValidationController(SqlValidatorService sqlValidatorService) {
        this.sqlValidatorService = sqlValidatorService;
    }

    @PostMapping("/validate-file")
    public ResponseEntity<String> validateSqlFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileContent = new String(file.getBytes()).trim();
            if (fileContent.isEmpty()) {
                return new ResponseEntity<>("File is empty", HttpStatus.BAD_REQUEST);
            }

            List<String> results = sqlValidatorService.validateSqlQueries(fileContent);
            String response = String.join("\n", results);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Error processing file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/validate-query")
    public ResponseEntity<String> validateSqlQuery(@RequestBody String query) {
        try {
            List<String> results = sqlValidatorService.validateSqlQueries(query.trim());
            String response = String.join("\n", results);
            if (response.contains("Invalid SQL")) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            } else {
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error validating query: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

