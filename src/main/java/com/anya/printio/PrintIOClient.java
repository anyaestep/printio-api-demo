package com.anya.printio;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PrintIOClient {
  private static final String EP_PRODUCTTEMPLATE = "producttemplate";
  private static final String EP_PRODUCTS = "products";
  private static final String EP_PRODUCTVARIANTS = "productvariants";
  private String uri;
  private String recipeId;
  
  public PrintIOClient(String uri, String recipeId) {
    this.uri = uri;
    this.recipeId = recipeId;
  }
  
  /**
   * Retrieve template information and calculate product dimentions 
   * @param sku
   * @return string representing dimentions
   * @throws PrintServiceException
   * TODO: error handling when json key isn't found
   */
  public long[] getSkuDimentions(String sku) throws PrintServiceException {
    try {
      JSONArray templateOptions = (JSONArray) get(getEndPoint(EP_PRODUCTTEMPLATE).setParameter("sku", sku).build()).get("Options");
      JSONObject dimObject = findByObjectValue("Name", "Single", templateOptions);
      long width, height;
      if (!dimObject.containsKey("FinalX1")) {
        //TODO: Options has Spaces and then Layers - I don't have info how to pick a space sp picking the first one
        dimObject = findByObjectValue("Type", "Image", (JSONArray)((JSONObject)((JSONArray)dimObject.get("Spaces")).get(0)).get("Layers"));
        width = (long)dimObject.get("X2") - (long)dimObject.get("X1");
        height = (long)dimObject.get("Y2") - (long)dimObject.get("Y1");
      } else {
        width = (long)dimObject.get("FinalX2") - (long)dimObject.get("FinalX1");
        height = (long)dimObject.get("FinalY2") - (long)dimObject.get("FinalY1");
      }
      return new long[]{width, height};
    } catch (URISyntaxException e) {
      throw new PrintServiceException(e);
    }
  }
  
  private JSONObject findByObjectValue(String name, Object value, JSONArray array) throws PrintServiceException {
    JSONObject obj;
    if (array  == null) throw new PrintServiceException("Unexpected response from print service");
    for (int i = 0; i < array.size(); i ++) {
      obj = (JSONObject) array.get(i);
      if (value.equals(obj.get(name))) return obj;
    }
    return null;
  }
  
  /**
   * Retrieve a product to skus mapping. 
   * @return
   * @throws PrintServiceException
   * TODO: make a better cache object
   */
  public Map<String, List<String>> getProductSkus() throws PrintServiceException {
    Map<String, List<String>> productToSku = new HashMap<String, List<String>>();
    JSONObject productsDoc;
    try {
      productsDoc = get(getEndPoint(EP_PRODUCTS).build());
      JSONArray products = (JSONArray) productsDoc.get("Products");
      int length = products.size();
      JSONObject product;
      JSONArray productVariants;
      Long productId;
      List<String> skus;
      for (int i = 0; i < length; i ++) {
        product = (JSONObject) products.get(i);
        productId = (Long) product.get("Id");
        skus = new ArrayList<String>();
        productVariants = (JSONArray)getProductVariant(productId).get("ProductVariants");
        for (int j = 0; j < productVariants.size(); j ++)
          skus.add((String)((JSONObject)productVariants.get(j)).get("Sku"));
        productToSku.put((String)product.get("Name"), skus);
      }
      return productToSku;
    } catch (URISyntaxException e) {
      throw new PrintServiceException(e);
    } 
  }
  
  private JSONObject getProductVariant(Long productId) throws PrintServiceException {
    try {
      return get(getEndPoint(EP_PRODUCTVARIANTS).setParameter("productId", Long.toString(productId)).build());
    } catch (URISyntaxException e) {
      throw new PrintServiceException(e);
    }
  }
  
  private URIBuilder getEndPoint(String endpoint) throws URISyntaxException {
    return new URIBuilder(this.uri + "/" + endpoint).setParameter("recipeId", this.recipeId).setParameter("countryCode", "US");
  }
  
  private JSONObject get(URI endpoint) throws PrintServiceException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    try {
      HttpGet httpget = new HttpGet(endpoint);
      ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
        @Override
        public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
          if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
              return EntityUtils.toString(entity);
            }
          }
          // TODO: add better exceptions
          throw new ClientProtocolException("Error has occurred");
        }
      };
      return (JSONObject)new JSONParser().parse(httpClient.execute(httpget, responseHandler));
    } catch (ParseException | IOException e) {
      throw new PrintServiceException("Unable to get content from endpoint" + endpoint, e);
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
