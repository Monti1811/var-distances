import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistanceResults {
    double amountDeclarations = 0.0;
    double amountLongDeclarations = 0.0;
    double totalDistance = 0.0;
    double averageDistance = 0.0;
    double amountDoubleDeclarations = 0.0;
    double totalDoubleDistance = 0.0;
    double averageDoubleDistance = 0.0;

    Map<Integer, List<String>> examples = new HashMap<>();
    Map<Integer, List<String>> double_declaration_examples = new HashMap<>();

    
    Map<String, Double> distances = new HashMap<>();
    Map<String, Double> doubleDeclarations = new HashMap<>();

    public DistanceResults() {}

    public DistanceResults(double amountDeclarations, double amountLongDeclarations, double totalDistance, double averageDistance, Map<String, Double> distances) {
        this.amountDeclarations = amountDeclarations;
        this.amountLongDeclarations = amountLongDeclarations;
        this.totalDistance = totalDistance;
        this.averageDistance = averageDistance;
        this.distances = distances;
    }

    public DistanceResults(double amountDeclarations, double amountLongDeclarations, double totalDistance, double averageDistance, Map<String, Double> distances, double amountDoubleDeclarations, double totalDoubleDistance, double averageDoubleDistance, Map<String, Double> doubleDeclarations) {
        this.amountDeclarations = amountDeclarations;
        this.amountLongDeclarations = amountLongDeclarations;
        this.totalDistance = totalDistance;
        this.averageDistance = averageDistance;
        this.distances = distances;
        this.amountDoubleDeclarations = amountDoubleDeclarations;
        this.totalDoubleDistance = totalDoubleDistance;
        this.averageDoubleDistance = averageDoubleDistance;
        this.doubleDeclarations = doubleDeclarations;
    }

    public DistanceResults(Map<Integer, List<String>> examples, Map<Integer, List<String>> double_declaration_examples) {
        this.examples = examples;
        this.double_declaration_examples = double_declaration_examples;
    }
}
