/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalrouter.producer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.wsaddressing10.AttributedURIType;

import se.skl.tjanst1.wsdl.GetProductDetailResponse;
import se.skl.tjanst1.wsdl.GetProductDetailType;
import se.skl.tjanst1.wsdl.ListProducts;
import se.skl.tjanst1.wsdl.ListProductsResponse;
import se.skl.tjanst1.wsdl.Product;
import se.skl.tjanst1.wsdl.Tjanst1Interface;

@WebService(serviceName = "Tjanst1Service"
	, portName = "Tjanst1ImplPort"
	, targetNamespace = "urn:skl:tjanst1:rivtabp20"
	, name = "Tjanst1")
public class VpFullServiceTestProducer implements Tjanst1Interface {
	private final Logger logger = LoggerFactory.getLogger(getClass());
    private Map<String, Product> productMap = new HashMap<String, Product>();

    public VpFullServiceTestProducer() {
        // Load some products
        Product product = newProduct("SW123", "Square Widget", 10, 10);
        productMap.put(product.getId(), product);
        product = newProduct("RW456", "Round Widget", 5, 5);
        productMap.put(product.getId(), product);
        logger.debug("WS initiated. Use /listProducts and /getProductDetail?productId=SW123");
    }

	public ListProductsResponse listProducts(AttributedURIType logicalAddress, ListProducts parameters) {
    	logger.info("Start");

    	ListProductsResponse response = new ListProductsResponse();
    	List<String> productListing = response.getItem();
    	fillProductList(productListing);

    	logger.info("End");
    	
        return response;
    }

    public GetProductDetailResponse getProductDetail( AttributedURIType logicalAddress, GetProductDetailType parameters) {
		String headerInfo = "NULL HEADER";
		if (logicalAddress != null) {
			headerInfo = "[" + logicalAddress.getValue() + "]";
		}
		log("Start", "Header: " + headerInfo + ", Search args: ProductId = " + parameters.getProductId());
    	
		if ( parameters.getProductId().equals("Exception")){
			throw new RuntimeException("PP01 Product Does Not Exist");
		} else if (parameters.getProductId().equals("Timeout")) {
			try {
				Thread.sleep(35000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
        Product product = productMap.get(parameters.getProductId());
        
        GetProductDetailResponse response = new GetProductDetailResponse();
        response.setProduct(product);

        log("End", "Returned product data: " + product.getId() + " - " + product.getDescription() + " - " + product.getHeight() + " - " + product.getWidth());
        return response;
    }

	private void fillProductList(List<String> productListing) {
        Collection<Product> products = productMap.values();
        for (Product p : products) {
            productListing.add(p.getId() + " - " + p.getDescription());
        }
	}

    private Product newProduct(String id, String description, int width, int height) {
        Product p = new Product();
        p.setId(id);
        p.setDescription(description);
        p.setWidth(width);
        p.setHeight(height);
		return p;
	}
    

    private void log(String event, String data) {
//    	logger.info(event + ": " + data);
    	System.out.println(event + ": " + data);
    }
}
