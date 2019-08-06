package cat.udl.eps.entsoftarch.textannot.config;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;

import javax.annotation.PostConstruct;

import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;

@Configuration
public class RepositoryRestConfig extends RepositoryRestConfigurerAdapter {
    @Autowired Environment environment;
    @Autowired ProjectRepository projectRepository;
    @Autowired MetadataFieldRepository metadataFieldRepository;

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(Admin.class);
        config.exposeIdsFor(MetadataValue.class);
        config.exposeIdsFor(MetadataField.class);
        config.exposeIdsFor(XmlSample.class);
        config.exposeIdsFor(Sample.class);
        config.exposeIdsFor(Linguist.class);
        config.exposeIdsFor(Tag.class);
        config.exposeIdsFor(Annotation.class);
        config.exposeIdsFor(Project.class);
    }

    @PostConstruct
    @Profile("!test")
    public void init() {
        if(!environment.acceptsProfiles("Test") &&
            !projectRepository.existsByName("default")) {
            Project project = new Project();
            project.setName("default");
            projectRepository.save(project);
        }
    }

    @Bean
    public HateoasPageableHandlerMethodArgumentResolver customResolver(
            HateoasPageableHandlerMethodArgumentResolver pageableResolver) {
        pageableResolver.setOneIndexedParameters(false);
        pageableResolver.setFallbackPageable(new PageRequest(0, Integer.MAX_VALUE));
        pageableResolver.setMaxPageSize(Integer.MAX_VALUE);
        return pageableResolver;
    }
}