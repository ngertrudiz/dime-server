/*
  Copyright (c) 2015-2016 University of Helsinki

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

package fi.hiit.dime;

import static org.junit.Assert.*;
// import static fi.hiit.dime.data.DiMeData.makeStub;

import fi.hiit.dime.authentication.User;
import fi.hiit.dime.authentication.Role;

// import fi.hiit.dime.ApiController.ApiMessage;
// import fi.hiit.dime.data.DiMeData;
// import fi.hiit.dime.data.Document;
// import fi.hiit.dime.data.Event;
// import fi.hiit.dime.data.EventRelation;
// import fi.hiit.dime.data.FeedbackEvent;
// import fi.hiit.dime.data.InformationElement;
// import fi.hiit.dime.data.InformationElementRelation;
// import fi.hiit.dime.data.Message;
// import fi.hiit.dime.data.MessageEvent;
// import fi.hiit.dime.data.Profile;
// import fi.hiit.dime.data.ReadingEvent;
// import fi.hiit.dime.data.ResourcedEvent;
// import fi.hiit.dime.data.ScientificDocument;
// import fi.hiit.dime.data.SearchEvent;
// import fi.hiit.dime.data.Tag;
// import fi.hiit.dime.search.KeywordSearchQuery;
// import fi.hiit.dime.search.SearchIndex;
// import fi.hiit.dime.search.SearchResults;
// import fi.hiit.dime.util.RandomPassword;

import org.junit.Test;
import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// import java.util.ArrayList;
// import java.util.HashSet;
// import java.util.Set;

/**
 * @author Mats Sjöberg (mats.sjoberg@helsinki.fi)
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class UsersControllerTest extends RestTest {
    @Test
    public void testUsers() throws Exception {
        // Try accessing user authenticated as testuser_XXX
        getDataExpectError(apiUrl("users", 1L));

        // "Log out"
        setTemporaryUnauthenticatedRest();

        // Register a new user
        User newUser = new User();
        newUser.username = "testuser42";
        newUser.password = "testuser123";
        newUser.role = Role.ADMIN;

        User createdUser = uploadData(apiUrl("users"), newUser, User.class);
        assertEquals(newUser.username, createdUser.username);
        assertTrue(createdUser.password == null);
        assertTrue(createdUser.passwordHash == null);
        assertTrue(createdUser.email == null);
        assertEquals(Role.USER, createdUser.role); // it should have ignored role=ADMIN

        Long id = createdUser.getId();

        // Try accessing new user still logged out
        getDataExpectError(apiUrl("users", id));

        // "Log in" as new user
        setTemporaryRest(newUser.username, newUser.password);

        // Get new users data
        User gotUser = getData(apiUrl("users", id), User.class);
        assertEquals(newUser.username, gotUser.username);
        assertEquals(id, gotUser.getId());

        // Add an email address and update
        gotUser.email = "testuser@example.com";

        // Test updating with wrong credentials
        removeTemporaryRest();
        uploadData(apiUrl("users"), gotUser, ApiError.class);

        // Test with real credentials
        setTemporaryRest(newUser.username, newUser.password);
        User updatedUser = uploadData(apiUrl("users"), gotUser, User.class);

        // Get user from the all users list
        User[] gotAllUsers = getData(apiUrl("users"), User[].class);
        assertEquals(1, gotAllUsers.length);

        User testUser = gotAllUsers[0];
        assertEquals(newUser.username, testUser.username);
        assertEquals(id, testUser.getId());
        assertEquals(gotUser.email, testUser.email);
        
        // Delete user
        deleteData(apiUrl("users", id));
        getDataExpectError(apiUrl("users", id));

        removeTemporaryRest();
    }

}
