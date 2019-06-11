package cat.udl.eps.entsoftarch.textannot.steps;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cat.udl.eps.entsoftarch.textannot.domain.MetadataField;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.repository.MetadataFieldRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class AddMetadataFieldStepDefs {

    @Autowired
    private StepDefs stepDefs;
    private MetadataField metaField;
    private Project project;


    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MetadataFieldRepository metadataFieldRepository;


    @When("^I create a new metadatafield with text \"([^\"]*)\" and type \"([^\"]*)\"$")
    public void iCreateANewMetadatafieldWithTextAndType(String name, String type) throws Throwable {
        JSONObject AddMetaDataField = new JSONObject();
        AddMetaDataField.put("name",name);
        AddMetaDataField.put("type",type);
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/metadataFields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AddMetaDataField.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());

    }

    @And("^It has been created a new metadatafield with text \"([^\"]*)\" and type \"([^\"]*)\" and Id (\\d+)$")
    public void itHasBeenCreatedANewMetadatafieldWithTextAndTypeAndId(String name, String type, Integer id) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.type", is(type)))
                .andExpect(jsonPath("$.id", is(id)));
    }

    @And("^It has not been created a metadatafield with text \"([^\"]*)\" and type \"([^\"]*)\" and Id (\\d+)$")
    public void itHasNotBeenCreatedAMetadatafieldWithTextAndTypeAndId(String name, String type, Integer id) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @And("^there is a created metadataTemplate with name \"([^\"]*)\"$")
    public void thereIsACreatedMetadataTemplateWithName(String arg0) throws Throwable {
        JSONObject metadataTemplate = new JSONObject();
        metadataTemplate.put("name", arg0);
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/metadataTemplates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metadataTemplate.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @When("^I register a new metadataField with text \"([^\"]*)\" and type \"([^\"]*)\" " +
            "for metadataTemplate with value \"([^\"]*)\"$")
    public void iRegisterANewMetadataFieldWithTextAndTypeForMetadataTemplateWithValue
            (String name, String type, String metadataTemplateValue) throws Throwable {
        JSONObject metadataField = new JSONObject();
        project = projectRepository.findByName(metadataTemplateValue);
        metadataField.put("name", name);
        metadataField.put("type", type);
        metadataField.put("definedAt", "/metadataTemplates/"+ project.getId()+"");
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/metadataFields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(metadataField.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());

        String newResourceUri = stepDefs.result.andReturn().getResponse().getHeader("Location");
    }

    @And("^It has been created a new metadataField with text \"([^\"]*)\"  for metadataTemplate with value \"([^\"]*)\"$")
    public void itHasBeenCreatedANewMetadataFieldWithTextForMetadataTemplateWithValue(String name, String arg1) throws Throwable {
        metaField=metadataFieldRepository.findByName(name);
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/metadataFields/{id}", metaField.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(name)));

        stepDefs.mockMvc.perform(
                get("/metadataFields/"+metaField.getId()+"/definedAt")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print())
                .andExpect(jsonPath("$.name", is(project.getName())))
                .andExpect(status().is(200));
    }
}
