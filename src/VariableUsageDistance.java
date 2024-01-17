import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableUsageDistance {

    private final String code;
    private int minDistance = 5;

    public VariableUsageDistance(String code) {
        // Remove empty lines and comments
        this.code = removeEmptyLinesAndComments(code);
    }

    public VariableUsageDistance(String code, boolean removeEmptyLinesAndComments) {
        // Remove empty lines and comments
        if (removeEmptyLinesAndComments) {
            this.code = removeEmptyLinesAndComments(code);
        } else {
            this.code = code;
        }
    }

    public VariableUsageDistance(String code, int minDistance) {
        // Remove empty lines and comments
        this.code = removeEmptyLinesAndComments(code);
        this.minDistance = minDistance;
    }

    public VariableUsageDistance(String code, int minDistance, boolean removeEmptyLinesAndComments) {
        // Remove empty lines and comments
        if (removeEmptyLinesAndComments) {
            this.code = removeEmptyLinesAndComments(code);
        } else {
            this.code = code;
        }
        this.minDistance = minDistance;
    }

    public DistanceResults calculateDistance() {
        return calculateDistance(new HashMap<>(0));
    }

    public DistanceResults calculateDistance(Map<Integer, List<Integer>> get_examples) {

        // Structure of Map in get_examples:
        // key = distance
        // value = array of size 4
            // [0] amount of times that are ignored before saving examples,
            // [1] amount of examples to save,
            // [2] amount of times that are ignored before saving double declaration examples,
            // [3] amount of double declaration examples to save

        double amountDeclarations = 0.0;
        double amountLongDeclarations = 0.0;
        double totalDistance = 0.0;
        double averageDistance = 0.0;
        double amountDoubleDeclarations = 0.0;
        double totalDoubleDistance = 0.0;
        double averageDoubleDistance = 0.0;
        Map<String, Double> distances = new HashMap<>();
        Map<String, Double> doubleDeclarations = new HashMap<>();
        Map<Integer, List<Integer>> examples = new HashMap<>();
        Map<Integer, List<Integer>> double_declaration_examples = new HashMap<>();
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(this.code);
        } catch (Exception e) {
            return new DistanceResults();
        }


        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : methodDeclarations) {
            Map<String, List<Double>> declarations = new HashMap<>();
            method.findAll(VariableDeclarator.class).forEach(variable -> {
                addToMap(declarations, variable.getNameAsString(), (double) variable.getBegin().get().line);
            });
            method.findAll(AssignExpr.class).forEach(assignExpr -> {
                assignExpr.getTarget().findAll(NameExpr.class).forEach(nameExpr -> {
                    addToMap(declarations, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                });
            });

            Map<String, List<Double>> usages = new HashMap<>();
            method.getBody().ifPresent(body -> body.getStatements().forEach(statement -> searchBody(usages, statement)));

            // For each declaration, check if there is a usage within the minDistance and at which line is the nearest declaration to the usage
            for (Map.Entry<String, List<Double>> entry : declarations.entrySet()) {
                String variableName = entry.getKey();
                List<Double> declarationLines = entry.getValue();
                List<Double> usageLines = usages.getOrDefault(variableName, new ArrayList<>());
                for (Double usageLine : usageLines) {
                    double minDistance = Double.MAX_VALUE;
                    double chosenDeclarationLine = 0;
                    for (Double declarationLine : declarationLines) {
                        double distance = usageLine - declarationLine;
                        if (distance > 0 && distance < minDistance) {
                            minDistance = distance;
                            chosenDeclarationLine = declarationLine;
                        }
                    }
                    if (minDistance >= this.minDistance && minDistance != Double.MAX_VALUE) {
                        // Add it to the number of saved distances if it exists
                        String minDistanceAsString = String.valueOf(minDistance);
                        distances.put(minDistanceAsString, distances.getOrDefault(minDistanceAsString, 0.0) + 1);
                        amountDeclarations++;
                        totalDistance += minDistance;
                        if (minDistance >= this.minDistance) {
                            amountLongDeclarations++;
                        }
                        if (!get_examples.isEmpty()) {
                            Integer minDistanceInt = (int) minDistance;
                            if (get_examples.containsKey(minDistanceInt) && !get_examples.get(minDistanceInt).isEmpty()) {
                                List<Integer> distance_example = get_examples.get((int) minDistance);
                                if (distance_example.get(0) > 0) {
                                    // Reduce it by 1
                                    Integer val = distance_example.get(0) - 1;
                                    distance_example.set(0, val);
                                } else if (distance_example.get(1) > 0) {
                                    // Add it to the examples map with the distance as the key and the line numbers as the value
                                    List<Integer> old_values = examples.getOrDefault(minDistanceInt, new ArrayList<>());
                                    old_values.addAll(Arrays.asList((int) chosenDeclarationLine, usageLine.intValue()));
                                    examples.put((int) minDistance, old_values);
                                    // Reduce it by 1
                                    Integer val = distance_example.get(1) - 1;
                                    distance_example.set(1, val);
                                }
                                get_examples.put(minDistanceInt, distance_example);
                            }
                        }
                    }
                }
            }
            // Search double declarations so a declaration after another declaration with no usage in between
            for (Map.Entry<String, List<Double>> entry : declarations.entrySet()) {
                String variableName = entry.getKey();
                List<Double> declarationLines = entry.getValue();
                List<Double> usageLines = usages.getOrDefault(variableName, new ArrayList<>());
                for (int i = 0; i < declarationLines.size() - 1; i++) {
                    Double declarationFirst = declarationLines.get(i);
                    Double declarationSecond = declarationLines.get(i + 1);
                    if (isDeclarationAfterDeclaration(declarationFirst, declarationSecond, usageLines)) {
                        double distance = declarationSecond - declarationFirst;
                        if (distance >= this.minDistance) {
                            String distanceAsString = String.valueOf(distance);
                            doubleDeclarations.put(distanceAsString, doubleDeclarations.getOrDefault(distanceAsString, 0.0) + 1);
                            amountDoubleDeclarations++;
                            totalDoubleDistance += distance;
                            if (!get_examples.isEmpty()) {
                                Integer key = (int) distance;
                                if (get_examples.containsKey(key) && !get_examples.get(key).isEmpty()) {
                                    List<Integer> distance_example = get_examples.get(key);
                                    if (distance_example.get(2) > 0) {
                                        // Reduce it by 1
                                        Integer val = distance_example.get(2) - 1;
                                        distance_example.set(2, val);
                                    } else if (distance_example.get(3) > 0) {
                                        // Add it to the examples map with the distance as the key and the line numbers as the value
                                        List<Integer> old_values = double_declaration_examples.getOrDefault(key, new ArrayList<>());
                                        old_values.addAll(Arrays.asList(declarationFirst.intValue(), declarationSecond.intValue()));
                                        double_declaration_examples.put(key, old_values);
                                        // Reduce it by 1
                                        Integer val = distance_example.get(3) - 1;
                                        distance_example.set(3, val);
                                    }
                                    get_examples.put(key, distance_example);
                                }
                            }
                        }
                    }
                }
            }
        }



        if (amountDeclarations == 0) {
            return new DistanceResults();
        }
        if (get_examples.isEmpty()) {
            averageDistance = totalDistance / amountDeclarations;
            return new DistanceResults(amountDeclarations, amountLongDeclarations, totalDistance, averageDistance, distances, amountDoubleDeclarations, totalDoubleDistance, averageDoubleDistance, doubleDeclarations);
        } else {
            return new DistanceResults(examples, double_declaration_examples);
        }
    }



    private boolean isDeclarationAfterDeclaration(Double declarationFirst, Double declarationSecond, List<Double> usageLines) {
        for (Double usageLine : usageLines) {
            if (declarationFirst < usageLine && usageLine < declarationSecond) {
                return false;
            }
        }
        return true;
    }

    private void searchBody(Map<String, List<Double>> usages, Statement statement) {
        // For all statements where the variable is used on the right side of an assignment or as a parameter, add it to usages
        if (statement.isExpressionStmt()) {
            ExpressionStmt expressionStmt = statement.asExpressionStmt();
            // Check if the expression is a reassignment, i.e. b = a;
            List<AssignExpr> assignExprs = expressionStmt.findAll(AssignExpr.class);
            // Check if the expression is an assignment, i.e. int b = a;
            List<VariableDeclarator> varDeclarators = expressionStmt.findAll(VariableDeclarator.class);
            // Check if the expression is a method call, i.e. fun(a);
            List<MethodCallExpr> methodCallExprs = expressionStmt.findAll(MethodCallExpr.class);
            if (!assignExprs.isEmpty()) {
                assignExprs.forEach(assignExpr -> {
                    assignExpr.getValue().findAll(NameExpr.class).forEach(nameExpr -> {
                        addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                    });
                });
            }
            if (!varDeclarators.isEmpty()) {
                varDeclarators.forEach(varDeclarator -> {
                    varDeclarator.getInitializer().ifPresent(initializer -> {
                        initializer.findAll(NameExpr.class).forEach(nameExpr -> {
                            addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                        });
                    });
                });
            }
            if (!methodCallExprs.isEmpty()) {
                methodCallExprs.forEach(methodCallExpr -> {
                    methodCallExpr.getArguments().forEach(argument -> {
                        argument.findAll(NameExpr.class).forEach(nameExpr -> {
                            addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                        });
                    });
                });
            }
        } else if (statement.isBlockStmt()) {
            statement.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
        } else if (statement.isForStmt()) {
            ForStmt forStmt = statement.asForStmt();
            Statement body = forStmt.getBody();
            if (body.isBlockStmt()) {
                body.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
            } else {
                searchBody(usages, body);
            }
            NodeList<Expression> initialization = forStmt.getInitialization();
            initialization.forEach(expression -> {
                expression.findAll(AssignExpr.class).forEach(assignExpr -> {
                    assignExpr.getValue().findAll(NameExpr.class).forEach(nameExpr -> {
                        addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                    });
                });
            });
            NodeList<Expression> update = forStmt.getUpdate();
            update.forEach(expression -> {
                expression.findAll(AssignExpr.class).forEach(assignExpr -> {
                    assignExpr.getValue().findAll(NameExpr.class).forEach(nameExpr -> {
                        addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                    });
                });
            });
            Expression compare = forStmt.getCompare().orElse(null);
            if (compare != null) {
                compare.findAll(NameExpr.class).forEach(nameExpr -> {
                    addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                });
            }

        } else if (statement.isIfStmt()) {
            IfStmt ifStmt = statement.asIfStmt();
            Statement thenStmt = ifStmt.getThenStmt();
            Statement elseStmt = ifStmt.getElseStmt().orElse(null);
            Expression condition = ifStmt.getCondition();
            condition.findAll(NameExpr.class).forEach(nameExpr -> {
                addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
            });
            if (thenStmt.isBlockStmt()) {
                thenStmt.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
            } else {
                searchBody(usages, thenStmt);
            }
            if (elseStmt != null) {
                if (elseStmt.isBlockStmt()) {
                    elseStmt.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
                } else {
                    searchBody(usages, elseStmt);
                }
            }
        } else if (statement.isReturnStmt()) {
            ReturnStmt returnStmt = statement.asReturnStmt();
            returnStmt.getExpression().ifPresent(expression -> {
                expression.findAll(NameExpr.class).forEach(nameExpr -> {
                    addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
                });
            });
        } else if (statement.isSwitchStmt()) {
            SwitchStmt switchStmt = statement.asSwitchStmt();
            switchStmt.getEntries().forEach(entry -> entry.getStatements().forEach(s -> searchBody(usages, s)));
            switchStmt.getSelector().findAll(NameExpr.class).forEach(nameExpr -> {
                addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
            });
        } else if (statement.isWhileStmt()) {
            WhileStmt whileStmt = statement.asWhileStmt();
            Statement body = whileStmt.getBody();
            if (body.isBlockStmt()) {
                body.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
            } else {
                searchBody(usages, body);
            }
        } else if (statement.isTryStmt()) {
            TryStmt tryStmt = statement.asTryStmt();
            tryStmt.getTryBlock().getStatements().forEach(s -> searchBody(usages, s));
            tryStmt.getCatchClauses().forEach(catchClause -> catchClause.getBody().getStatements().forEach(s -> searchBody(usages, s)));
            tryStmt.getFinallyBlock().ifPresent(finallyBlock -> finallyBlock.getStatements().forEach(s -> searchBody(usages, s)));
        } else if (statement.isDoStmt()) {
            DoStmt doStmt = statement.asDoStmt();
            Statement body = doStmt.getBody();
            if (body.isBlockStmt()) {
                body.asBlockStmt().getStatements().forEach(s -> searchBody(usages, s));
            } else {
                searchBody(usages, body);
            }
            doStmt.getCondition().findAll(NameExpr.class).forEach(nameExpr -> {
                addToMap(usages, nameExpr.getNameAsString(), (double) nameExpr.getBegin().get().line);
            });
        }
    }

    void addToMap(Map<String, List<Double>> map, String key, Double value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, new ArrayList<>(Collections.singletonList(value)));
        }
    }

    public String removeEmptyLinesAndComments(String code) {
        // Remove single-line comments
        code = code.replaceAll("//.*", "");

        // Remove multi-line comments
        Pattern pattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(code);
        code = matcher.replaceAll("");

        // Remove empty lines
        code = code.replaceAll("(?m)^[ \t]*\r?\n", "");

        return code;
    }

}


