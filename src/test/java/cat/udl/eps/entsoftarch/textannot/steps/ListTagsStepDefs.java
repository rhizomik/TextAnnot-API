package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ListTagsStepDefs {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProjectRepository projectRepository;


    @When("^I list tags$")
    public void iListTags() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/tags")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate())
        )
                .andDo(print());
    }

    @Given("^I create a tag with name \"([^\"]*)\"$")
    public void ICreateATag(String name) throws Throwable {
        Tag tag = new Tag(name);
        tagRepository.save(tag);
    }

    @And("^The tag with name \"([^\"]*)\" is in the response$")
    public void theTagWithNameIsInTheResponse(String name) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.tags.*.name", hasItem(name)));
    }

    @And("^The tags' list is empty$")
    public void theTagsListIsEmpty() throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.tags", hasSize(0)));
    }

    @Then("^I create a new Tag Hierarchy called \"([^\"]*)\"$")
    public void iCreateANewTagHierarchyCalled(String name) throws Throwable {
        Project project = new Project();
        project.setName(name);
        projectRepository.save(project);
    }

    @And("^I create a tag with name \"([^\"]*)\" linked to the tag hierarchy called \"([^\"]*)\"$")
    public void iCreateATagWithNameLinkedToTheTagHierarchyCalled(String name, String tagHierarchyName) throws Throwable {
        Project project = projectRepository.findByName(tagHierarchyName);
        Tag tag = new Tag(name);
        if (project != null)
            tag.setProject(project);
        tagRepository.save(tag);
        JSONObject AddTag = new JSONObject();
        AddTag.put("name", name);
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AddTag.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @Then("^I list tags in the tag hierarchy called \"([^\"]*)\"$")
    public void iListTagsInTheTagHierarchyCalled(String name) throws Throwable {
        Project project = projectRepository.findByName(name);
        List<Tag> tags = tagRepository.findByProject(projectRepository.findByName(name));
        stepDefs.mockMvc.perform(
                get("/tags/search/findByTagHierarchy?project=" + project.getUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$._embedded.tags[0].id", is(tags.get(0).getId())))
                .andExpect(jsonPath("$._embedded.tags[1].id", is(tags.get(1).getId())));
    }

    @And("^I create a tag with name \"([^\"]*)\" not linked to any tag hierarchy$")
    public void iCreateATagWithNameNotLinkedToAnyTagHierarchy(String name) throws Throwable {
        Tag tag = new Tag(name);
        tagRepository.save(tag);
    }


    @Then("^The tags' list is empty in the tag hierarchy called \"([^\"]*)\"$")
    public void theTagsListIsEmptyInTheTagHierarchyCalled(String name) throws Throwable {
        if(projectRepository.findByName(name) != null) {
            List<Tag> tags = tagRepository.findByProject(projectRepository.findByName(name));
            assertEquals(0, tags.size());
        }
    }

    @And("^It exists a TagHierarchy with name \"([^\"]*)\"$")
    public void itExistsATagHierarchyWithName(String name) throws Throwable {
        Project project = new Project();
        project.setName(name);
        projectRepository.save(project);
    }

    @When("^I list tags in tag hierarchy \"([^\"]*)\"$")
    public void iListTagsInTagHierarchy(String name) throws Throwable {
        String uri = "";
        Project project = projectRepository.findByName(name);
        if(project != null) {
            uri = project.getUri();
        }
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/tags/search/findByTagHierarchy?project=" + uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }
}
