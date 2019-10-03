package cat.udl.eps.entsoftarch.textannot.steps;

import cat.udl.eps.entsoftarch.textannot.domain.AnnotationStatus;
import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.AnnotationStatusRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import javax.transaction.Transactional;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class AnnotationStatusStepdefs {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AnnotationStatusRepository annotationStatusRepository;

    @Autowired
    private SampleRepository sampleRepository;


    Project project;

    @When("^I create a new Annotation status with name \"([^\"]*)\" for the project \"([^\"]*)\"$")
    public void iCreateANewAnnotationStatusWithNameForTheProject(String name, String projectName) throws Throwable {
        JSONObject request = new JSONObject();
        request.put("name", name);
        project = projectRepository.findByName(projectName);
        request.put("definedAt", project.getUri());
        stepDefs.result = stepDefs.mockMvc.perform(
                post("/annotationStatuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @And("^There is an Annotation status with name \"([^\"]*)\"$")
    public void thereIsAnAnnotationStatusWithName(String name) throws Throwable {
        AnnotationStatus annotationStatus = annotationStatusRepository.findByNameAndDefinedAt(name, project);
        assertNotNull(annotationStatus);
    }

    @And("^There is an Annotaion Status with name \"([^\"]*)\" related to the project \"([^\"]*)\"$")
    public void thereIsAnAnnotaionStatusWithName(String name, String projectName) throws Throwable {
        project = projectRepository.findByName(projectName);
        AnnotationStatus annotationStatus = new AnnotationStatus();
        annotationStatus.setName(name);
        annotationStatus.setDefinedAt(project);
        annotationStatusRepository.save(annotationStatus);
    }

    @When("^I attach the Sample with text \"([^\"]*)\" to the Annotation Status \"([^\"]*)\"$")
    public void iAttachTheSampleWithTextToTheAnnotationStatus(String sampleText, String annotStatus) throws Throwable {
        Sample sample = sampleRepository.findByText(sampleText);
        AnnotationStatus annotationStatus = annotationStatusRepository.findByNameAndDefinedAt(annotStatus, project);


        stepDefs.result = stepDefs.mockMvc.perform(
                post("/samples/{sample_id}/annotationStatuses", sample.getId())
                        .contentType("text/uri-list")
                        .content(annotationStatus.getUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @And("^The sample with text \"([^\"]*)\" has annotation status \"([^\"]*)\"$")
    @Transactional
    public void theSampleWithTextHasAnnotationStatus(String text, String status) throws Throwable {
        Sample sample = sampleRepository.findByText(text);
        assertTrue(sample.getAnnotationStatuses().stream().anyMatch(annotationStatus -> annotationStatus.getName().equals(status)));
    }

    @And("^The sample with text \"([^\"]*)\" is marked with Annotation status \"([^\"]*)\"$")
    public void theSampleWithTextIsMarkedWithAnnotationStatus(String text, String status) throws Throwable {
        Sample sample = sampleRepository.findByText(text);
        AnnotationStatus annotationStatus = annotationStatusRepository.findByNameAndDefinedAt(status, project);
        sample.setAnnotationStatuses(Arrays.asList(annotationStatus));
        sampleRepository.save(sample);
    }

    @When("^I remove the association of Sample with text \"([^\"]*)\" with Annotation status \"([^\"]*)\"$")
    public void iRemoveTheAssociationOfSampleWithTextWithAnnotationStatus(String sampleText, String annotationStatus) throws Throwable {
        Sample sample = sampleRepository.findByText(sampleText);
        AnnotationStatus status = annotationStatusRepository.findByNameAndDefinedAt(annotationStatus, project);

        stepDefs.result = stepDefs.mockMvc.perform(
                delete("/samples/{sample_id}/annotationStatuses/{status_id}", sample.getId(), status.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @And("^The sample with text \"([^\"]*)\" does not have status \"([^\"]*)\"$")
    @Transactional
    public void theSampleWithTextDoesNotHaveStatus(String text, String status) throws Throwable {
        Sample sample = sampleRepository.findByText(text);
        assertTrue(sample.getAnnotationStatuses().stream().noneMatch(annotationStatus -> annotationStatus.getName().equals(status)));
    }

    @When("^I retrieve the Samples associated with the Annotation status \"([^\"]*)\"$")
    public void iRetrieveTheSamplesAssociatedWithTheAnnotationStatus(String annotStatus) throws Throwable {
        AnnotationStatus annotationStatus = annotationStatusRepository.findByNameAndDefinedAt(annotStatus, project);

        stepDefs.result = stepDefs.mockMvc.perform(
                get("/samples/search/findByAnnotationStatuses?annotationStatus={annotStatusURI}", annotationStatus.getUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }

    @When("^I delete the Annotation status \"([^\"]*)\"$")
    public void iDeleteTheAnnotationStatus(String annotStatus) throws Throwable {
        AnnotationStatus annotationStatus = annotationStatusRepository.findByNameAndDefinedAt(annotStatus, project);

        stepDefs.result = stepDefs.mockMvc.perform(
                delete(annotationStatus.getUri())
                        .with(AuthenticationStepDefs.authenticate()))
                .andDo(print());
    }
}
