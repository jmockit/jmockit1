package mockit.integration.junit4.github173;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import javax.annotation.Resource;

@Stateless
public class ServicesEJB {

    private final static Logger LOGGER = Logger.getLogger(ServicesEJB.class.getName());

    @EJB
    private DatabaseRemote maarsDb;
    @EJB
    private Clock clock;

}
