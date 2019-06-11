package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.*;

public class MetadataTemplateMetadataFieldRelation {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MetadataFieldRepository mtfr;

    @Autowired
    private StepDefs stepDefs;

    private List<Project> projectList;

    @Given("^A MetadataTemplate with name \"([^\"]*)\" defines a MetadataField with name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void aMetadataTemplateWithNameDefinesAMetadataFieldWithNameAndType(String name, String FName, String FType) {
        Project project = new Project();
        project.setName(name);
        MetadataField mf = new MetadataField(FName, FType);

        project = projectRepository.save(project);
        mf.setDefinedAt(project);
        mtfr.save(mf);
    }

    @When("^I find MetadataTemplates by MetadataField name \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void iFindMetadataTemplateByMetadataFieldNameAndType(String FName, String FType) throws Throwable {

        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/search/findAllMetadataTemplatesByNameAndType?name={FName}&type={FType}", FName, FType)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
                .andDo(MockMvcResultHandlers.print());
    }

    @Then("^I get the list with a MetadataTemplate with name \"([^\"]*)\"$")
    public void iGetTheListWithAMetadataTemplateWithName(String name) throws Throwable {

        stepDefs.result.andExpect(jsonPath("$._embedded.metadataTemplates.*.name", hasItem(name)));

    }


    @Given("^A MetadataTemplate with name \"([^\"]*)\" which defines a MetadataFields with name \"([^\"]*)\"$")
    public void aMetadataTemplateWithNameWhichDefinesAMetadataFieldsWithName(String name, String FName) {
        Project project = new Project();
        project.setName(name);
        MetadataField f = new MetadataField(FName,"NTp"); //no type

        project = projectRepository.save(project);
        f.setDefinedAt(project);
        mtfr.save(f);

    }

    @When("^I find MetadataTemplate by MetadataField name \"([^\"]*)\"$")
    public void iFindMetadataTemplateByMetadataFieldName(String FName) throws Throwable {

        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/search/findAllMetadataTemplatesByName?name={FName}", FName)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
                .andDo(MockMvcResultHandlers.print());
    }

    @Given("^A MetadataTemplate with name \"([^\"]*)\" defines a MetadataField with type \"([^\"]*)\"$")
    public void aMetadataTemplateWithNameDefinesAMedatataFieldWithType(String name, String FType) {
        Project project = new Project();
        project.setName(name);
        MetadataField f = new MetadataField("NName",FType); //no name

        project = projectRepository.save(project);
        f.setDefinedAt(project);
        mtfr.save(f);
    }

    @When("^I find MetadataTemplate by MetadataField type \"([^\"]*)\"$")
    public void iFindMetadataTemplateByMetadataFieldType(String FType) throws Throwable {

        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/search/findAllMetadataTemplatesByType?type={FType}",FType)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
                .andDo(MockMvcResultHandlers.print());
    }
}
