package ca.utoronto.utm.mcs;

import javax.inject.Singleton;
import dagger.Component;

@Singleton
@Component(modules = DaggerModule.class)
public interface DaggerComponent {

  public Dagger buildMongoHttp();

  public Post buildPost();
}
