package com.example;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class SqlValidatorService {

    // Method to validate all SQL queries
    public List<String> validateSqlQueries(String fileContent) {
        List<String> results = new ArrayList<>();

        String decodedContent = StringEscapeUtils.unescapeHtml4(fileContent);
        String queryWithPlaceholders = replaceQuestionMarksWithPlaceholder(decodedContent);
        String[] statements = safeSplitSQL(queryWithPlaceholders);

        int lineNumber = 1;
        for (String statement : statements) {
            statement = statement.trim();
            if (!statement.isEmpty()) {
                int statementLineCount = statement.split("\n").length;
                validateSingleStatement(statement, results, lineNumber);
                lineNumber += statementLineCount;
            }
        }

        return results;
    }

    private String[] safeSplitSQL(String sqlContent) {
        List<String> statements = new ArrayList<>();
        int start = 0;
        boolean insideString = false;
        boolean insideComment = false;
        int beginEndBlockCount = 0;
        int parenthesisCount = 0;

        for (int i = 0; i < sqlContent.length(); i++) {
            char currentChar = sqlContent.charAt(i);

            if (currentChar == ';') {
                // Handle end
                if (!insideString && !insideComment && parenthesisCount == 0 && beginEndBlockCount == 0) {
                    statements.add(sqlContent.substring(start, i + 1).trim());
                    start = i + 1;
                }
            } else if (currentChar == '\'') {
                insideString = !insideString;
            } else if (currentChar == '-' && i + 1 < sqlContent.length() && sqlContent.charAt(i + 1) == '-') {
                insideComment = true;
                while (i < sqlContent.length() && sqlContent.charAt(i) != '\n') {
                    i++;
                }
                insideComment = false;
            } else if (currentChar == 'B' && i + 5 < sqlContent.length() && sqlContent.substring(i, i + 5).equals("BEGIN")) {
                if (!insideString && !insideComment) {
                    beginEndBlockCount++;
                }
            } else if (currentChar == 'E' && i + 4 < sqlContent.length() && sqlContent.substring(i, i + 4).equals("END;")) {
                if (!insideString && !insideComment) {
                    beginEndBlockCount--;
                }
            } else if (currentChar == '(') {
                if (!insideString && !insideComment) {
                    parenthesisCount++;
                }
            } else if (currentChar == ')') {
                if (!insideString && !insideComment) {
                    parenthesisCount--;
                }
            }

            if (parenthesisCount == 0 && beginEndBlockCount == 0 && currentChar == ';' && !insideString && !insideComment) {
                statements.add(sqlContent.substring(start, i + 1).trim());
                start = i + 1;
            }
        }

        if (start < sqlContent.length()) {
            statements.add(sqlContent.substring(start).trim());
        }

        return statements.toArray(new String[0]);
    }

    private String replaceQuestionMarksWithPlaceholder(String sqlContent) {
        StringBuilder result = new StringBuilder();
        boolean insideString = false;
        boolean insideComment = false;

        for (int i = 0; i < sqlContent.length(); i++) {
            char c = sqlContent.charAt(i);

            if (c == '\'' && !insideComment) {
                insideString = !insideString;
            }

            if (c == '-' && i < sqlContent.length() - 1 && sqlContent.charAt(i + 1) == '-' && !insideString) {
                insideComment = true;
            }

            if (c == '\n' && insideComment) {
                insideComment = false;
            }

            if (c == '?' && !insideString && !insideComment) {
                result.append("1");
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // Method to validate a single query
    private void validateSingleStatement(String statement, List<String> results, int lineNumber) {
        if (statement.toUpperCase().startsWith("INSERT INTO")) {
            validateInsertInto(statement, results, lineNumber);
        } else {
            validateWithParser(statement, results, lineNumber);
        }
    }

    private void validateInsertInto(String statement, List<String> results, int lineNumber) {
        Pattern patternWithColumns = Pattern.compile(
                "INSERT\\s+INTO\\s+`?(\\w+)`?\\s*\\((.*?)\\)\\s*VALUES\\s*(.+)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Pattern patternWithoutColumns = Pattern.compile(
                "INSERT\\s+INTO\\s+`?(\\w+)`?\\s+VALUES\\s*(.+)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );


        Matcher matcherWithColumns = patternWithColumns.matcher(statement);
        Matcher matcherWithoutColumns = patternWithoutColumns.matcher(statement);

        if (matcherWithColumns.find()) {
            validateInsertWithColumns(matcherWithColumns, results, lineNumber, statement);
        } else if (matcherWithoutColumns.find()) {
            validateInsertWithoutColumns(matcherWithoutColumns, results, lineNumber, statement);
        } else {
            validateWithParser(statement, results, lineNumber);
        }
    }

    private void validateInsertWithColumns(Matcher matcher, List<String> results, int lineNumber, String statement) {
        String tableName = matcher.group(1);
        String columns = matcher.group(2);
        String values = matcher.group(3);

        String[] columnList = columns.split("\\s*,\\s*");
        String[] valueRows = values.split("\\)\\s*,\\s*\\(");

        boolean isValid = true;
        for (String valueRow : valueRows) {
            valueRow = valueRow.replaceAll("^\\(|\\)$", "").trim();
            String[] valueList = valueRow.split("\\s*,\\s*");

            if (columnList.length != valueList.length) {
                results.add(String.format("Invalid query at line %d: Mismatch between number of columns (%d) and values (%d) in INSERT INTO statement for table %s",
                        lineNumber, columnList.length, valueList.length, tableName));
                isValid = false;
                break;
            }
        }

        if (isValid) {
            results.add(String.format("Valid SQL at line %d: %s", lineNumber, statement));
        }
    }

    private void validateInsertWithoutColumns(Matcher matcher, List<String> results, int lineNumber, String statement) {
        String values = matcher.group(2);
        String[] valueRows = values.split("\\)\\s*,\\s*\\(");

        if (valueRows.length > 1) {
            int firstRowValueCount = valueRows[0].replaceAll("^\\(|\\)$", "").trim().split("\\s*,\\s*").length;
            boolean isValid = true;

            for (int i = 1; i < valueRows.length; i++) {
                int currentRowValueCount = valueRows[i].replaceAll("^\\(|\\)$", "").trim().split("\\s*,\\s*").length;
                if (currentRowValueCount != firstRowValueCount) {
                    results.add(String.format("Invalid query at line %d: Mismatch in number of values between rows in multi-row INSERT INTO statement", lineNumber));
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                results.add(String.format("Valid SQL at line %d: %s", lineNumber, statement));
            }
        } else {
            validateWithParser(statement, results, lineNumber);
        }
    }

    private void validateWithParser(String statement, List<String> results, int lineNumber) {
        try {
            // Create lexer and parser
            CharStream input = CharStreams.fromString(statement);
            com.example.MariaDBLexer lexer = new com.example.MariaDBLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            MariaDBParser parser = new MariaDBParser(tokens);

            parser.removeErrorListeners();
            SyntaxErrorListener errorListener = new SyntaxErrorListener();
            parser.addErrorListener(errorListener);

            parser.root();

            if (errorListener.hasErrors()) {
                for (String error : errorListener.getErrors()) {
                    results.add(String.format("Invalid query at line %d: %s", lineNumber, error));
                }
            } else {
                results.add(String.format("Valid SQL at line %d: %s", lineNumber, statement));
            }
        } catch (Exception e) {
            results.add(String.format("Error validating SQL statement at line %d: %s", lineNumber, e.getMessage()));
        }
    }

    public boolean areAllQueriesValid(List<String> results) {
        return results.stream().noneMatch(result -> result.startsWith("Invalid") || result.startsWith("Error validating SQL statement:"));
    }

    private static class SyntaxErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            String error = String.format("Line %d:%d %s", line, charPositionInLine, msg);
            errors.add(error);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}