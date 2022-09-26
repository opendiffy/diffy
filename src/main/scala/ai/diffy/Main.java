package ai.diffy;

import com.samskivert.mustache.DefaultCollector;
import com.samskivert.mustache.Mustache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {
  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

  @Bean
  public Mustache.Compiler mustacheCompiler(Mustache.TemplateLoader templateLoader) {

    return Mustache.compiler()
            .defaultValue("Some Default Value")
            .withLoader(templateLoader)
            .withCollector(new DefaultCollector());
  }
}