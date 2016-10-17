package org.sagebionetworks.bridge.sdk.rest;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Map;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.sagebionetworks.bridge.sdk.rest.model.SignIn;

/**
 * Created by liujoshua on 10/11/16.
 */
public class ApiClientProvider {
    public static final Gson GSON = Converters.registerAll(new GsonBuilder())
            .registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();
    
    private final OkHttpClient unauthenticatedOkHttpClient;
    private final Retrofit.Builder retrofitBuilder;
    private final UserSessionInfoProvider userSessionInfoProvider;
    private final Map<SignIn, WeakReference<Retrofit>> authenticatedRetrofits;
    private final Map<SignIn, Map<Class, WeakReference>> authenticatedClients;

    public ApiClientProvider(String baseUrl, String userAgent) {
        this(baseUrl, userAgent, null);
    }

    // allow unit tests to inject a UserSessionInfoProvider
    ApiClientProvider(String baseUrl, String userAgent, UserSessionInfoProvider userSessionInfoProvider) {
        authenticatedRetrofits = Maps.newHashMap();
        authenticatedClients = Maps.newHashMap();

        HeaderHandler headerHandler = new HeaderHandler(userAgent);

        unauthenticatedOkHttpClient = new OkHttpClient.Builder().addInterceptor(headerHandler)
                .addInterceptor(new ErrorResponseInterceptor()).build();

        retrofitBuilder = new Retrofit.Builder().baseUrl(baseUrl)
                .client(unauthenticatedOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GSON));
        
        this.userSessionInfoProvider = userSessionInfoProvider != null ? userSessionInfoProvider
                : new UserSessionInfoProvider(getAuthenticatedRetrofit(null));
    }

    /**
     * Creates an unauthenticated client.
     *
     * @param service
     *         Class representing the service
     * @return service client
     */
    public <T> T getClient(Class<T> service) {
        return getClientImpl(service, null);
    }

    /**
     * @param service
     *         Class representing the service
     * @param signIn
     *         credentials for the user, or null for an unauthenticated client
     * @return service client that is authenticated with the user's credentials
     */
    public <T> T getClient(Class<T> service, SignIn signIn) {
        Preconditions.checkNotNull(signIn);

        return getClientImpl(service, signIn);
    }

    private <T> T getClientImpl(Class<T> service, SignIn signIn) {
        Map<Class, WeakReference> userClients = authenticatedClients.get(signIn);
        if (userClients == null) {
            userClients = Maps.newHashMap();
            authenticatedClients.put(signIn, userClients);
        }

        T authenticateClient = null;
        WeakReference<T> clientReference = userClients.get(service);

        if (clientReference != null) {
            authenticateClient = (T) clientReference.get();
        }

        if (authenticateClient == null) {
            authenticateClient = getAuthenticatedRetrofit(signIn).create(service);
            userClients.put(service, new WeakReference<>(authenticateClient));
        }

        return authenticateClient;
    }

    Retrofit getAuthenticatedRetrofit(SignIn signIn) {
        Retrofit authenticatedRetrofit = null;

        WeakReference<Retrofit> authenticatedRetrofitReference = authenticatedRetrofits.get(signIn);
        if (authenticatedRetrofitReference != null) {
            authenticatedRetrofit = authenticatedRetrofitReference.get();
        }

        if (authenticatedRetrofit == null) {
            authenticatedRetrofit = createAuthenticatedRetrofit(signIn, null);
        }

        return authenticatedRetrofit;
    }

    // allow test to inject retrofit
    Retrofit createAuthenticatedRetrofit(SignIn signIn, AuthenticationHandler handler) {
        OkHttpClient.Builder httpClientBuilder = unauthenticatedOkHttpClient.newBuilder();

        if (signIn != null) {
            AuthenticationHandler authenticationHandler = handler;
            // this is the normal code path (only tests will inject a handler)
            if (authenticationHandler == null) {
                authenticationHandler = new AuthenticationHandler(signIn,
                        userSessionInfoProvider
                );
            }
            httpClientBuilder.addInterceptor(authenticationHandler).authenticator(authenticationHandler);
        }

        Retrofit authenticatedRetrofit = retrofitBuilder.client(httpClientBuilder.build()).build();
        authenticatedRetrofits.put(signIn,
                new WeakReference<>(authenticatedRetrofit));
        return authenticatedRetrofit;
    }

    // allow test access to retrofit builder
    Retrofit.Builder getRetrofitBuilder() {
        return retrofitBuilder;
    }

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>,
            JsonDeserializer<byte[]> {
        public byte[] deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context
        ) throws JsonParseException {
            return BaseEncoding.base64().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(BaseEncoding.base64().encode(src));
        }
    }
}