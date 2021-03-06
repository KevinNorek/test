package io.fourcast.gae.service;


import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.googlecode.objectify.Ref;
import io.fourcast.gae.dao.ProjectDao;
import io.fourcast.gae.dao.UserDao;
import io.fourcast.gae.model.project.Project;
import io.fourcast.gae.model.user.User;
import io.fourcast.gae.util.Globals;
import io.fourcast.gae.util.ServiceConstants;
import io.fourcast.gae.util.exceptions.ConstraintViolationsException;
import io.fourcast.gae.util.exceptions.FCServerException;
import io.fourcast.gae.util.exceptions.FCTimestampConflictException;
import io.fourcast.gae.util.exceptions.FCUserException;

import java.util.List;

@Api(
        name = "projectService",
        version = "v0.0.1",
        description = "Service to handle Project DSEntry requests",
        clientIds = {
                ServiceConstants.WEB_CLIENT_ID_DEV,
                ServiceConstants.WEB_CLIENT_ID_QA,
                ServiceConstants.WEB_CLIENT_ID_PROD,
                com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID}//when going live, remove this client id!
)

public class ProjectService extends AbstractService {

    //TODO dependency injection for unit testing instead of fugly constructor?
    private ProjectDao projectDao = new ProjectDao();
    private UserDao userDao = new UserDao();

    public ProjectService(ProjectDao projectDao,UserDao userDao) {
        this.projectDao = projectDao;
        this.userDao = userDao;
    }

    public ProjectService() {
    }


    /**
     * Saves the project, given that both the user has access and all validation pass. If the project exists in the DS
     * already, some data may be recovered from that instance (e.g. creationDate should not be modifiable afterwards)
     * @param project the project that needs saving
     * @return the saved project. For new projects, an ID is assigned.
     * @throws UnauthorizedException if the user has no access to perform this operation
     */
    @ApiMethod(name = "saveProject", httpMethod = "post")
    public Project saveProject(Project project) throws UnauthorizedException, ConstraintViolationsException, FCTimestampConflictException, FCUserException, FCServerException {

        com.google.appengine.api.users.User user = validateUser(true);
        User currentUser = userDao.getUserByEmail(user.getEmail());

        //TODO BUSINESS LOGIC - check if the user actually has the right to save this project. e.g. validate existing project
        //TODO BUSINESS LOGIC - to see if he is owner etc.

        //no parent project is a 0
        if (project.getParentId() == null) {
            project.setParentId(0L);
        }

        //there must always be an owner
        if (project.getOwner() == null) {
            project.setOwner(Ref.create(currentUser));
        }

        // move un-changeable field from existing db instance to passed instance
        // depends on business logic needed per customer case.
        if (project.getId() != null) {
            Project oldProject = projectDao.getProject(project.getId());
            //example: should not modify creation date
            project.setCreationDate(oldProject.getCreationDate());
        }

        project = projectDao.saveProject(project);

        return project;
    }

    /**
     * @param projectId the ID of the project to retrieve
     * @return the project with the given ID
     * @throws OAuthRequestException Login failure
     * @throws UnauthorizedException User has no access to the project
     */
    @ApiMethod(name = "getProject")
    public Project getProject(@Named("id") Long projectId) throws OAuthRequestException, UnauthorizedException {

        validateUser();

        //TODO BUSINESS LOGIC - implement logic to check if the user should have access. we want to avoid tech-savy users who guess id's and then have access
        //TODO BUSINESS LOGIC - to projects they would otherwise not have access to with a regular flow (like using getProjectsForUser).

        Project project = projectDao.getProject(projectId);
        return project;
    }

    @ApiMethod(name = "getAllProjects", path = "getAllProjects")
    public List<Project> getAllProjects() throws UnauthorizedException, OAuthRequestException {
        validateUser();

        //TODO BUSINESS LOGIC - validate if user has access to these projects! Not only FE validation!

        return projectDao.getAllProjects();
    }


    @ApiMethod(name = "getProjectsForUser")
    public List<Project> getProjectsFortUser() throws Exception {
        com.google.appengine.api.users.User user = validateUser();

        User dsUser = userDao.getUserByEmail(user.getEmail());

        if (dsUser == null) {
            throw new NotFoundException("user not found in datastore");
        }

        return projectDao.getProjectsForUser(dsUser);
    }


    /**
     * Anyone can read
     * @return
     */
    @Override
    protected Globals.USER_ROLE requiredReadRole() {
        return Globals.USER_ROLE.ROLE_USER;
    }

    /**
     * only owners can write
     * @return
     */
    @Override
    protected Globals.USER_ROLE requiredWriteRole() {
        return Globals.USER_ROLE.ROLE_PROJECT_OWNER;
    }


}