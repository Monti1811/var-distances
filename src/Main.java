import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Main {

    static double totalDeclarations = 0;
    static double longDistanceDeclarations = 0;
    static double totalDistance = 0;
    static double averageDistance = 0;

    static int distance = 1;

    static Map<String, Double> distances = new HashMap<>();

    private static final String GITHUB_API_URL = "https://api.github.com/graphql";

    public static String getGitHubToken() {
        // Replace 'your_github_token' with your actual GitHub personal access token
        return "your_github_token";
    }

    public static String searchRepositories(String query, String token) throws Exception {
        String url = GITHUB_API_URL + "/search/repositories?query=" + query + "&type=REPOSITORY";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Authorization", "token " + token);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    public static String getRepositoryContent(String owner, String repo, String path, String token) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Authorization", "token " + token);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }


    public static void analyzeCode(String repositoryOwner, String repositoryName, String filePath, String githubToken) throws Exception {
        analyzeFolder(repositoryOwner, repositoryName, filePath, githubToken);
    }

    private static void analyzeFolder(String repositoryOwner, String repositoryName, String folderPath, String githubToken) throws Exception {
        String content;
        try {
            content = getRepositoryContent(repositoryOwner, repositoryName, folderPath, githubToken);
        } catch (Exception e) {
            System.out.println("Error at folder " + folderPath + " with message: " + e.getMessage());
            return;
        }

        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject fileInfo = jsonArray.getJSONObject(i);
            if ("file".equals(fileInfo.getString("type")) && fileInfo.getString("name").endsWith(".java")) {
                String fileContent = getFileContent(fileInfo.getString("download_url"));
                analyzeFile(fileInfo.getString("name"), fileContent);
            } else if ("dir".equals(fileInfo.getString("type"))) {
                String subFolderPath = folderPath + fileInfo.getString("name") + "/";
                analyzeFolder(repositoryOwner, repositoryName, subFolderPath, githubToken);
            }
        }
    }

    private static String getFileContent(String downloadUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        return content.toString();
    }

    private static void analyzeFile(String fileName, String fileContent) {
        // Your code analysis logic here
        VariableUsageDistance calculator = new VariableUsageDistance(fileContent, distance);
        DistanceResults values = calculator.calculateDistance();
        totalDeclarations += values.amountDeclarations;
        longDistanceDeclarations += values.amountLongDeclarations;
        totalDistance += values.totalDistance;
        averageDistance += values.averageDistance;
        for (Map.Entry<String, Double> entry : values.distances.entrySet()) {
            if (distances.containsKey(entry.getKey())) {
                distances.put(entry.getKey(), distances.get(entry.getKey()) + entry.getValue());
            } else {
                distances.put(entry.getKey(), entry.getValue());
            }
        }
    }


    // This method is used to analyze local files
    private static void analyzeFiles(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                // Recursively analyze files in subdirectories
                analyzeFiles(file);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String fileContent = readFileContent(file);
                    analyzeFile(file.getName(), fileContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));

        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        reader.close();
        return content.toString();
    }


    public static void main(String[] args) throws Exception {
        String query = "language:java";  // You can adjust the query for other languages
        String githubToken = getGitHubToken();
        String localDirectory = null;
        String name = "no_name";

        distance = Integer.parseInt(args[0]);
        int max_length = 10; // repositories.length()

        if (args.length > 1 && args[1] != null && args[2] != null) {
            localDirectory = args[1];
            name = args[2];
            File folder = new File(localDirectory);
            if (folder.exists() && folder.isDirectory()) {
                analyzeFiles(folder);
            }
        } else {

            /*
            JSONArray repositories = new JSONArray(searchRepositories(query, githubToken));


            for (int i = 0; i < max_length; i++) {
                JSONObject repo = repositories.getJSONObject(i);
                analyzeCode(repo.getJSONObject("owner").getString("login"),
                        repo.getString("name"),
                        "/",
                        githubToken);
            }
            */

            analyzeCode("elastic",
                    "elasticsearch",
                    "/",
                    githubToken);
        }


        String jsonFilePath = "results.json"; // Replace with your JSON file path
        Object jsonFile = readJsonFromFile(jsonFilePath);
        Map<String, Object> jsonObject;
        if (jsonFile instanceof Map) {
            jsonObject = (Map<String, Object>) jsonFile;
        } else {
            jsonObject = new HashMap<>();
        }


        Map<String, Map<String, Double>> resultsMap = new HashMap<>();

        Map<String, Double> results = new HashMap<>();
        results.put("totalDeclarations", totalDeclarations);
        results.put("longDistanceDeclarations", longDistanceDeclarations);
        results.put("totalDistance", totalDistance);
        results.put("averageDistance", totalDistance / totalDeclarations);

        resultsMap.put("values", results);
        // Sort the distances map by length which is a string of the length
        Map<String, Double> sortedDistances = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedDistances.putAll(distances);

        resultsMap.put("distances", sortedDistances);
        jsonObject.put(name, resultsMap);

        writeJsonToFile(jsonFilePath, jsonObject);


        System.out.println("Analyzed repository: "+ name + ", Total Declarations: "
            + totalDeclarations + ", Declarations that had their first reference after " + distance
            + ": " + longDistanceDeclarations);







    }

    static Object readJsonFromFile(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new File(filePath), Object.class);
        } catch (IOException e) {
            // Handle file reading errors
            return null;
        }
    }

    static void writeJsonToFile(String filePath, Object data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new java.io.File(filePath), data);
        } catch (IOException e) {
            // Handle file writing errors
            e.printStackTrace();
        }
    }
}
