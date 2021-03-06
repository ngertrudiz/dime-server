/*
  Copyright (c) 2015-2017 University of Helsinki

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation files
  (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package fi.hiit.dime.authentication;

import fi.hiit.dime.data.Profile;
import fi.hiit.dime.database.*;
import fi.hiit.dime.util.RandomPassword;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;

/**
   Service that gives us a general interface to fetch and create users
   independent of the underlying user database.
*/
@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDAO userDAO;
    private final EventDAO eventDAO;
    private final InformationElementDAO infoElemDAO;
    private final ProfileDAO profileDAO;
    private final static String ADMIN_USERNAME = "admin";
    private final static String ADMIN_PASSWORD = ""; // empty means random
    private RandomPassword pw;

    private static final int MIN_PASSWORD_LENGTH = 3;

    @Autowired
    UserServiceImpl(UserDAO userDAO, EventDAO eventDAO,
                    InformationElementDAO infoElemDAO,
                    ProfileDAO profileDAO) {
        this.userDAO = userDAO;
        this.eventDAO = eventDAO;
        this.infoElemDAO = infoElemDAO;
        this.profileDAO = profileDAO;
        this.pw = new RandomPassword();
    }

    /** 
        This method is run on startup, to create a default admin user
        if it is not already present.
    */
    @PostConstruct
    public void installAdminUser() {
        if (getUserByUsername(ADMIN_USERNAME) == null) {
            String passwd = ADMIN_PASSWORD;
            if (passwd.isEmpty())
                passwd = pw.getPassword(20, true, false);

            User user = new User();
            user.username = ADMIN_USERNAME;
            user.role = Role.ADMIN;

            try {
                create(user, passwd);
                System.out.printf("\nCreated default admin user with password " +
                                  "\"%s\"\n\n.", passwd);
            } catch (CannotCreateUserException e) {
            }
        }
    }

    @Override
    public User getUserById(Long id) {
        return userDAO.findById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }
    
    @Override
    public Collection<User> getAllUsers() {
        return userDAO.findAll();
    }

    private void setPassword(User user, String password) throws CannotCreateUserException
    {
        if (password.length() < MIN_PASSWORD_LENGTH) 
            throw new CannotCreateUserException(String.format("Password is too short! "+
                                                              "Please use at least %d characters.",
                                                              MIN_PASSWORD_LENGTH));
        user.passwordHash = new BCryptPasswordEncoder().encode(password);
    }

    @Override
    public User create(User user, String password) 
        throws CannotCreateUserException
    {
        if (getUserByUsername(user.username) != null)
            throw new CannotCreateUserException("This user name is not available.");

        return update(user, password);
    }

    @Override
    public User update(User user, String password) 
        throws CannotCreateUserException
    {
        if (password != null)
            setPassword(user, password);
        return userDAO.save(user);
    }
    
    @Override
    public boolean removeAllForUserId(Long id) {
        List<Profile> profiles = profileDAO.profilesForUser(id);
        for (Profile p : profiles) p.removeAllRelations();

        eventDAO.removeForUser(id);
        infoElemDAO.removeForUser(id);
        profileDAO.removeForUser(id);
        userDAO.remove(id);
        return true;
    }

}
