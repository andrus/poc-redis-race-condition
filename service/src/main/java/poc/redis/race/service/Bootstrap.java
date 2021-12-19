package poc.redis.race.service;

import io.bootique.BaseModule;
import io.bootique.Bootique;
import io.bootique.di.Binder;
import io.bootique.jersey.JerseyModule;

public class Bootstrap extends BaseModule {

    public static void main(String[] args) {
        Bootique.app(args)
                .autoLoadModules()
                .module(Bootstrap.class)
                .exec()
                .exit();
    }

    @Override
    public void configure(Binder binder) {
        JerseyModule.extend(binder).addResource(ScoreApi.class);
    }
}
