package cat.udl.eps.entsoftarch.textannot.handler;

import cat.udl.eps.entsoftarch.textannot.domain.Admin;
import cat.udl.eps.entsoftarch.textannot.repository.AdminRepository;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class AdminEventHandler {
    final Logger logger = LoggerFactory.getLogger(Admin.class);

    @Autowired
    AdminRepository adminRepository;

    @HandleAfterCreate
    @Transactional
    public void handleAdminPostCreate(Admin admin){
        logger.info("After creating: {}", admin.toString());
        admin.encodePassword();
        adminRepository.save(admin);
    }

    @HandleAfterSave
    @Transactional
    public void handleAdminPostSave(Admin admin){
        logger.info("After updating: {}", admin.toString());
        admin.encodePassword();
        adminRepository.save(admin);
    }
}
