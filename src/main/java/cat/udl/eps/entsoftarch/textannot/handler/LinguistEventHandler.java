package cat.udl.eps.entsoftarch.textannot.handler;

import cat.udl.eps.entsoftarch.textannot.domain.Linguist;
import cat.udl.eps.entsoftarch.textannot.repository.LinguistRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class LinguistEventHandler {
    final Logger logger = LoggerFactory.getLogger(Linguist.class);

    @Autowired
    LinguistRepository linguistRepository;

    @Autowired
    EntityManager entityManager;

    @Value("${default-password}")
    String defaultPassword;


    @HandleBeforeCreate
    @Transactional
    public void handleLinguistPreCreate(Linguist linguist) {
        logger.info("Before creating: {}", linguist.toString());
        linguist.setPassword(defaultPassword);
        linguist.encodePassword();
    }

    @HandleBeforeSave
    @Transactional
    public void handleLinguistPreSave(Linguist linguist){
        logger.info("Before updating: {}", linguist.toString());
        entityManager.detach(linguist);
        Linguist oldLinguist = linguistRepository.findById(linguist.getUsername()).get();
        if (!oldLinguist.getPassword().equals(linguist.getPassword()))
            linguist.encodePassword();
        else if (linguist.isResetPassword()) {
            linguist.setPassword(defaultPassword);
            linguist.encodePassword();
        }
    }

    @HandleBeforeDelete
    @Transactional
    public void handleLinguistPreDelete(Linguist linguist){
        logger.info("Before deleting: {}", linguist.toString());
    }

    @HandleBeforeLinkSave
    public void handleLinguistPreLinkSave(Linguist linguist, Object o) {
        logger.info("Before linking: {} to {}", linguist.toString(), o.toString());
    }

    @HandleAfterCreate
    @Transactional
    public void handleLinguistPostCreate(Linguist linguist){
        logger.info("After creating: {}", linguist.toString());
    }

    @HandleAfterSave
    @Transactional
    public void handleLinguistPostSave(Linguist linguist){
        logger.info("After updating: {}", linguist.toString());
    }

    @HandleAfterDelete
    @Transactional
    public void handleLinguistPostDelete(Linguist linguist){
        logger.info("After deleting: {}", linguist.toString());
    }

    @HandleAfterLinkSave
    public void handleLinguistPostLinkSave(Linguist linguist, Object o) {
        logger.info("After linking: {} to {}", linguist.toString(), o.toString());
    }
}
