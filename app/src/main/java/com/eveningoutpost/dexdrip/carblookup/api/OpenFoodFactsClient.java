package com.eveningoutpost.dexdrip.carblookup.api;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eveningoutpost.dexdrip.BuildConfig;

public class OpenFoodFactsClient {
    private static final String BASE_URL = "https://world.openfoodfacts.org/api/v2/product/";
    private static final int MAX_SEARCH_ATTEMPTS = 2;
    private static final long SEARCH_RETRY_DELAY_MS = 750L;
    private final OkHttpClient client;
    private final String baseUrl;
    private final String searchUrl;
    private final String userAgent;
    private final long searchRetryDelayMs;

    public OpenFoodFactsClient() {
        this(FoodDbSource.current());
    }

    public OpenFoodFactsClient(FoodDbSource source) {
        this(new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build(), source != null ? source.baseUrl() : BASE_URL);
    }

    OpenFoodFactsClient(OkHttpClient client, String baseUrl) {
        this(client, baseUrl, SEARCH_RETRY_DELAY_MS);
    }

    OpenFoodFactsClient(OkHttpClient client, String baseUrl, long searchRetryDelayMs) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.searchUrl = deriveSearchUrl(baseUrl);
        this.userAgent = "xDrip-CarbLookup/" + BuildConfig.VERSION_NAME + " (personal-use)";
        this.searchRetryDelayMs = searchRetryDelayMs;
    }

    public interface ProductCallback {
        void onSuccess(ProductData.Product product);
        void onNotFound(String barcode);
        void onError(String errorMessage);
    }

    public interface SearchCallback {
        void onSuccess(List<ProductData.Product> products);
        void onError(String errorMessage);
    }

    public void fetchByBarcode(String barcode, final ProductCallback callback) {
        String url = baseUrl + barcode + ".json?fields=product_name,brands,nutriments,serving_size,quantity";
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postToMain(() -> callback.onError("Failed to fetch product: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        postToMain(() -> callback.onError("Server responded with error: " + response.code()));
                        return;
                    }

                    if (response.body() == null) {
                        postToMain(() -> callback.onError("Empty response body"));
                        return;
                    }

                    String responseBody = response.body().string();
                    final ProductData productData;
                    try {
                        productData = new Gson().fromJson(responseBody, ProductData.class);
                    } catch (RuntimeException parseException) {
                        postToMain(() -> callback.onError("Failed to parse product data"));
                        return;
                    }

                    if (productData != null && productData.status == 1 && productData.product != null) {
                        postToMain(() -> callback.onSuccess(productData.product));
                    } else {
                        postToMain(() -> callback.onNotFound(barcode));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    public void searchProducts(String query, int pageSize, final SearchCallback callback) {
        String normalizedQuery = normalizeSearchQuery(query);
        if (normalizedQuery.isEmpty()) {
            postToMain(() -> callback.onSuccess(Collections.emptyList()));
            return;
        }

        HttpUrl parsedSearchUrl = HttpUrl.parse(searchUrl);
        if (parsedSearchUrl == null) {
            postToMain(() -> callback.onError("Invalid search URL"));
            return;
        }

        String url = parsedSearchUrl.newBuilder()
        .addQueryParameter("search_terms", normalizedQuery)
                .addQueryParameter("search_simple", "1")
                .addQueryParameter("action", "process")
                .addQueryParameter("json", "1")
                .addQueryParameter("page_size", String.valueOf(pageSize))
                .addQueryParameter("fields", "code,product_name,product_name_en,brands,nutriments,serving_size,quantity")
                .build()
                .toString();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();

        executeSearchRequest(request, callback, 1);
    }

    private void executeSearchRequest(Request request, final SearchCallback callback, int attempt) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (attempt < MAX_SEARCH_ATTEMPTS) {
                    retrySearch(request, callback, attempt + 1);
                    return;
                }
                postToMain(() -> callback.onError("Failed to search products: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        int responseCode = response.code();
                        if (isTransientSearchError(responseCode) && attempt < MAX_SEARCH_ATTEMPTS) {
                            retrySearch(request, callback, attempt + 1);
                            return;
                        }
                        postToMain(() -> callback.onError(searchHttpErrorMessage(responseCode)));
                        return;
                    }

                    if (response.body() == null) {
                        postToMain(() -> callback.onError("Empty response body"));
                        return;
                    }

                    String responseBody = response.body().string();
                    final ProductSearchData searchData;
                    try {
                        searchData = new Gson().fromJson(responseBody, ProductSearchData.class);
                    } catch (RuntimeException parseException) {
                        postToMain(() -> callback.onError("Failed to parse search data"));
                        return;
                    }

                    List<ProductData.Product> products = new ArrayList<>();
                    if (searchData != null && searchData.products != null) {
                        for (ProductData.Product product : searchData.products) {
                            if (hasSearchResultIdentity(product)) {
                                products.add(product);
                            }
                        }
                    }
                    postToMain(() -> callback.onSuccess(products));
                } finally {
                    response.close();
                }
            }
        });
    }

    private void retrySearch(Request request, SearchCallback callback, int nextAttempt) {
        postToMainDelayed(() -> executeSearchRequest(request, callback, nextAttempt), searchRetryDelayMs);
    }

    private boolean isTransientSearchError(int responseCode) {
        return responseCode == 429 || responseCode == 502 || responseCode == 503 || responseCode == 504;
    }

    private String searchHttpErrorMessage(int responseCode) {
        if (isTransientSearchError(responseCode)) {
            return "Open Food Facts is busy (" + responseCode + "). Please wait a moment and try again.";
        }
        return "Server responded with error: " + responseCode;
    }

    private boolean hasSearchResultIdentity(ProductData.Product product) {
        if (product == null || product.code == null || product.code.trim().isEmpty()) {
            return false;
        }
        return (product.productName != null && !product.productName.trim().isEmpty())
                || (product.productNameEn != null && !product.productNameEn.trim().isEmpty());
    }

    private static String deriveSearchUrl(String productBaseUrl) {
        String productPath = "/api/v2/product/";
        int productPathIndex = productBaseUrl != null ? productBaseUrl.indexOf(productPath) : -1;
        if (productPathIndex >= 0) {
            return productBaseUrl.substring(0, productPathIndex) + "/cgi/search.pl";
        }
        return productBaseUrl;
    }

    private static String normalizeSearchQuery(String query) {
        return query == null ? "" : query.trim().replaceAll("\\s+", " ");
    }

    private void postToMain(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    private void postToMainDelayed(Runnable runnable, long delayMs) {
        new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMs);
    }
}
