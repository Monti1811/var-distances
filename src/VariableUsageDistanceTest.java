import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableUsageDistanceTest {

    @Test
    void calculateDistanceEmptyLines() {
        String code =
        """
        public class Test {
            public void test() {
                int a = 1;
                int b = 2;
                
                
                
                
                b = a;
            };
            void fun(int a, int b) {};
        };
        """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();
        assertEquals(1, values.distances.size());
        assertEquals(1.0, values.distances.get("2.0"));
    }

    @Test
    void calculateDistanceCommentEmptyLines() {
        String code =
                """
                 public class Test {
                     public void test() {
                         int a = 1;
                         int b/*one line test */ = 2;
 
                         // test
 
 
                         if (a == 1)/*
                             
                             double test
                             
                             */
                             b = b + 1;
                          else
                             a = a + 1;
                          
                         
                     };
                     void fun(int a, int b) {};
                 };
                 """;

        String trimmedCode =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        if (a == 1)
                            b = b + 1;
                         else
                            a = a + 1;
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();
        assertEquals(2, values.distances.size());
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(1.0, values.distances.get("5.0"));
    }


    @Test
    void calculateDistance() {
        String code =
        """
        public class Test {
            public void test() {
                int a = 1;
                int b = 2;
                a = 3;
                b = a + 4;
                int c = a + b;
                a = a + b;
                fun(a, b);
            };
            void fun(int a, int b) {};
        };
        """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(3, values.distances.size());
        assertEquals(3.0, values.distances.get("1.0"));
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(2.0, values.distances.get("3.0"));


    }

    @Test
    void calculateDoubleDeclarations() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(b, b);
                        a = 1;
                        a = 2;
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(3, values.distances.size());
        assertEquals(2.0, values.distances.get("1.0"));
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(3.0, values.distances.get("3.0"));

        assertEquals(1.0, values.doubleDeclarations.get("1.0"));
        assertEquals(3.0, values.doubleDeclarations.get("2.0"));


    }

    @Test
    void calculateDistanceIf() {
        DistanceResults values = getDistanceResults();


        assertEquals(4, values.distances.size());
        assertEquals(3.0, values.distances.get("1.0"));
        assertEquals(3.0, values.distances.get("2.0"));
        assertEquals(2.0, values.distances.get("3.0"));
        assertEquals(2.0, values.distances.get("5.0"));

        String code2 =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        if (a == 1)
                            b = b + 1;
                         else
                            a = a + 1;
                        
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator2 = new VariableUsageDistance(code2, 1);
        DistanceResults values2 = calculator2.calculateDistance();


        assertEquals(4, values2.distances.size());
        assertEquals(3.0, values2.distances.get("1.0"));
        assertEquals(3.0, values2.distances.get("2.0"));
        assertEquals(2.0, values2.distances.get("3.0"));
        assertEquals(2.0, values2.distances.get("5.0"));


    }

    private static DistanceResults getDistanceResults() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        if (a == 1) {
                            int d = b + 1;
                        } else {
                            int g = a + 1;
                        }
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        return calculator.calculateDistance();
    }

    @Test
    void calculateDistanceWhile() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        while (a == 1) {
                            int d = b + 1;
                        }
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(4, values.distances.size());
        assertEquals(3.0, values.distances.get("1.0"));
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(2.0, values.distances.get("3.0"));
        assertEquals(1.0, values.distances.get("5.0"));

        String code2 =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        while (a == 1)
                            b = b + 1;
                        
                        
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator2 = new VariableUsageDistance(code2, 1);
        DistanceResults values2 = calculator2.calculateDistance();


        assertEquals(4, values2.distances.size());
        assertEquals(3.0, values2.distances.get("1.0"));
        assertEquals(2.0, values2.distances.get("2.0"));
        assertEquals(2.0, values2.distances.get("3.0"));
        assertEquals(1.0, values2.distances.get("5.0"));


    }

    @Test
    void calculateDistanceDo() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        do {
                            int d = b + 1;
                        } while (a == 1);
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(5, values.distances.size());
        assertEquals(3.0, values.distances.get("1.0"));
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(2.0, values.distances.get("3.0"));
        assertEquals(1.0, values.distances.get("4.0"));
        assertEquals(1.0, values.distances.get("5.0"));

        String code2 =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        do
                            b = b + 1;
                        while (a == 1);
                        
                        
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator2 = new VariableUsageDistance(code2, 1);
        DistanceResults values2 = calculator2.calculateDistance();


        assertEquals(5, values2.distances.size());
        assertEquals(3.0, values2.distances.get("1.0"));
        assertEquals(2.0, values2.distances.get("2.0"));
        assertEquals(2.0, values2.distances.get("3.0"));
        assertEquals(1.0, values2.distances.get("4.0"));
        assertEquals(1.0, values2.distances.get("5.0"));


    }

    @Test
    void calculateDistanceBlock() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        a = 3;
                        b = a + 4;
                        int c = a + b;
                        a = a + b;
                        fun(a, b);
                        {
                            int d = b + 1;
                        }
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(4, values.distances.size());
        assertEquals(3.0, values.distances.get("1.0"));
        assertEquals(2.0, values.distances.get("2.0"));
        assertEquals(2.0, values.distances.get("3.0"));
        assertEquals(1.0, values.distances.get("5.0"));

    }

    @Test
    void calculateDistanceFor() {
        String code =
                """
                public class Test {
                    public void test() {
                        int b = 2;
                        for (int c = 1; c < 10; c = b + 1) {
                            b = b + 1;
                        }
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(2, values.distances.size());
        assertEquals(1.0, values.distances.get("1.0"));
        assertEquals(1.0, values.distances.get("2.0"));
        String code2 =
                """
                public class Test {
                    public void test() {
                        int b = 2;
                        for (int c = 1; c < 10; c = b + 1)
                            b = b + 1;
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator2 = new VariableUsageDistance(code2, 1);
        DistanceResults values2 = calculator2.calculateDistance();


        assertEquals(2, values2.distances.size());
        assertEquals(1.0, values2.distances.get("1.0"));
        assertEquals(1.0, values2.distances.get("2.0"));


    }

    @Test
    void calculateDistanceReturn() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        return a + b;
                    };
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(2, values.distances.size());
        assertEquals(1.0, values.distances.get("1.0"));
        assertEquals(1.0, values.distances.get("2.0"));

    }

    @Test
    void calculateDistanceSwitch() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        int c = 0;
                        switch (a) {
                            case 1:
                                c = b + 1;
                                break;
                            case 2:
                                c = b + 2;
                                break;
                            default:
                                c = b + 3;
                                break;
                        }
                    };
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(4, values.distances.size());
        assertEquals(1.0, values.distances.get("3.0"));
        assertEquals(1.0, values.distances.get("4.0"));
        assertEquals(1.0, values.distances.get("7.0"));
        assertEquals(1.0, values.distances.get("10.0"));

    }

    @Test
    void calculateDistanceTry() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        int c = 0;
                        try {
                            c = b + 1;
                        } catch (Exception e) {
                            c = b + 1;
                        } finally {
                            c = b + 2;
                        }
                    };
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        DistanceResults values = calculator.calculateDistance();


        assertEquals(3, values.distances.size());
        assertEquals(1.0, values.distances.get("3.0"));
        assertEquals(1.0, values.distances.get("5.0"));
        assertEquals(1.0, values.distances.get("7.0"));

    }



    @Test
    void sortMap() {
        Map<String, Double> map = new TreeMap<>();
        map.put("9.0", 3.0);
        map.put("22.0", 2.0);
        map.put("8.0", 2.0);
        map.put("4.0", 2.0);

        Map<String, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedMap.putAll(map);

        // Use gson to write the map to a json file
        try (FileWriter writer = new FileWriter("test_json.json")) {
            // Create Gson instance with pretty printing
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // Convert the sorted map to JSON and write to a file
            gson.toJson(map, writer);

            System.out.println("JSON written to test_json.json");
        } catch (IOException e) {
            e.printStackTrace();
        }


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("distances", sortedMap);
        //Main.writeJsonToFile("test_json.json", jsonObject);

        // Check if the map is sorted in the json file



    }

    @Test
    void sortMap2() throws IOException {
        Map<String, Double> map = new TreeMap<>();
        map.put("9.0", 3.0);
        map.put("22.0", 2.0);
        map.put("8.0", 2.0);
        map.put("4.0", 2.0);

        Map<String, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedMap.putAll(map);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Map<String, Double>> resultsMap = new HashMap<>();
        Map<String, Double> results = new HashMap<>();
        resultsMap.put("values", results);
        // Sort the distances map by length which is a string of the length
        Map<String, Double> sortedDistances = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedDistances.putAll(sortedMap);

        resultsMap.put("distances", sortedDistances);
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("elastic", resultsMap);

        // Write the sorted map to a JSON file
        objectMapper.writeValue(new java.io.File("test_json.json"), jsonMap);

        // Check if the map is sorted in the json file
        ObjectMapper objectMapper2 = new ObjectMapper();


        Map results2 = objectMapper2.readValue(new File("test_json.json"), Map.class);
        Map distances = (Map) results2.get("distances");





    }

    @Test
    void sortMap3() throws IOException {
        Map<String, Double> map = new TreeMap<>();
        map.put("9.0", 3.0);
        map.put("22.0", 2.0);
        map.put("8.0", 2.0);
        map.put("4.0", 2.0);

        Map<String, Double> sortedMap = new TreeMap<>(Comparator.comparingDouble(Double::parseDouble));
        sortedMap.putAll(map);

        // Iterate through the map and save the key and value as an array
        // This is needed because the map is not a valid json object
        // and we need to convert it to a json object
        String[][] sortedMapArray = new String[sortedMap.size()][2];
        int i = 0;
        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
            sortedMapArray[i][0] = entry.getKey();
            sortedMapArray[i][1] = String.valueOf(entry.getValue());
            i++;
        }


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("distances", sortedMapArray);
        Main.writeJsonToFile("test_json.json", jsonObject);



    }

    @Test
    void testRemovingLines() throws IOException {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b/*one line test */ = 2;

                        // test


                        if (a == 1)/*
                            
                            double test
                            
                            */
                            b = b + 1;
                         else
                            a = a + 1;
                         
                        
                    };
                    void fun(int a, int b) {};
                };
                """;

        String trimmedCode =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        if (a == 1)
                            b = b + 1;
                         else
                            a = a + 1;
                    };
                    void fun(int a, int b) {};
                };
                """;
        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        String newCode = calculator.removeEmptyLinesAndComments(code);

        assertEquals(trimmedCode, newCode);
    }

    @Test
    void testFindExamples() {
        String code =
                """
                public class Test {
                    public void test() {
                        int a = 1;
                        int b = 2;
                        if (a == 1)
                            b = b + 1;
                         else
                            a = a + 1;
                            
                        a = 1;
                        a = 5;
                        a = 2;
                        int c = 3;
                        c = a;
                    };
                    void fun(int a, int b) {};
                };
                """;

        VariableUsageDistance calculator = new VariableUsageDistance(code, 1);
        Map<Integer, List<Integer>> get_examples = new HashMap<>();
        get_examples.put(1, Arrays.asList(0, 0, 0, 1));
        get_examples.put(2, Arrays.asList(0, 1, 0, 0));
        DistanceResults values = calculator.calculateDistance(get_examples);
        Map<Integer, List<Integer>> examples = values.examples;

        assertEquals(1, examples.size());
        assertEquals(Arrays.asList(3,5), examples.get(2));

        assertEquals(1, values.double_declaration_examples.size());
        assertEquals(Arrays.asList(8,9), values.double_declaration_examples.get(1));

        Map<Integer, List<Integer>> get_examples2 = new HashMap<>();
        get_examples2.put(1, Arrays.asList(0, 0, 1, 1));
        get_examples2.put(2, Arrays.asList(1, 1, 0, 0));
        DistanceResults values2 = calculator.calculateDistance(get_examples2);

        assertEquals(1, values2.examples.size());
        assertEquals(Arrays.asList(11,13), values2.examples.get(2));

        assertEquals(1, values2.double_declaration_examples.size());
        assertEquals(Arrays.asList(9,10), values2.double_declaration_examples.get(1));

    }

    @Test
    void testFn() {
        String code = """
                public long readLong() throws IOException {
                        long res = 0L;
                        streamInput.reset();
                        final int reads = pagedBytes.length() / 8;
                        for (int i = 0; i < reads; i++) {
                            res = res ^ streamInput.readLong();
                        }
                        return res;
                    }
                """;
        VariableUsageDistance calculator = new VariableUsageDistance(code, 1, false);

        Map<Integer, List<Integer>> get_examples = new HashMap<>();
        get_examples.put(1, Arrays.asList(0, 1, 0, 0));
        DistanceResults values = calculator.calculateDistance(get_examples);

        assertEquals(0, values.examples.size());

    }


}