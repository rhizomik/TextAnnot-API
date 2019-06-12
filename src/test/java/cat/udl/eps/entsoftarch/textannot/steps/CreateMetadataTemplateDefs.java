package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class CreateMetadataTemplateDefs {

    @Autowired
    private StepDefs stepDefs;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private ProjectRepository projectRepository;

    protected ResultActions result;

    private Sample sample;

    private String newResourceUri;

    @When("^I create a new Project with name \"([^\"]*)\"$")
    public void iCreateANewProjectTemplateWithName(String name) throws Throwable {
        JSONObject project = new JSONObject();
        project.put("name", name);
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
        newResourceUri = stepDefs.result.andReturn().getResponse().getHeader("Location");
    }

    @And("^The Project name is \"([^\"]*)\"$")
    public void theProjectNameIs(String name) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get(newResourceUri)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)));
    }

    @When("^There is a single Sample with text \"([^\"]*)\"$")
    public void thereIsASingleSampleWithText(String text) throws Throwable {
        sample = new Sample();
        sample.setText(text);
        sampleRepository.save(sample);
    }


    @When("^I create a new Project \"([^\"]*)\" with the previous sample$")
    public void iCreateANewMetadataTemplateThePreviousSample(String name) throws Throwable {
        Project project =  new Project();
        project.setName(name);
        projectRepository.save(project);
        stepDefs.result = stepDefs.mockMvc.perform(
                put("/samples/"+ sample.getId() +"/project")
                        .contentType("text/uri-list")
                        .content(project.getUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @Then("^The Project with name \"([^\"]*)\" have (\\d+) samples$")
    public void theMetadataTemplateWithNameHaveSamples(String name, int size) throws Throwable {
        List<Sample> samples = sampleRepository.findByProjectName(name);
        Assert.assertTrue(
                "Only exists 1 sample project a MetadataTemplate with name " + name,
                samples != null && samples.size() == size
        );
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/samples/" + samples.get(0).getId() + "/project")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)));
    }

}
