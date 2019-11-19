package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.*;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class FilterSamplesStepDef {

    private final int projectId = 1;

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MetadataFieldRepository metadataFieldRepository;

    @Autowired
    private MetadataValueRepository metadataValueRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @And("^There is a sample with text \"([^\"]*)\" with metadata$")
    public void thereIsASampleWithText(String text, DataTable metadata) throws Throwable {

        Sample sample = new Sample(text);
        sample.setProject(projectRepository.findById(projectId).get());
        sampleRepository.save(sample);
        for (Map.Entry<String, String> e: metadata.asMap(String.class, String.class).entrySet()) {
            MetadataField field = metadataFieldRepository.findByName(e.getKey());
            MetadataValue value = new MetadataValue();
            value.setValues(field);
            value.setForA(sample);
            value.setValue(e.getValue());
            metadataValueRepository.save(value);
        }
    }

    @And("^There is a sample with text \"([^\"]*)\" with annotations$")
    public void thereIsASampleWithTextWithAnnotations(String text, DataTable annotations) throws Throwable {
        Sample sample = new Sample(text);
        sample.setProject(projectRepository.findById(projectId).get());
        sampleRepository.save(sample);
        for (List<String> e: annotations.asLists(String.class)) {
            Annotation annotation = new Annotation();
            annotation.setSample(sample);
            annotation.setStart(Integer.parseInt(e.get(1)));
            annotation.setEnd(Integer.parseInt(e.get(2)));
            annotation.setTag(tagRepository.findByName(e.get(0)));
            annotationRepository.save(annotation);
        }
    }

    @When("^I filter the samples having the word \"([^\"]*)\"$")
    public void iFilterTheSamplesHavingTheWord(String word) throws Throwable {

        stepDefs.result = stepDefs.mockMvc.perform(get("/samples/filter")
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .param("word", word)
                .param("tags", "")
                .param("projectId", "" + projectId)).andDo(print());
    }

    @And("^The response contains (\\d+) sample$")
    public void theResponseContainsSample(int numSamples) throws Exception {
        stepDefs.result.andExpect(jsonPath("$._embedded.samples", hasSize(numSamples)));
    }

    @And("^There is a metadata field \"([^\"]*)\" related to the template \"([^\"]*)\"$")
    public void thereIsAMetadataFieldRelatedToTheTemplate(String field, String template) throws Throwable {
        Project metadataTemplate = projectRepository.findByName(template);
        MetadataField metadataField = new MetadataField();
        metadataField.setDefinedAt(metadataTemplate);
        metadataField.setName(field);
        metadataField.setType(MetadataField.FieldType.STRING);
        metadataFieldRepository.save(metadataField);

    }

    @When("^I filter the samples having the word \"([^\"]*)\" and the metadata$")
    public void iFilterTheSamplesHavingTheWordAndTheMetadata(String word, DataTable table) throws Throwable {
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("word", word);
        table.asMap(String.class, String.class).forEach(request::add);
        request.add("tags", "");
        request.add("projectId", String.valueOf(projectId));

        stepDefs.result = stepDefs.mockMvc.perform(get("/samples/filter")
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .params(request)).andDo(print());
    }

    @When("^I filter the samples having the word \"([^\"]*)\" and annotated by the tag \"([^\"]*)\"$")
    public void iFilterTheSamplesHavingTheWordAndAnnotatedByTheTag(String word, String tagName) throws Throwable {
        Tag tag = tagRepository.findByName(tagName);

        stepDefs.result = stepDefs.mockMvc.perform(get("/samples/filter")
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .param("word", word)
                .param("tags", String.valueOf(tag.getId()))
                .param("projectId", String.valueOf(projectId))).andDo(print());
    }
}
