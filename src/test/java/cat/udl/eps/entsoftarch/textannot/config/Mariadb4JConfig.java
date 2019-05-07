package cat.udl.eps.entsoftarch.textannot.config;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;


import javax.sql.DataSource;
import java.sql.Driver;

@Lazy
@Configuration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@Profile("test")
public class Mariadb4JConfig {

    @Value("${db.port} ?: 3306")
    private Integer port;

    private static final String DB_SERVICE = "dbServiceBean";
    @Bean(name = {DB_SERVICE})
    public MariaDB4jSpringService mariaDb() {
        MariaDB4jSpringService mariaDb = new MariaDB4jSpringService();
        mariaDb.setDefaultPort(port);
        return mariaDb;
    }

    /**
     * If you want to use Flyway/Liqubase, make sure those beans are also depend
     * on the MariaDB service or this bean.
     *
     * @param dataSourceProperties configured in application.properties, spring.datasource.*
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    @DependsOn(DB_SERVICE)
    public DataSource dataSource(DataSourceProperties dataSourceProperties) throws ClassNotFoundException {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(getDriverClassByName(dataSourceProperties.determineDriverClassName()));
        dataSource.setUrl(dataSourceProperties.getUrl());
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        return dataSource;
    }

    @SuppressWarnings("unchecked")
    private Class<Driver> getDriverClassByName(String className) {
        try {
            return (Class<Driver>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager manager = new DataSourceTransactionManager();
        manager.setDataSource(dataSource);
        return manager;
    }

}