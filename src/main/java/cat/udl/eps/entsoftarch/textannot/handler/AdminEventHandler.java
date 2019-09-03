package cat.udl.eps.entsoftarch.textannot.handler;

import cat.udl.eps.entsoftarch.textannot.domain.Admin;
import cat.udl.eps.entsoftarch.textannot.repository.AdminRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class AdminEventHandler {
    final Logger logger = LoggerFactory.getLogger(Admin.class);

    @Autowired
    AdminRepository adminRepository;

    @Autowired
    EntityManager entityManager;

    @HandleBeforeCreate
    @Transactional
    public void handleAdminPreCreate(Admin admin){
        logger.info("Before creating: {}", admin.toString());
        admin.encodePassword();
    }

    @HandleBeforeSave
    @Transactional
    public void handleAdminPreSave(Admin admin){
        logger.info("Before updating: {}", admin.toString());
        entityManager.detach(admin);
        Admin oldAdmin = adminRepository.findById(admin.getUsername()).get();
        if (!admin.getPassword().equals(oldAdmin.getPassword()))
            admin.encodePassword();
    }
}
