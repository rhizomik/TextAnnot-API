package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.*;
import cat.udl.eps.entsoftarch.textannot.repository.*;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class FilterSamplesStepDef {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private MetadataTemplateRepository metadataTemplateRepository;

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
        Sample sample = sampleRepository.save(new Sample(text));
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
        Sample sample = sampleRepository.save(new Sample(text));
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
        JSONObject request = new JSONObject();
        request.put("word", word);

        stepDefs.result = stepDefs.mockMvc.perform(post("/samples/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .content(request.toString()))
        .andDo(print());
    }

    @And("^The response contains (\\d+) sample$")
    public void theResponseContainsSample(int numSamples) throws Exception {
        stepDefs.result.andExpect(jsonPath("$._embedded.samples", hasSize(numSamples)));
    }

    @And("^There is a metadata field \"([^\"]*)\" related to the template \"([^\"]*)\"$")
    public void thereIsAMetadataFieldRelatedToTheTemplate(String field, String template) throws Throwable {
        MetadataTemplate metadataTemplate = metadataTemplateRepository.findByName(template);
        MetadataField metadataField = new MetadataField();
        metadataField.setDefinedAt(metadataTemplate);
        metadataField.setName(field);
        metadataField.setType("string");
        metadataFieldRepository.save(metadataField);

    }

    @When("^I filter the samples having the word \"([^\"]*)\" and the metadata$")
    public void iFilterTheSamplesHavingTheWordAndTheMetadata(String word, DataTable table) throws Throwable {
        JSONObject request = new JSONObject();
        request.put("word", word);
        request.put("metadata", new JSONObject(table.asMap(String.class, String.class)));

        stepDefs.result = stepDefs.mockMvc.perform(post("/samples/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .content(request.toString()))
                .andDo(print());
    }

    @When("^I filter the samples having the word \"([^\"]*)\" and annotated by the tag \"([^\"]*)\"$")
    public void iFilterTheSamplesHavingTheWordAndAnnotatedByTheTag(String word, String tag) throws Throwable {
        JSONObject request = new JSONObject();
        request.put("word", word);
        request.put("tags", new JSONArray(Arrays.asList(tag)));

        stepDefs.result = stepDefs.mockMvc.perform(post("/samples/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate())
                .content(request.toString()))
                .andDo(print());
    }
}
