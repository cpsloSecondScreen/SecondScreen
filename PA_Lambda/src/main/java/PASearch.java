/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * ProductAdvertisingAPI
 * 
 * https://webservices.amazon.com/paapi5/documentation/index.html
 */

/*
 * This sample code snippet is for ProductAdvertisingAPI 5.0's SearchItems API
 * For more details, refer:
 * https://webservices.amazon.com/paapi5/documentation/search-items.html
 */

import java.util.ArrayList;
import java.util.List;

import com.amazon.paapi5.v1.ApiClient;
import com.amazon.paapi5.v1.ApiException;
import com.amazon.paapi5.v1.ErrorData;
import com.amazon.paapi5.v1.Item;
import com.amazon.paapi5.v1.PartnerType;
import com.amazon.paapi5.v1.SearchItemsRequest;
import com.amazon.paapi5.v1.SearchItemsResource;
import com.amazon.paapi5.v1.SearchItemsResponse;
import com.amazon.paapi5.v1.api.DefaultApi;

public class PASearch {

    public static void SearchByKeyword(String keyword) {
	ApiClient client = new ApiClient();

	// Add your credentials
	// Please add your access key here
	String accessKey = System.getenv("AccessKey");
	client.setAwsAccessKey(accessKey);
	// Please add your secret key here
	String secretKey = System.getenv("SecretKey");
	client.setAwsSecretKey(secretKey);
	
	// Enter your partner tag (store/tracking id)
	String partnerTag = System.getenv("AssociateTag");
	
	/*
	 * PAAPI Host and Region to which you want to send request. For more
	 * details refer:
	 * https://webservices.amazon.com/paapi5/documentation/common-request-
	 * parameters.html#host-and-region
	 */
	client.setHost("webservices.amazon.com");
	client.setRegion("us-east-1");

	DefaultApi api = new DefaultApi(client);

	// Request initialization

	/*
	 * Choose resources you want from SearchItemsResource enum For more
	 * details, refer:
	 * https://webservices.amazon.com/paapi5/documentation/search-items.html
	 * #resources-parameter
	 */
	List<SearchItemsResource> searchItemsResources = new ArrayList<SearchItemsResource>();
	searchItemsResources.add(SearchItemsResource.ITEMINFO_TITLE);
	searchItemsResources.add(SearchItemsResource.OFFERS_LISTINGS_PRICE);
v
	/*
	 * Specify the category in which search request is to be made
	 * For more details, refer:
	 * https://webservices.amazon.com/paapi5/documentation/use-cases/organization-of-items-on-amazon/search-index.html
	 */
	String searchIndex = "All";

	// Specify keywords
	String keywords = keyword;

	// Sending the request
	SearchItemsRequest searchItemsRequest = new SearchItemsRequest().partnerTag(partnerTag).keywords(keywords)
	    .searchIndex(searchIndex).resources(searchItemsResources).partnerType(PartnerType.ASSOCIATES);

	try {
	    // Forming the request
	    SearchItemsResponse response = api.searchItems(searchItemsRequest);

	    System.out.println("API called successfully");
	    System.out.println("Complete response: " + response);

	    // Parsing the request
	    if (response.getSearchResult() != null) {
		System.out.println("Printing first item information in SearchResult:");
		Item item = response.getSearchResult().getItems().get(0);
		if (item != null) {
		    if (item.getASIN() != null) {
			System.out.println("ASIN: " + item.getASIN());
		    }
		    if (item.getDetailPageURL() != null) {
			System.out.println("DetailPageURL: " + item.getDetailPageURL());
		    }
		    if (item.getItemInfo() != null && item.getItemInfo().getTitle() != null
			&& item.getItemInfo().getTitle().getDisplayValue() != null) {
			System.out.println("Title: " + item.getItemInfo().getTitle().getDisplayValue());
		    }
		    if (item.getOffers() != null && item.getOffers().getListings() != null
			&& item.getOffers().getListings().get(0).getPrice() != null
			&& item.getOffers().getListings().get(0).getPrice().getDisplayAmount() != null) {
			System.out.println(
					   "Buying price: " + item.getOffers().getListings().get(0).getPrice().getDisplayAmount());
		    }
		}
	    }
	    if (response.getErrors() != null) {
		System.out.println("Printing errors:\nPrinting Errors from list of Errors");
		for (ErrorData error : response.getErrors()) {
		    System.out.println("Error code: " + error.getCode());
		    System.out.println("Error message: " + error.getMessage());
		}
	    }
	} catch (ApiException exception) {
	    // Exception handling
	    System.out.println("Error calling PA-API 5.0!");
	    System.out.println("Status code: " + exception.getCode());
	    System.out.println("Errors: " + exception.getResponseBody());
	    System.out.println("Message: " + exception.getMessage());
	    if (exception.getResponseHeaders() != null) {
		// Printing request reference
		System.out.println("Request ID: " + exception.getResponseHeaders().get("x-amzn-RequestId"));
	    }
	    // exception.printStackTrace();
	} catch (Exception exception) {
	    System.out.println("Exception message: " + exception.getMessage());
	    // exception.printStackTrace();
	}
    }
}
