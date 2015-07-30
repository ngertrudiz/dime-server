/*
  Copyright (c) 2015 University of Helsinki

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

package fi.hiit.dime.data;

import fi.hiit.dime.authentication.User;

import com.fasterxml.jackson.annotation.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
   Base class for all DiMe data objects, i.e. data items uploaded to be stored.
*/
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
/*
 * NOTE: all concrete (non-abstract) subclasses need to be declared
 * here below in order for Jackson's Polymorphic Type Handling to
 * work. This is the feature that interprets and outputs the "@type"
 * property in the JSON representing a Java class, e.g. "@type":
 * "FeedbackEvent".
 *
 * See:
 * http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 */
@JsonSubTypes({
	    // Events
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.FeedbackEvent.class, name="FeedbackEvent"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.Document.class, name="Document"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.DesktopEvent.class, name="DesktopEvent"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.MessageEvent.class, name="MessageEvent"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.PhysicalEvent.class, name="PhysicalEvent"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.SearchEvent.class, name="SearchEvent"),
	    // InformationElements
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.Document.class, name="Document"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.Message.class, name="Message"),
	    @JsonSubTypes.Type(value=fi.hiit.dime.data.Person.class, name="Person")
})
@Document
public class DiMeData {

    /** Unique identifier in the database */
    @Id
    public String id;

    /** Date and time when the object was first uploaded via the
	external API. 
     */
    public Date timeCreated;

    /** Date and time when the objects was last modified via the
	external API.
    */
    public Date timeModified;

    /** User associated with the object. */
    public User user;

    public DiMeData() {
	// Set to current date and time
	timeCreated = new Date();
	timeModified = new Date();
    }	
}
