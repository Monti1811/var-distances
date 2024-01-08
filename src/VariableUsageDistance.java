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

public class VariableUsageDistance {

    private final String code;
    private int minDistance = 5;

    public VariableUsageDistance(String code) {
        this.code = code;
    }

    public VariableUsageDistance(String code, int minDistance) {
        this.code = code;
        this.minDistance = minDistance;
    }

    public DistanceResults calculateDistance() {

        double amountDeclarations = 0.0;
        double amountLongDeclarations = 0.0;
        double totalDistance = 0.0;
        double averageDistance = 0.0;
        Map<String, Double> distances = new HashMap<>();
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
                    for (Double declarationLine : declarationLines) {
                        double distance = usageLine - declarationLine;
                        if (distance > 0 && distance < minDistance) {
                            minDistance = distance;
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
                    }
                }
            }

        }

        if (amountDeclarations == 0) {
            return new DistanceResults();
        }
        averageDistance = totalDistance / amountDeclarations;

        return new DistanceResults(amountDeclarations, amountLongDeclarations, totalDistance, averageDistance, distances);
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

}


