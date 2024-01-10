package headspin.io.hsapi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.IOException;

import static headspin.io.hsapi.GlobalVar.*;
import static headspin.io.hsapi.PropertyFileReader.getConfigProperty;
public class HsApi {
    String url_root = "https://api-dev.headspin.io/v0/";
    Map<String,String> headers = new HashMap<>();
    public HsApi() {
        Access_token = getConfigProperty("Access_token");
        user_flow_id = getConfigProperty("user_flow_id");
        image_path = getConfigProperty("image_path");
        audio_path = getConfigProperty("audio_path");
        text_match = getConfigProperty("text_match");
        img_threshold = Float.parseFloat(getConfigProperty("img_threshold"));
        image_label_name = getConfigProperty("image_label_name");
        date = getConfigProperty("date");
        ocr_video_box = parseOrcVideoBox(getConfigProperty("ocr_video_box"));
        String ocrThresholdString = getConfigProperty("img_scaling_factor");
        String[] ocrThresholdValues = ocrThresholdString.split(",");
        for (String value : ocrThresholdValues) {
            img_scaling_factor.add(Float.parseFloat(value.trim()));
        }
        ocr_threshold =Integer.parseInt(getConfigProperty("ocr_threshold"));
        headers.put("Authorization", "Bearer " + Access_token);

        System.out.println("access_token: "+Access_token);
        System.out.println("user_flow_id: "+user_flow_id);
        System.out.println("image_path: "+image_path);
        System.out.println("audio_path: "+audio_path);
        System.out.println("text_match: "+text_match);
        System.out.println("img_threshold: "+img_threshold);
        System.out.println("img_scaling_factor: "+img_scaling_factor);
        System.out.println("ocr_threshold: "+ocr_threshold);
        System.out.println("date: "+date);
        System.out.println("date: "+ocr_video_box);
    }
    private static List<List<Integer>> parseOrcVideoBox(String propertyValue) {
        List<List<Integer>> result = new ArrayList<>();

        // Split the property value into individual arrays
        String[] arrayStrings = propertyValue.split("],");

        for (String arrayString : arrayStrings) {
            // Remove leading and trailing brackets
            arrayString = arrayString.replace("[", "").replace("]", "");

            // Split the values within each array
            String[] values = arrayString.split(",");

            // Parse values as integers and add to the result list
            List<Integer> innerList = new ArrayList<>();
            for (String value : values) {
                innerList.add(Integer.parseInt(value.trim()));
            }

            result.add(innerList);
        }

        return result;
    }
    public void get_session_ids() {
        String request_url = url_root + "userflows/" + user_flow_id + "/sessions";
        try {
            URL url = new URL(request_url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    // Extract session IDs based on a specified date// Specify the target date
                    List<String> sessionIds = getSessionIdsByDate(response.toString(), date);

                    // Use the sessionIds as needed
                    System.out.println("Session IDs for " + date + ": " + sessionIds);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private List<String> getSessionIdsByDate(String jsonResponse, String targetDate) {

        JSONObject jsonObject = new JSONObject(jsonResponse);

        JSONArray sessionsArray = jsonObject.getJSONArray("sessions");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < sessionsArray.length(); i++) {
            JSONObject session = sessionsArray.getJSONObject(i);
            String eventTime = session.getString("event_time").split(" ")[0];
            String status = session.getString("status");

            try {
                Date sessionDate = dateFormat.parse(eventTime);
                if (dateFormat.format(sessionDate).equals(targetDate)) {
                    // If the dates match, extract the session_id and add it to the list
                    if (status.equals("Passed")) {  // || status.equals("Failed")
                        String sessionId = session.getString("session_id");
                        session_ids.add(sessionId);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return session_ids;
    }

    public Map<String, Object> get_capture_timestamp(String Session_id) {
        String requestUrl = url_root + "sessions/" + Session_id + "/timestamps";
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            int responseCode = connection.getResponseCode();
            String responseBody = connection.getResponseMessage();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
                Map<String, Object> responseMap = objectMapper.readValue(connection.getInputStream(), typeRef);
                return responseMap;
            } else {
                connection.disconnect();
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /*--------------------------------------------------image matching--------------------------------------------*/
    public void upload_image() {
        String requestUrl = url_root + "sessions/analysis/queryimage/upload";
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            File imageFile = new File(image_path);
            FileInputStream fileInputStream = new FileInputStream(imageFile);
            OutputStream outputStream = connection.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            outputStream.close();
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code for image_id: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    image_id = jsonResponse.getString("image_id");
                    System.out.println("Image ID: " + image_id);
                }
                connection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Image_Analysis(String session_id) {
        long durationInSeconds = 600;
        long endTimeMillis = System.currentTimeMillis() + (durationInSeconds * 1000);
        Double start = 0.0;
        Double end = 0.0;
        while (System.currentTimeMillis() < endTimeMillis) {
            Map<String, Object> capture_timestamp = get_capture_timestamp(session_id);
            if (capture_timestamp.containsKey("capture-complete")) {
                Double video_start_timestamp = (Double) capture_timestamp.get("capture-started");
                Double video_end_timestamp = (Double) capture_timestamp.get("capture-ended");
                start = (Double) 0.0;
                end = (Double)  video_end_timestamp-video_start_timestamp;
//                System.out.println("capture_timestamp: "+capture_timestamp);
                System.out.println("session_id:"+session_id+"     Start: "+""+start +"   endtime: "+end);
                break;
            }
        }
        if (end!=0.0) {
            try {
            String apiUrl = "https://api-dev.headspin.io/v0/sessions/" + session_id + "/label/add";
            Map<String, Object> jsonDataMap = new HashMap<>();
            jsonDataMap.put("label_type", "image-match-request");
            jsonDataMap.put("name", image_label_name);
            jsonDataMap.put("start_time", start);
            jsonDataMap.put("end_time", end);
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("method", "template");
            dataMap.put("image_id", image_id);
            dataMap.put("threshold", img_threshold);
            dataMap.put("use_multiscale", true);
            dataMap.put("scaling_factors", img_scaling_factor);
            jsonDataMap.put("data", dataMap);
            String jsonData = new Gson().toJson(jsonDataMap);
            System.out.println(jsonData);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonData.getBytes("UTF-8"));
            }
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response: " + response.toString());
            }
            connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.exit(0);
        }

    }

    /*-------------------------------------------------- audio matching --------------------------------------------*/
    public String upload_audio() {
        String audioId = null;
        try {
            String apiUrl = "https://api-dev.headspin.io/v0/audio/upload";
            URL url = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestProperty("Content-Type", "audio/wav");
            connection.setDoOutput(true);
            File file = new File(audio_path);
            byte[] fileBytes = new byte[(int) file.length()];
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                fileInputStream.read(fileBytes);
            }
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(fileBytes);
            }
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                System.out.println("Response: " + response.toString());
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                audioId = jsonNode.get("audio_id").asText();
            }
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return audioId;
    }
    public void audio_analysis(String session_id,String audio_id_ref) {
        long durationInSeconds = 600;
        long endTimeMillis = System.currentTimeMillis() + (durationInSeconds * 1000);
        Double start = 0.0;
        Double end = 0.0;
        while (System.currentTimeMillis() < endTimeMillis) {
            Map<String, Object> capture_timestamp = get_capture_timestamp(session_id);
            if (capture_timestamp.containsKey("capture-complete")) {
                Double video_start_timestamp = (Double) capture_timestamp.get("capture-started");
                Double video_end_timestamp = (Double) capture_timestamp.get("capture-ended");
                start = (Double) 0.0;
                end = (Double)  video_end_timestamp-video_start_timestamp;
//                System.out.println("capture_timestamp: "+capture_timestamp);
                System.out.println("session_id:"+session_id+"     Start: "+""+start +"   endtime: "+end);
                break;
            }
        }
        if (end!=0.0) {
            try {
                String apiUrl = "https://api-dev.headspin.io/v0/sessions/" + session_id + "/label/add";
                Map<String, Object> jsonDataMap = new HashMap<>();

                List<Map<String, Object>> labelsList = new ArrayList<>();
                Map<String, Object> labelMap = new HashMap<>();
                labelMap.put("name", "audio_analysis");
                labelMap.put("label_type", "audio-match-request");
                labelMap.put("start_time", start);
                labelMap.put("end_time", end);
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("ref_audio_media_id", audio_id_ref);
                Map<String, Object> matchStatusThresholds = new HashMap<>();
                Map<String, Object> fullMap = new HashMap<>();
                fullMap.put("match_score", 0.8);
                fullMap.put("prominence_ratio", 1.0);
                Map<String, Object> partialMap = new HashMap<>();
                partialMap.put("match_score", 0.5);
                partialMap.put("prominence_ratio", 1.3);
                matchStatusThresholds.put("full", fullMap);
                matchStatusThresholds.put("partial", partialMap);
                dataMap.put("match_status_thresholds", matchStatusThresholds);
                labelMap.put("data", dataMap);
                labelsList.add(labelMap);
                jsonDataMap.put("labels", labelsList);
                String jsonData = new Gson().toJson(jsonDataMap);
                System.out.println(jsonData);
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonData.getBytes("UTF-8"));
                }
                int responseCode = connection.getResponseCode();
                System.out.println("Response Code: " + responseCode);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("Response: " + response.toString());
                }
                connection.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    /*-------------------------------------------------- text matching --------------------------------------------*/
    public Map<String,Float> get_labels_time(String session_id,String label_type,String category) {
            try {
                // Construct the API URL
                String apiUrl = "https://api-dev.headspin.io/v0/sessions/" + session_id + "/label/list";
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                connection.setRequestProperty("Content-Type", "application/json");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
                    Map<String, Object> responseMap = objectMapper.readValue(connection.getInputStream(), typeRef);
                    List<Map<String, Object>> labels = (List<Map<String, Object>>) responseMap.get("labels");
                    Map<String,Float> label_time = new HashMap<>();
                    for (Map<String, Object> label : labels) {
                        String labelCategory = (String) label.get("category");
                        String labelType = (String) label.get("label_type");

                        if (labelType.equals(label_type) && labelCategory.equals(category)) {
                            Integer startTime = (Integer) label.get("start_time");
                            Integer endTime = (Integer) label.get("end_time");
                            float startTimeInSeconds = startTime != null ? startTime / 1000.0f : 0.0f;
                            float endTimeInSeconds = endTime != null ? endTime / 1000.0f : 0.0f;

                            label_time.put("start", startTimeInSeconds);
                            label_time.put("end", endTimeInSeconds);
                            break;
                        }
                    }
                    return label_time;
                } else {
                    connection.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
    }
    public void text_analysis(String session_id,String label_type,String category){
        Map<String,Float> label_time = new HashMap<>();
        long durationInSeconds = 600;
        long endTimeMillis = System.currentTimeMillis() + (durationInSeconds * 1000);
        while (System.currentTimeMillis() < endTimeMillis) {
            label_time = get_labels_time(session_id,label_type,category);
            if (label_time.containsKey("start")) {
                break;
            }
        }
        System.out.println(label_time);
        float start = (label_time.get("start")+label_time.get("end"))/2;
        double en = start + 0.1;
        float end = (float) en;
        try {
            String apiUrl = "https://api-dev.headspin.io/v0/sessions/"+session_id+"/label/add";
            Map<String, Object> jsonDataMap = new HashMap<>();
            jsonDataMap.put("label_type", "ocr-request");
            jsonDataMap.put("name", "text_match");
            jsonDataMap.put("start_time", start);
            jsonDataMap.put("end_time", end);
            jsonDataMap.put("video_box",ocr_video_box);
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("ocr_config", "--psm 3");
            dataMap.put("confidence_threshold",ocr_threshold);
            dataMap.put( "landscape",  false);
            dataMap.put("target_height", null);
            jsonDataMap.put("data", dataMap);
            String jsonData = new Gson().toJson(jsonDataMap);
            System.out.println(jsonData);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonData.getBytes("UTF-8"));
            }
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response: " + response.toString());
            }
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

