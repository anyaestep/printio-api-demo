package com.anya.printio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


@Path("/product")
public class ProductService {
  // TODO: make a better cache object
  private static Map<String, List<String>> productToSkuMapping = null;
  private PrintIOClient pclient;
  

  public ProductService() {
    Properties p = new Properties();
    try {
      p.load(getClass().getResourceAsStream("/properties.ini"));
    } catch (IOException e) {
      // TODO Handle it!!!
      e.printStackTrace();
    }
    this.pclient = new PrintIOClient(p.getProperty("printio.url"), p.getProperty("printio.recipeId"));
  }
  
  public ProductService(PrintIOClient pclient) {
    this.pclient = pclient;
  }
  
  @Path("skuimagesize")
  @GET
  @Produces("application/json")
  public Response getSkuImageSize(@QueryParam("sku") String sku) {
    return Response.status(200).entity(JSONValue.toJSONString(getSkuDimentionObject(sku))).build();
  }
  
  @Path("productimagesizes")
  @GET
  @Produces("application/json")
  // TODO: better exceptions
  public Response getProductImageSizes(@QueryParam("name") String name) {
    try {
      List<String> skus = getProductSku(name);
      if (skus == null) return Response.status(404).build();
      List<JSONObject> res = new ArrayList<JSONObject>();
      for (String sku: skus) {
        res.add(getSkuDimentionObject(sku));
      }
      return Response.status(200).entity(JSONValue.toJSONString(res)).build();
    } catch (PrintServiceException e) { // TODO: make sure it's 404 when not found
      return Response.status(500).build();
    }
    
  }
  
  @SuppressWarnings("unchecked")
  private JSONObject getSkuDimentionObject(String sku) {
    JSONObject obj = new JSONObject(); 
    long [] size = null;
    try {
      size = pclient.getSkuDimentions(sku);
      
    } catch (PrintServiceException e) {
      // TODO: what to do in this case???
    } 
    if (size == null) size = new long[]{0, 0}; // TODO: make sure it's 404 when not found 
    obj.put(sku, size[0] + "x" + size[1]);
    return obj; 
  }
  
  /**
   * Get skus for a product by product name. The cache will be lazy loaded if needed.
   * @param name
   * @return
   * TODO: better synch
   * @throws PrintServiceException 
   */
  private synchronized List<String> getProductSku(String name) throws PrintServiceException {
    if (productToSkuMapping == null) {
      productToSkuMapping = Collections.synchronizedMap(pclient.getProductSkus());
    }
    return productToSkuMapping.get(name);
  }
  
}
