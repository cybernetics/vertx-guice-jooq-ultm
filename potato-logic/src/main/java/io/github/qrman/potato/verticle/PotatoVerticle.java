package io.github.qrman.potato.verticle;

import com.google.inject.Guice;
import io.github.qrman.potato.model.Potato;
import io.github.qrman.potato.model.PotatoBag;
import io.github.qrman.potato.guice.GuiceModule;
import io.github.qrman.potato.logic.BasementFetch;
import io.github.qrman.potato.logic.RottenPotatoException;
import io.github.qrman.potato.logic.BasementStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Inject;
import lombok.extern.java.Log;

@Log
public class PotatoVerticle extends AbstractVerticle {

    @Inject
    private BasementStore basementStore;

    @Inject
    private BasementFetch basementFetch;

    @Override
    public void start() throws Exception {
        Guice.createInjector(new GuiceModule(vertx)).injectMembers(this);

        vertx.eventBus().consumer("potato-bag", (Message<String> bagWithPotatoMessage) -> {
            PotatoBag potatoBag = Json.decodeValue(bagWithPotatoMessage.body(), PotatoBag.class);
            log.log(Level.INFO, "Potato bag received {0}", potatoBag);

            vertx.executeBlocking((Future<Void> future) -> {
                basementStore.store(potatoBag);
                future.complete();
                
            }, result -> {
                if (result.succeeded()) {
                    bagWithPotatoMessage.reply("Potato bag stored in basement");
                }
                if (result.failed() && result.cause() instanceof RottenPotatoException) {
                    bagWithPotatoMessage.reply("Bag with rotten potato cannot be stored");
                }
            });
        });

        vertx.eventBus().consumer("potatoes-in-basement", (Message<String> countryMessage) -> {
            String potatoOriginCountry = countryMessage.body();
            log.log(Level.INFO, "Searching in basement for potatoes from {0}", potatoOriginCountry);
            List<Potato> potatoes = basementFetch.fetchByOrigin(potatoOriginCountry);
            countryMessage.reply(Json.encode(potatoes));
        });
    }
}
