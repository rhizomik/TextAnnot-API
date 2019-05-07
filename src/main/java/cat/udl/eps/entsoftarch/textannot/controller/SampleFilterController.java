package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.QSample;
import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.util.List;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @PostMapping("/samples/filter")
    public @ResponseBody PagedResources<PersistentEntityResource> getFilteredSamples(@RequestBody SampleFilters filters, Pageable pageable, PagedResourcesAssembler resourceAssembler) {
        List<Integer> samplesContainingWord = sampleRepository.findByTextContainingWord(filters.getWord());
        Page<Sample> samples = sampleRepository.findAll(QSample.sample.id.in(samplesContainingWord), pageable);
        return resourceAssembler.toResource(samples);
    }

    private static class SampleFilters implements Serializable {
        private String word;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }
    }

}
