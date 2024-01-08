import java.util.HashMap;
import java.util.Map;

public class DistanceResults {
    double amountDeclarations = 0.0;
    double amountLongDeclarations = 0.0;
    double totalDistance = 0.0;
    double averageDistance = 0.0;
    
    Map<String, Double> distances = new HashMap<>();

    public DistanceResults() {}

    public DistanceResults(double amountDeclarations, double amountLongDeclarations, double totalDistance, double averageDistance, Map<String, Double> distances) {
        this.amountDeclarations = amountDeclarations;
        this.amountLongDeclarations = amountLongDeclarations;
        this.totalDistance = totalDistance;
        this.averageDistance = averageDistance;
        this.distances = distances;
    }
}
