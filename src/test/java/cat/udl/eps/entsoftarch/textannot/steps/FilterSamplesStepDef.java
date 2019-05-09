package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataTemplate;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataTemplateRepository;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataValueRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
}
