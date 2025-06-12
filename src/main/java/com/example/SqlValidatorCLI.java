package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.List;

@SpringBootApplication
public class SqlValidatorCLI implements CommandLineRunner {

    @Autowired
    private SqlValidatorService sqlValidatorService;

    @Autowired
    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(SqlValidatorCLI.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -jar <jar-file> <sql-query-file1> <sql-query-file2> ...");
            SpringApplication.exit(context, () -> 1); // Exit with code 1 for incorrect usage
            System.exit(1);
            return;
        }

        boolean allFilesValid = true;

        // Process each file passed as an argument
        for (String sqlFilePath : args) {
            String fileName = new java.io.File(sqlFilePath).getName();
            String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(sqlFilePath)));

            List<String> results = sqlValidatorService.validateSqlQueries(fileContent);

            if (sqlValidatorService.areAllQueriesValid(results)) {
                System.out.println("\n\nValid file : " + sqlFilePath);
            } else {
                System.out.println("\n\nInvalid file: " + sqlFilePath);
                results.stream()
                        .filter(result -> result.startsWith("Invalid") || result.startsWith("Error validating SQL statement:"))
                        .forEach(System.out::println);
                allFilesValid = false;
            }
        }

        // Exit the application after processing all files
        int exitCode = allFilesValid ? 0 : 1;
        SpringApplication.exit(context, () -> exitCode);
        System.exit(exitCode);
    }
}