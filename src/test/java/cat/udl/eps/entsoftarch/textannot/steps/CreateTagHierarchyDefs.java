package cat.udl.eps.entsoftarch.textannot.steps;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;
import org.springframework.http.MediaType;

public class CreateTagHierarchyDefs {

    private final StepDefs stepDefs;
    private final SampleRepository sampleRepository;
    private final ProjectRepository projectRepository;

    private Sample sample;
    private Project project;

    public CreateTagHierarchyDefs(StepDefs stepDefs,
                                  SampleRepository sampleRepository,
                                  ProjectRepository projectRepository) {
        this.stepDefs = stepDefs;
        this.sampleRepository = sampleRepository;
        this.projectRepository = projectRepository;
    }

    @When("^I create a new Tag Hierarchy with name \"([^\"]*)\"$")
    public void iCreateANewTagHierarchyWithName(String name) throws Exception {
        Project project = new Project();
        project.setName(name);
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/tagHierarchies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stepDefs.mapper.writeValueAsString(project))
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @And("^The Tag Hierarchy name is \"([^\"]*)\"$")
    public void theTagHierarchyNameIs(String name) throws Exception {
        String location = stepDefs.result.andReturn().getResponse().getHeader("Location");
        Assert.assertTrue("location is not null", location != null);
        stepDefs.result = stepDefs.mockMvc.perform(
                get(location)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)));
    }

    @Given("^Exists a Sample with text \"([^\"]*)\" and Project \"([^\"]*)\"$")
    public void thereIsASingleSampleWithText(String text, String projectName) {
        Project project = this.existsAProjectWithName(projectName);
        sample = new Sample(text);
        sample.setProject(project);
        sampleRepository.save(sample);
    }

    @And("^Exists a Project with name \"([^\"]*)\"$")
    public Project existsAProjectWithName(String name) {
        project = projectRepository.findByName(name);
        if (project == null){
            project = new Project();
            project.setName(name);
            projectRepository.save(project);
        }
        return project;
    }

    @When("^I set the previous Sample tagged by the previous Tag Hierarchy$")
    public void iSetThePreviousSampleTaggedByThePreviousTagHierarchy() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                put("/samples/"+ sample.getId() +"/taggedBy")
                        .contentType("text/uri-list")
                        .content(project.getUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()));
    }

    @Then("^The Tag Hierarchy have (\\d+) samples$")
    public void theTagHierarchyWithNameHaveSamples(String name, int numSamples) throws Exception {
        stepDefs.mockMvc.perform(
                get("/tagHierarchies/" + project.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @Then("^The Sample is taged by the Tag Hierarchy$")
    public void theSampleIsTagedByTheTagHierarchy() throws Throwable {
        stepDefs.mockMvc.perform(
                get("/samples/" + sample.getId() + "/taggedBy")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.id", is(project.getId())));
    }

    @When("^I create a new sample with text \"([^\"]*)\" tagged by the tag hierarchy \"([^\"]*)\"$")
    public void iCreateANewSampleWithTextTaggedByTheTagHierarchy(String text, String tagHierarchyName) throws Throwable {
        Project tagHierarchy = projectRepository.findByName(tagHierarchyName);
        Assert.assertNotNull("Tag hierarchy must exist", tagHierarchy);
        Sample sample = new Sample();
        sample.setText(text);
        sample.setProject(tagHierarchy);
        stepDefs.mockMvc.perform(post("/samples")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(stepDefs.mapper.writeValueAsString(sample))
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(status().isCreated());
    }

    @Then("^The tag hierarchy \"([^\"]*)\" tags a sample with text \"([^\"]*)\"$")
    public void theTagHierarchyTagsASampleWithText(String tagHierarchyName, String text) throws Throwable {
        Project project = projectRepository.findByName(tagHierarchyName);
        Assert.assertNotNull("Tag hiearchy must exist", project);
        stepDefs.mockMvc.perform(
            get("/samples/search/findByTaggedBy?taggedBy=" + project.getUri())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$._embedded.samples[0].text", is(text)));
    }
}
