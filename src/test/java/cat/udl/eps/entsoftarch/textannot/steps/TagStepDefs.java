package cat.udl.eps.entsoftarch.textannot.steps;


import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class TagStepDefs {

    @Autowired
    private StepDefs stepDefs;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TagRepository tagRepository;

    private Tag tag, parent, child;
    private Project project = null;
    private String childUri, parentUri, errormessage;

    @When("^I create a new tag with name \"([^\"]*)\"$")
    public void iCreateANewTagWithName(String name) throws Throwable {
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

    @And("^It has been created a new tag with name \"([^\"]*)\" and Id (\\d+)$")
    public void itHasBeenCreatedANewTagWithNameAndId(String name, Integer id) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/tags/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.id", is(id)));
    }

//    @And("^Exists a Project with name \"([^\"]*)\"$")
    public Project existsATagHierarchyWithName(String name) throws Throwable {
        project = projectRepository.findByName(name);
        if(project == null){
            project = new Project();
            project.setName(name);
            projectRepository.save(project);
        }
        return project;
    }

    @And("^Exists a Tag with name \"([^\"]*)\" associated to the Project \"([^\"]*)\"$")
    public void existsATagWithName(String tagName, String tagHierarchyName) throws Throwable {
        Project project = existsATagHierarchyWithName(tagHierarchyName);
        tag = tagRepository.findByName(tagName);
        if(tag == null)
            tag = new Tag(tagName);

        tag.setProject(project);
        tagRepository.save(tag);
    }

    @When("^I create a new tag with name \"([^\"]*)\" defined in the tag hierarchy \"([^\"]*)\"$")
    public void iCreateANewTagWithNameDefinedInTheTagHierarchy(String name, String tagHierName) throws Throwable {
        List<Tag> tags = tagRepository.findByNameContaining(name);
        Project project = projectRepository.findByName(tagHierName);
        Assert.assertNotNull("Project exists", project);
        if(!tags.isEmpty())
            tag.setName(tags.get(0).getName());
        stepDefs.result = stepDefs.mockMvc.perform(
                put("/tags/" + tag.getId() + "/project")
                    .contentType("text/uri-list")
                    .content(project.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @Then("^The tag hierarchy \"([^\"]*)\" defines a tag with the text \"([^\"]*)\"$")
    public void theTagHierarchyDefinesATagWithTheText(String tagHierName, String name) throws Throwable {
        stepDefs.mockMvc.perform(
                get("/tags/search/findByTagHierarchy?project=" + project.getUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$._embedded.tags[0].id", is(tag.getId()))
        );
    }

    @And("^I create the parent Tag with name \"([^\"]*)\"$")
    public void iCreateTheParentTagWithName(String parentName) throws Throwable {
        parent = new Tag(parentName);
        parent.setProject(project);
        tagRepository.save(parent);
        parentUri = parent.getUri();
        System.out.println(parent.getUri());


    }

    @And("^I create the child Tag with name \"([^\"]*)\"$")
    public void iCreateTheChildTagWithName(String childName) throws Throwable {
        child = new Tag(childName);
        child.setProject(project);
        tagRepository.save(child);
        childUri = child.getUri();

    }

    @When("^I set the parent with name \"([^\"]*)\" to child with name \"([^\"]*)\"$")
    public void iSetTheParentWithNameToChildWithName(String parentName, String childName) throws Throwable {
        parent = tagRepository.findByNameContaining(parentName).get(0);
        child = tagRepository.findByNameContaining(childName).get(0);
        child.setParent(parent);
    }

    @And("^I create link between parent with name \"([^\"]*)\" and child with name \"([^\"]*)\"$")
    public void iCreateLinkBetweenParentWithNameAndChildWithName(String parentName, String childName) throws Throwable {
        stepDefs.mockMvc.perform(
            put(childUri + "/parent")
                .contentType("text/uri-list")
                .content(parentUri)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
            .andDo(print()
        );


    }

    @Then("^Parent with name \"([^\"]*)\" was set correctly to the child with name \"([^\"]*)\"$")
    public void parentWithNameWasSetCorrectlyToTheChildWithName(String parentName, String childName) throws Throwable {
        stepDefs.mockMvc.perform(
            get("/tags/search/findByParent?parent=" + parentUri)
                .contentType(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
            .andDo(print())
            .andExpect(jsonPath("$._embedded.tags[0].id", is(child.getId()))
        );
    }


    @And("^I try to link between parent with name \"([^\"]*)\" and child with name \"([^\"]*)\"$")
    public void iTryToLinkBetweenParentWithNameAndChildWithName(String arg0, String arg1) throws Throwable {


        JSONObject parent = new JSONObject();
        parent.put("parent",parentUri);

        stepDefs.result  = stepDefs.mockMvc.perform(patch(childUri)
                .content("text/uri-list")
                .content(parent.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());

    }

    @When("^I delete the parent$")
    public void iDeleteTheParent() throws Throwable {
        stepDefs.result  = stepDefs.mockMvc.perform(delete(parentUri).with(AuthenticationStepDefs.authenticate())).andDo(print());
    }
}
