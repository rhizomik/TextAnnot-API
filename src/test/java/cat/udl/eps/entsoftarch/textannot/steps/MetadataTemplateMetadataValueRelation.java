package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.MetadataValue;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataValueRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.transaction.Transactional;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

public class MetadataTemplateMetadataValueRelation {
    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MetadataFieldRepository metadataFieldRepository;

    @Autowired
    private MetadataValueRepository metadataValueRepository;

    private List<Project> projectList;

    @Given("^A MetadataTemplate with name \"([^\"]*)\" defines a MetadataField with name \"([^\"]*)\" and type \"([^\"]*)\" that values a MetadataValue with value \"([^\"]*)\"$")
    public void aMetadataTemplateWithNameDefinesAMetadataFieldWithNameAndTypeThatValuesAMetadataValueWithValue(String name, String FName, String FType, String VValue) {
        Project project = new Project();
        project.setName(name);
        MetadataField mf = new MetadataField(FName, FType);
        MetadataValue mv = new MetadataValue(VValue);

        project = projectRepository.save(project);
        mf.setDefinedAt(project);
        metadataFieldRepository.save(mf);
        mv.setValues(mf);
        metadataValueRepository.save(mv);
    }

    @When("^I find MetadataTemplates by MetadataValue with value \"([^\"]*)\"$")
    @Transactional
    public void iFindMetadataTemplatesByMetadataValueWithValue(String VValue) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataValues/search/findAllMetadataTemplatesByValue?value={VValue}", VValue)
                .accept(MediaType.APPLICATION_JSON)
                .with(AuthenticationStepDefs.authenticate()))
                .andDo(MockMvcResultHandlers.print());
    }

    @Then("^I get a list with a MetadataTemplate with name \"([^\"]*)\"$")
    public void iGetAListWithAMetadataTemplateWithName(String name) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.metadataTemplates.*.name", hasItem(name)));

    }
}
