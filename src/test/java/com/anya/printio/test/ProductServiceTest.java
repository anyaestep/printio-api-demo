package com.anya.printio.test;

import static org.mockito.Mockito.when;

import java.util.*;

import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.anya.printio.PrintIOClient;
import com.anya.printio.ProductService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ProductServiceTest {

  @Mock
  private PrintIOClient pclient;
  private ProductService service;
  
  @Before
  public void setUp() throws Exception {
    service = new ProductService(pclient);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodSkuDimentions() throws Exception {
    when(pclient.getSkuDimentions("sku")).thenReturn(new long[]{10, 20});
    JSONObject sku = new JSONObject();
    sku.put("sku", "10x20");
    Response r = Response.status(200).entity(JSONValue.toJSONString(sku)).build();
    assertThat(service.getSkuImageSize("sku").getEntity(), is(r.getEntity()));
  }
  
  @Test
  @SuppressWarnings("unchecked")
  public void testNonExistingSkuDimentions() throws Exception {
    when(pclient.getSkuDimentions("sku")).thenReturn(new long[]{10, 20});
    JSONObject sku = new JSONObject();
    sku.put("notSku", "0x0");
    // TODO: must be 400
    Response r = Response.status(200).entity(JSONValue.toJSONString(sku)).build();
    assertThat(service.getSkuImageSize("notSku").getEntity(), is(JSONValue.toJSONString(sku)));
  }
  
  @Test
  public void testVariousErrorSKuDimention() {
    // try PrintServieException scenarios
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testGoodProductMap() throws Exception {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    map.put("x", Arrays.asList("a", "b0"));
    map.put("x1", Arrays.asList("b", "b1"));
    map.put("x2", Arrays.asList("c", "b3"));
    when(pclient.getProductSkus()).thenReturn(map);
    when(pclient.getSkuDimentions("a")).thenReturn(new long[]{1, 20});
    when(pclient.getSkuDimentions("b")).thenReturn(new long[]{2, 20});
    when(pclient.getSkuDimentions("c")).thenReturn(new long[]{3, 20});
    when(pclient.getSkuDimentions("b0")).thenReturn(new long[]{10, 200});
    when(pclient.getSkuDimentions("b1")).thenReturn(new long[]{10, 30});
    when(pclient.getSkuDimentions("b3")).thenReturn(new long[]{10, 40});
    
    List<JSONObject> list = new ArrayList<JSONObject>();
    JSONObject response = new JSONObject();
    response.put("a", "1x20");
    list.add(response);
    response = new JSONObject();
    response.put("b0", "10x200");
    list.add(response);
    
    Response r = Response.status(200).entity(JSONValue.toJSONString(list)).build();
    assertThat(service.getProductImageSizes("x").getEntity(), is(r.getEntity()));
    // more tests
  }
  
  @Test
  public void testBadProductProductMap() throws Exception {
    
  }

}
