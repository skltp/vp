/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalrouter.producer;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.wsaddressing10.AttributedURIType;

import se.skltp.domain.subdomain.getproducdetail.v1.GetProductDetailResponse;
import se.skltp.domain.subdomain.getproducdetail.v1.GetProductDetailType;
import se.skltp.domain.subdomain.getproducdetail.v1.Product;
import se.skltp.domain.subdomain.getproducdetail.v1.rivtabp20.GetProductDetailResponderInterface;

@WebService(
		serviceName = "GetProductDetailResponderService", 
		endpointInterface="se.skltp.domain.subdomain.getproducdetail.v1.rivtabp20.GetProductDetailResponderInterface", 
		portName = "GetProductDetailResponderPort", 
		targetNamespace = "urn:riv:domain:subdomain:GetProductDetail:1:rivtabp20",
		wsdlLocation = "schemas/interactions/GetProductDetailInteraction/GetProductDetailInteraction_1.0_RIVTABP20.wsdl")
public class VpFullServiceTestProducer implements GetProductDetailResponderInterface {
	private final Logger logger = LoggerFactory.getLogger(getClass());
    private Map<String, Product> productMap = new HashMap<>();

    public VpFullServiceTestProducer() {
        // Load some products
        Product product = newProduct("SW123", "Square Widget", 10, 10);
        productMap.put(product.getId(), product);
        product = newProduct("RW456", "Round Widget", 5, 5);
        productMap.put(product.getId(), product);
        logger.debug("WS initiated. Use getProductDetail?productId=SW123");
    }

    public GetProductDetailResponse getProductDetail( AttributedURIType logicalAddress, GetProductDetailType parameters) {
		String headerInfo = "NULL HEADER";
		if (logicalAddress != null) {
			headerInfo = "[" + logicalAddress.getValue() + "]";
		}
		logger.info("Producer-teststub. Start getProductDetail(), Header: " + headerInfo + ", Search args: ProductId = " + parameters.getProductId());
    	
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
        
        if(product != null){
            logger.info("Producer-teststub. End getProductDetail(), Returned product data: " + product.getId() + " - " + product.getDescription() + " - " + product.getHeight() + " - " + product.getWidth());
        }else{
            logger.info("Producer-teststub. End getProductDetail(), Returned null, no product found using {}", parameters.getProductId());
        }

       
        return response;
    }

    private Product newProduct(String id, String description, int width, int height) {
        Product p = new Product();
        p.setId(id);
        p.setDescription(description);
        p.setWidth(width);
        p.setHeight(height);
		return p;
	}
}
