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

            // Following values corresponds to:
            //   category - name - xmlName - type - privateField - includeStatistics
            String[][] defaultTemplateFields = {
                {"datos_generales", "Número Muestra", "número_muestra", "String", "true", "false"},
                {"datos_generales", "Código Informante", "código_informante", "String", "true", "false"},
                {"datos_generales", "Transliteración", "transliteración", "String", "true", "false"},
                {"datos_generales", "Primera Revisión", "revisión_primera", "String", "true", "false"},
                {"datos_generales", "Segunda Revisión", "revisión_segunda", "String", "true", "false"},
                {"datos_generales", "Etiquetado", "etiquetado", "String", "true", "false"},
                {"datos_muestra", "Fecha Recogida", "fecha_recogida", "String", "false", "false"},
                {"datos_muestra", "Número Palabras", "palabras", "String", "false", "false"},
                {"datos_muestra", "Género Discursivo", "género_discursivo", "String", "false", "true"},
                {"datos_muestra", "Observaciones", "observaciones", "String", "true", "false"},
                {"datos_informante", "Nombre", "nombre", "String", "false", "true"},
                {"datos_informante", "Sexo", "sexo", "String", "false", "true"},
                {"datos_informante", "Nivel Referencia", "nivel_referencia", "String", "false", "true"},
                {"datos_informante", "Curso", "curso", "String", "false", "true"},
                {"datos_informante", "Universidad", "universidad", "String", "false", "true"},
                {"datos_informante", "Nivel CET", "nivel_CET", "String", "false", "true"},
                {"datos_informante", "Estancia España", "estancia_España", "String", "false", "true"},
                {"datos_informante", "Observaciones", "observaciones", "String", "true", "false"},
            };

            for (String[] fieldData : defaultTemplateFields) {
                MetadataField field = new MetadataField();
                field.setCategory(fieldData[0]);
                field.setName(fieldData[1]);
                field.setXmlName(fieldData[2]);
                field.setType(fieldData[3]);
                field.setPrivateField(Boolean.parseBoolean(fieldData[4]));
                field.setIncludeStatistics(Boolean.parseBoolean(fieldData[5]));
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