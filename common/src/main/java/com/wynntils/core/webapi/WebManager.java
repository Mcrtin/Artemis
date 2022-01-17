/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.webapi;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.FeatureLoader;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.webapi.profiles.ItemGuessProfile;
import com.wynntils.core.webapi.request.Request;
import com.wynntils.core.webapi.request.RequestBuilder;
import com.wynntils.core.webapi.request.RequestHandler;
import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import org.jetbrains.annotations.Nullable;

/** Provides and loads web content on demand */
public class WebManager {
    public static final File API_CACHE_ROOT = new File(WynntilsMod.MOD_STORAGE_ROOT, "apicache");

    private static boolean setup = false;
    private static final RequestHandler handler = new RequestHandler();

    public static @Nullable WebReader apiUrls = null;

    private static final SingleStaticProvider<HashMap<String, ItemGuessProfile>>
            itemGuessesProvider;

    public static void init() {
        tryReloadApiUrls(false, true);
    }

    static {
        itemGuessesProvider =
                new SingleStaticProvider<>() {
                    @Override
                    protected Request getRequest() {
                        return new RequestBuilder(apiUrls.get("ItemGuesses"), "item_guesses")
                                .cacheTo(new File(API_CACHE_ROOT, "item_guesses.json"))
                                .handleJsonObject(
                                        json -> {
                                            Type type =
                                                    new TypeToken<
                                                            HashMap<
                                                                    String,
                                                                    ItemGuessProfile>>() {}.getType();

                                            GsonBuilder gsonBuilder = new GsonBuilder();
                                            gsonBuilder.registerTypeHierarchyAdapter(
                                                    HashMap.class,
                                                    new ItemGuessProfile.ItemGuessDeserializer());
                                            Gson gson = gsonBuilder.create();

                                            value = gson.fromJson(json, type);
                                            return true;
                                        })
                                .useCacheAsBackup()
                                .onError(
                                        () -> {
                                            value = new HashMap<>();
                                        })
                                .build();
                    }
                };
    }

    public static void tryReloadApis(boolean async) {
        tryReloadApiUrls(async, false);
    }

    private static void tryReloadApiUrls(boolean async, boolean isSetup) {
        if (apiUrls == null) {
            handler.addRequest(
                    new RequestBuilder("https://api.wynntils.com/webapi", "webapi")
                            .cacheTo(new File(API_CACHE_ROOT, "webapi.txt"))
                            .handleWebReader(
                                    reader -> {
                                        apiUrls = reader;
                                        if (!setup) {
                                            setup = true;
                                            WebManager.setupWebApi(isSetup);
                                        }
                                        return true;
                                    })
                            .build());

            handler.dispatch(async);
        }
    }

    public static void setupWebApi(boolean isSetup) {
        // If it is during the setup the features will be enabled by the FeatureHandler later, so
        // there is no
        // need to do it here
        // This is for if the apiUrls fail and are reloaded externally
        if (!isSetup) {
            FeatureLoader.getFeatures()
                    .forEach(
                            f -> {
                                if (f.isApiDependent() && !f.isEnabled()) {
                                    f.onEnable();
                                }
                            });
        }

        // MapModule.getModule().getMainMap().updateMap();
    }

    public static void loadMarked(boolean async) {
        boolean toLoad = false;

        for (StaticProvider provider : Arrays.asList(itemGuessesProvider)) {
            if (provider.shouldLoad == LoadingPhase.TO_LOAD) {
                handler.addRequest(provider.getRequest());
                provider.shouldLoad = LoadingPhase.LOADED;
                toLoad = true;
            }
        }

        if (toLoad) handler.dispatch(async);
    }

    public static SingleStaticProvider<HashMap<String, ItemGuessProfile>> getItemGuessesProvider() {
        return itemGuessesProvider;
    }

    public abstract static class StaticProvider {
        protected LoadingPhase shouldLoad = LoadingPhase.UNLOADED;

        public void markToLoad() {
            if (shouldLoad == LoadingPhase.UNLOADED) shouldLoad = LoadingPhase.TO_LOAD;
        }

        protected abstract Request getRequest();
    }

    public abstract static class SingleStaticProvider<T> extends StaticProvider {
        protected T value;

        public void markToLoad() {
            if (shouldLoad == LoadingPhase.UNLOADED) shouldLoad = LoadingPhase.TO_LOAD;
        }

        public T getValue() {
            return value;
        }
    }

    public static boolean isSetup() {
        return setup;
    }
}