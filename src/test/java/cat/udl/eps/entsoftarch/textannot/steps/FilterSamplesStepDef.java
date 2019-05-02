package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
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

    @And("^There is a sample with text \"([^\"]*)\"$")
    public void thereIsASampleWithText(String text) throws Throwable {
        sampleRepository.save(new Sample(text));
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
}
