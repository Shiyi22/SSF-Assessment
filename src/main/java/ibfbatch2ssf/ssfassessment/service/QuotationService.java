package ibfbatch2ssf.ssfassessment.service;

import java.util.LinkedList;
import java.util.List;
import java.io.StringReader;

import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ibfbatch2ssf.ssfassessment.model.Cart;
import ibfbatch2ssf.ssfassessment.model.Item;
import ibfbatch2ssf.ssfassessment.model.Quotation;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

@Service
public class QuotationService {

    public String url = "https://quotation.chuklee.com"; 

    // method to get quotation
    public Quotation getQuotations(List<String> items) throws Exception {

        Quotation quote = new Quotation(); 
        
        // convert items to JSON array 
        JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
        for (int i = 0; i < items.size(); i++) {
            arrBuilder.add(items.get(i)); 
        }
        JsonArray arr = arrBuilder.build(); 
        System.out.println("JsonArray: " + arr);

        // Make HTTP Call to get quotation (POSTING Json Data)
        RequestEntity<String> req = RequestEntity.post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .body(arr.toString(), String.class); 

        RestTemplate template = new RestTemplate(); 
        ResponseEntity<String> resp = null;
        String payload = ""; 
        Integer statusCode = 0; 

        try {
            resp = template.exchange(req, String.class); 
            payload = resp.getBody(); 
            statusCode = resp.getStatusCode().value(); 
        } catch (HttpClientErrorException ex) {
            payload = ex.getResponseBodyAsString();
            statusCode= ex.getStatusCode().value(); 
            return quote; // return empty Quotation
        } finally {
            System.out.printf(">>> Status Code: %d\n", statusCode);
            System.out.printf(">>> Payload: %s\n", payload);
        }

        // Proceed: 
        // convert json string to JsonObject 
        JsonReader reader = Json.createReader(new StringReader(payload)); 
        JsonObject json = reader.readObject(); 

        // convert JsonObject into Quotation and return 
        quote.setQuoteId(json.getString("quoteId"));
        JsonObject json2 = json.getJsonObject("quotations"); 

        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i); 
            quote.addQuotation(item, Float.parseFloat(json2.getString(item)));
        }

        return quote; 
    }

    // method to get list of items from customer cart 
    public List<String> getList(Cart cart) {

        List<Item> contents = cart.getContents(); 
        List<String> items = new LinkedList<>(); 

        for (int i = 0; i < contents.size(); i++) {
            items.add(contents.get(i).getItemName()); 
        }
        return items; 
    }

    // method to calculate total cost 
    public Float calculateCost(Quotation quote, Cart cart) {

        List<Item> contents = cart.getContents(); 
        Float cost = 0.0f; 

        for (int i = 0; i < contents.size(); i++) {
            // match cart item with quotation list 
            Float unitPxOfItem = quote.getQuotations().get(contents.get(i).getItemName());
            Integer quantity = contents.get(i).getQuantity(); 
            cost += (unitPxOfItem * quantity); 
        }
        return cost; 
    }
    
}
