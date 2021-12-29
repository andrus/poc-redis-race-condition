package poc.redis.race.service;

import io.bootique.BQCoreModule;
import io.bootique.BaseModule;
import io.bootique.Bootique;
import io.bootique.di.Binder;
import io.bootique.jersey.JerseyModule;
import io.bootique.meta.application.OptionMetadata;

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
		OptionMetadata singularity = OptionMetadata.builder("single-jedi", "Forces to use single Jedis connection in all CAS routine")
									.valueOptionalWithDefault("false")
									.build();
		
		OptionMetadata cacheStrategy = OptionMetadata.builder("jedi-strategy", "Selects which cache to initialize: blind | dummy | wtw | cas | pessimistic | late-cas | late-pessimistic ")
							.valueOptionalWithDefault("blind")
							.build();

		BQCoreModule.extend(binder).addOption(singularity);
		BQCoreModule.extend(binder).addOption(cacheStrategy);
				
        JerseyModule.extend(binder).addResource(ScoreApi.class);
	}
}
