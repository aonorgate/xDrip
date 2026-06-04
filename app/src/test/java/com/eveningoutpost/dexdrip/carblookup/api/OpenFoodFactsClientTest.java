package com.eveningoutpost.dexdrip.carblookup.api;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

public class OpenFoodFactsClientTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void fetchByBarcode_success_callsOnSuccess() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"status\":1,\"product\":{\"product_name\":\"Weetabix\",\"brands\":\"Weetabix Ltd\",\"nutriments\":{\"carbohydrates_100g\":68.4}}}"));

        CallbackRecorder recorder = new CallbackRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.fetchByBarcode("5000169105306", recorder);
        awaitCallback(recorder);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().contains("5000169105306.json"));
        assertNotNull(recorder.product);
        assertEquals("Weetabix", recorder.product.productName);
        assertNull(recorder.notFoundBarcode);
        assertNull(recorder.errorMessage);
    }

    @Test
    public void fetchByBarcode_statusZero_callsOnNotFound() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":0,\"code\":\"123\"}"));

        CallbackRecorder recorder = new CallbackRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.fetchByBarcode("123", recorder);
        awaitCallback(recorder);

        assertEquals("123", recorder.notFoundBarcode);
        assertNull(recorder.product);
        assertNull(recorder.errorMessage);
    }

    @Test
    public void fetchByBarcode_httpError_callsOnError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("server error"));

        CallbackRecorder recorder = new CallbackRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.fetchByBarcode("123", recorder);
        awaitCallback(recorder);

        assertTrue(recorder.errorMessage.contains("500"));
        assertNull(recorder.product);
        assertNull(recorder.notFoundBarcode);
    }

    @Test
    public void fetchByBarcode_malformedJson_callsOnError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{not-json}"));

        CallbackRecorder recorder = new CallbackRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.fetchByBarcode("123", recorder);
        awaitCallback(recorder);

        assertEquals("Failed to parse product data", recorder.errorMessage);
        assertNull(recorder.product);
        assertNull(recorder.notFoundBarcode);
    }

    @Test
    public void searchProducts_success_usesSearchEndpointAndReturnsProducts() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"count\":1,\"products\":[{\"code\":\"1234567890123\",\"product_name\":\"Rice pudding\",\"brands\":\"Kitchen Co\",\"nutriments\":{\"carbohydrates_100g\":18.5}}]}"));

        SearchRecorder recorder = new SearchRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.searchProducts("  rice   pudding  ", 12, recorder);
        awaitCallback(recorder);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("/cgi/search.pl", request.getRequestUrl().encodedPath());
        assertEquals("rice pudding", request.getRequestUrl().queryParameter("search_terms"));
        assertEquals("12", request.getRequestUrl().queryParameter("page_size"));
        assertNotNull(recorder.products);
        assertEquals(1, recorder.products.size());
        assertEquals("1234567890123", recorder.products.get(0).code);
        assertEquals("Rice pudding", recorder.products.get(0).productName);
        assertNull(recorder.errorMessage);
    }

    @Test
    public void searchProducts_transient503RetriesAndReturnsProducts() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("busy"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"count\":1,\"products\":[{\"code\":\"1234567890123\",\"product_name\":\"Rice pudding\"}]}"));

        SearchRecorder recorder = new SearchRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString(), 0L);

        client.searchProducts("rice", 12, recorder);
        awaitCallback(recorder);

        RecordedRequest firstRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest retryRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(firstRequest);
        assertNotNull(retryRequest);
        assertEquals("rice", firstRequest.getRequestUrl().queryParameter("search_terms"));
        assertEquals("rice", retryRequest.getRequestUrl().queryParameter("search_terms"));
        assertNotNull(recorder.products);
        assertEquals(1, recorder.products.size());
        assertEquals("Rice pudding", recorder.products.get(0).productName);
        assertNull(recorder.errorMessage);
    }

    @Test
    public void searchProducts_emptyQueryReturnsEmptyListWithoutRequest() throws Exception {
        SearchRecorder recorder = new SearchRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.searchProducts(" ", 12, recorder);
        awaitCallback(recorder);

        assertNotNull(recorder.products);
        assertEquals(0, recorder.products.size());
        assertNull(recorder.errorMessage);
        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void searchProducts_filtersProductsWithoutBarcodeOrName() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "{\"count\":4,\"products\":["
                        + "{\"code\":\"\",\"product_name\":\"Missing barcode\"},"
                        + "{\"code\":\"111\",\"product_name\":\"\"},"
                        + "{\"code\":\"222\",\"product_name_en\":\"English name\"},"
                        + "{\"code\":\"333\",\"product_name\":\"Local name\"}]}"));

        SearchRecorder recorder = new SearchRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.searchProducts("name", 12, recorder);
        awaitCallback(recorder);

        assertNotNull(recorder.products);
        assertEquals(2, recorder.products.size());
        assertEquals("222", recorder.products.get(0).code);
        assertEquals("English name", recorder.products.get(0).productNameEn);
        assertEquals("333", recorder.products.get(1).code);
        assertNull(recorder.errorMessage);
    }

    @Test
    public void searchProducts_malformedJson_callsOnError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{not-json}"));

        SearchRecorder recorder = new SearchRecorder();
        OpenFoodFactsClient client = new OpenFoodFactsClient(new OkHttpClient(), server.url("/api/v2/product/").toString());

        client.searchProducts("rice", 12, recorder);
        awaitCallback(recorder);

        assertEquals("Failed to parse search data", recorder.errorMessage);
        assertNull(recorder.products);
    }

    private void awaitCallback(CallbackRecorder recorder) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (recorder.latch.getCount() > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            recorder.latch.await(50, TimeUnit.MILLISECONDS);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, recorder.latch.getCount());
    }

    private void awaitCallback(SearchRecorder recorder) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (recorder.latch.getCount() > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            recorder.latch.await(50, TimeUnit.MILLISECONDS);
        }
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, recorder.latch.getCount());
    }

    private static class CallbackRecorder implements OpenFoodFactsClient.ProductCallback {
        final CountDownLatch latch = new CountDownLatch(1);
        ProductData.Product product;
        String notFoundBarcode;
        String errorMessage;

        @Override
        public void onSuccess(ProductData.Product product) {
            this.product = product;
            latch.countDown();
        }

        @Override
        public void onNotFound(String barcode) {
            this.notFoundBarcode = barcode;
            latch.countDown();
        }

        @Override
        public void onError(String errorMessage) {
            this.errorMessage = errorMessage;
            latch.countDown();
        }
    }

    private static class SearchRecorder implements OpenFoodFactsClient.SearchCallback {
        final CountDownLatch latch = new CountDownLatch(1);
        List<ProductData.Product> products;
        String errorMessage;

        @Override
        public void onSuccess(List<ProductData.Product> products) {
            this.products = products;
            latch.countDown();
        }

        @Override
        public void onError(String errorMessage) {
            this.errorMessage = errorMessage;
            latch.countDown();
        }
    }
}