import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main {

    static double totalDeclarations = 0;
    static double longDistanceDeclarations = 0;
    static double totalDistance = 0;
    static double averageDistance = 0;
    static double amountDoubleDeclarations = 0;
    static double totalDoubleDistance = 0;
    static double averageDoubleDistance = 0;

    static Map<String, List<String>> examples = new HashMap<>();
    static Map<String, List<String>> double_declaration_examples = new HashMap<>();
    static Map<Integer, List<Integer>> get_examples = new HashMap<>(0);

    static int distance = 1;

    static Map<String, Double> distances = new HashMap<>();
    static Map<String, Double> doubleDeclarations = new HashMap<>();

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
        amountDoubleDeclarations += values.amountDoubleDeclarations;
        totalDoubleDistance += values.totalDoubleDistance;
        averageDoubleDistance += values.averageDoubleDistance;
        for (Map.Entry<String, Double> entry : values.distances.entrySet()) {
            if (distances.containsKey(entry.getKey())) {
                distances.put(entry.getKey(), distances.get(entry.getKey()) + entry.getValue());
            } else {
                distances.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Double> entry : values.doubleDeclarations.entrySet()) {
            if (doubleDeclarations.containsKey(entry.getKey())) {
                doubleDeclarations.put(entry.getKey(), doubleDeclarations.get(entry.getKey()) + entry.getValue());
            } else {
                doubleDeclarations.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void findExample(String fileName, String fileContent, String path) {
        // Check if all entries of get_examples are 0, if yes stop
        boolean all_zero = true;
        for (Map.Entry<Integer, List<Integer>> entry : get_examples.entrySet()) {
            List<Integer> valuesListEntry = entry.getValue();
            for (Integer integer : valuesListEntry) {
                if (integer > 0) {
                    all_zero = false;
                    break;
                }
            }
        }
        if (all_zero) {
            return;
        }

        VariableUsageDistance calculator = new VariableUsageDistance(fileContent, distance, false);
        DistanceResults values = calculator.calculateDistance(get_examples);
        for (Map.Entry<Integer, List<String>> entry : values.examples.entrySet()) {
            List<String> valuesListEntry = entry.getValue();
            for (int i = 0; i < valuesListEntry.size()-1; i = i + 3) {
                String lineBegin = valuesListEntry.get(i);
                String lineEnd = valuesListEntry.get(i+1);
                String entry_name = path + " Distance: " + entry.getKey() + " Line: " + lineBegin + " to " + lineEnd;
                // Get the code from the fileContent from line i to lineBegin to lineEnd
                StringBuilder code = new StringBuilder();
                String[] lines = fileContent.split("\n");
                // Show the previous 5 lines and the next 5 lines, if they exist
                int lineBeginInt = Integer.parseInt(lineBegin);
                int lineEndInt = Integer.parseInt(lineEnd);
                int showCodeBegin = lineBeginInt - 5;
                if (showCodeBegin < 0) {
                    showCodeBegin = 0;
                }
                int showCodeEnd = lineEndInt + 5;
                if (showCodeEnd > lines.length) {
                    showCodeEnd = lines.length;
                }
                for (int j = showCodeBegin-1; j < showCodeEnd; j++) {
                    if (j == lineBeginInt-1) {
                        code.append(">>> var: ").append(valuesListEntry.get(i + 2)).append("\n").append(lines[j]).append("\n");
                    } else if (j == lineEndInt) {
                        code.append("<<< var: ").append(valuesListEntry.get(i + 2)).append("\n").append(lines[j]).append("\n");
                    } else
                        code.append(lines[j]).append("\n");
                }
                examples.put(entry_name, List.of(code.toString().split("\n")));
            }
        }
        for (Map.Entry<Integer, List<String>> entry : values.double_declaration_examples.entrySet()) {
            List<String> valuesListEntry = entry.getValue();
            for (int i = 0; i < valuesListEntry.size()-1; i = i + 3) {
                String lineBegin = valuesListEntry.get(i);
                String lineEnd = valuesListEntry.get(i+1);
                int lineBeginInt = Integer.parseInt(lineBegin);
                int lineEndInt = Integer.parseInt(lineEnd);
                String entry_name = path + " Distance: " + entry.getKey() + " Line: " + lineBegin + " to " + lineEnd;
                // Get the code from the fileContent from line i to lineBegin to lineEnd
                StringBuilder code = new StringBuilder();
                String[] lines = fileContent.split("\n");
                // Show the previous 5 lines and the next 5 lines, if they exist
                int showCodeBegin = lineBeginInt - 5;
                if (showCodeBegin < 0) {
                    showCodeBegin = 0;
                }
                int showCodeEnd = lineEndInt + 5;
                if (showCodeEnd > lines.length) {
                    showCodeEnd = lines.length;
                }
                for (int j = showCodeBegin-1; j < showCodeEnd; j++) {
                    if (j == lineBeginInt-1) {
                        code.append(">>> var: ").append(valuesListEntry.get(i + 2)).append("\n").append(lines[j]).append("\n");
                    } else if (j == lineEndInt) {
                        code.append("<<< var: ").append(valuesListEntry.get(i + 2)).append("\n").append(lines[j]).append("\n");
                    } else
                        code.append(lines[j]).append("\n");
                }
                double_declaration_examples.put(entry_name, List.of(code.toString().split("\n")));
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

    private static void findExamples(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                // Recursively analyze files in subdirectories
                findExamples(file);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String fileContent = readFileContent(file);
                    String path = file.getAbsolutePath().substring(26);
                    findExample(file.getName(), fileContent, path);
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

        int max_length = 10; // repositories.length()

        if (Objects.equals(args[0], "ex")) {
            localDirectory = args[1];
            // Parse the json file of args[2] and add the examples to get_examples
            String jsonFilePath = args[2]; // Replace with your JSON file path
            Object jsonFile = readJsonFromFile(jsonFilePath);
            // Convert the linked hash map to a Map of lists
            Map<String, Object> jsonObject;
            if (jsonFile instanceof Map) {
                jsonObject = (Map<String, Object>) jsonFile;
            } else {
                jsonObject = new HashMap<>();
            }
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                List<Integer> examplesListEntry = new ArrayList<>();
                Integer key = Integer.parseInt(entry.getKey());
                Map<String, Integer> values = (Map<String, Integer>) entry.getValue();
                for (Map.Entry<String, Integer> value : values.entrySet()) {
                    examplesListEntry.add(value.getValue());
                }
                get_examples.put(key, examplesListEntry);
            }

            File folder = new File(localDirectory);
            if (folder.exists() && folder.isDirectory()) {
                findExamples(folder);
                Map<String, Object> all_examples = new HashMap<>();
                all_examples.put("examples", examples);
                all_examples.put("double_declaration_examples", double_declaration_examples);
                writeJsonToFile("examples.json", all_examples);

            }
            return;
        } else if (args.length > 1 && args[1] != null && args[2] != null) {
            distance = Integer.parseInt(args[0]);
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
        results.put("amountDoubleDeclarations", amountDoubleDeclarations);
        results.put("totalDoubleDeclarationsDistance", totalDoubleDistance);
        results.put("averageDoubleDeclarationsDistance", totalDoubleDistance / amountDoubleDeclarations);

        resultsMap.put("values", results);
        // Sort the distances map by length which is a string of the length
        Map<String, Double> sortedDistances = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedDistances.putAll(distances);
        Map<String, Double> sortedDoubleDeclarationDistances = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedDoubleDeclarationDistances.putAll(doubleDeclarations);

        resultsMap.put("distances", sortedDistances);
        resultsMap.put("doubleDeclarations", sortedDoubleDeclarationDistances);
        jsonObject.put(name, resultsMap);

        writeJsonToFile(jsonFilePath, jsonObject);


        System.out.println("Analyzed repository: "+ name + ", Total Declarations: "
            + totalDeclarations + ", Total Double Declarations: " + amountDoubleDeclarations);







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
