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

            String[][] defaultTemplateFields = {
                {"datos_generales", "número_muestra", "String"},
                {"datos_generales", "código_informante", "String"},
                {"datos_generales", "transliteración", "String"},
                {"datos_generales", "revisión_primera", "String"},
                {"datos_generales", "revisión_segunda", "String"},
                {"datos_generales", "etiquetado", "String"},
                {"datos_muestra", "fecha_recogida", "String"},
                {"datos_muestra", "palabras", "String"},
                {"datos_muestra", "género_discursivo", "String"},
                {"datos_muestra", "observaciones", "String"},
                {"datos_informante", "nombre", "String"},
                {"datos_informante", "sexo", "String"},
                {"datos_informante", "nivel_referencia", "String"},
                {"datos_informante", "curso", "String"},
                {"datos_informante", "universidad", "String"},
                {"datos_informante", "nivel_CET", "String"},
                {"datos_informante", "estancia_España", "String"},
                {"datos_informante", "observaciones", "String"},
            };

            for (String[] fieldData : defaultTemplateFields) {
                MetadataField field = new MetadataField();
                field.setCategory(fieldData[0]);
                field.setName(fieldData[1]);
                field.setType(fieldData[2]);
                field.setDefinedAt(project);
                metadataFieldRepository.save(field);
            }
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